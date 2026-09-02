package ru.bogdanov.gitlabreviewmcp.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffFile;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffVersion;
import ru.bogdanov.gitlabreviewmcp.application.model.MergeRequestDetails;
import ru.bogdanov.gitlabreviewmcp.application.model.PreparedReview;
import ru.bogdanov.gitlabreviewmcp.application.model.ReviewLimits;
import ru.bogdanov.gitlabreviewmcp.application.port.GitLabClient;
import ru.bogdanov.gitlabreviewmcp.application.port.MergeRequestReferenceParser;
import ru.bogdanov.gitlabreviewmcp.application.port.ReviewProposalRepository;
import ru.bogdanov.gitlabreviewmcp.domain.GeneralReviewComment;
import ru.bogdanov.gitlabreviewmcp.domain.InlineReviewComment;
import ru.bogdanov.gitlabreviewmcp.domain.MergeRequestRef;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewCommentDraft;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewProposal;

/**
 * Validates proposed comments and creates the immutable approval preview.
 */
public final class ReviewPreparationService {

    private final MergeRequestReferenceParser referenceParser;
    private final GitLabClient gitLabClient;
    private final ReviewProposalRepository repository;
    private final ProposalDigestService digestService;
    private final DiffPositionValidator positionValidator;
    private final ReviewLimits limits;
    private final Clock clock;
    private final SecureRandom secureRandom;

    /**
     * Creates the service.
     *
     * @param referenceParser trusted merge request URL parser
     * @param gitLabClient GitLab API port
     * @param repository ephemeral proposal repository
     * @param digestService canonical digest service
     * @param positionValidator diff position validator
     * @param limits review limits
     * @param clock application clock
     * @param secureRandom secure identifier source
     */
    public ReviewPreparationService(
            MergeRequestReferenceParser referenceParser,
            GitLabClient gitLabClient,
            ReviewProposalRepository repository,
            ProposalDigestService digestService,
            DiffPositionValidator positionValidator,
            ReviewLimits limits,
            Clock clock,
            SecureRandom secureRandom) {
        this.referenceParser = referenceParser;
        this.gitLabClient = gitLabClient;
        this.repository = repository;
        this.digestService = digestService;
        this.positionValidator = positionValidator;
        this.limits = limits;
        this.clock = clock;
        this.secureRandom = secureRandom;
    }

    /**
     * Prepares a review without performing any GitLab write request.
     *
     * @param mergeRequestUrl full merge request URL
     * @param comments proposed comments
     * @return immutable preview descriptor
     */
    public PreparedReview prepare(String mergeRequestUrl, List<ReviewCommentDraft> comments) {
        MergeRequestRef reference = referenceParser.parse(mergeRequestUrl);
        List<ReviewCommentDraft> normalized = normalize(comments);
        MergeRequestDetails mergeRequest = gitLabClient.getMergeRequest(reference);
        DiffVersion version = currentVersion(reference, mergeRequest.headSha());
        List<DiffFile> diffs = normalized.stream().anyMatch(InlineReviewComment.class::isInstance)
                ? gitLabClient.getAllDiffs(reference)
                : List.of();
        List<ReviewCommentDraft> positioned = normalized.stream()
                .map(comment -> comment instanceof InlineReviewComment inline
                        ? positionValidator.normalize(inline, diffs)
                        : comment)
                .toList();

        List<String> warnings = diffs.stream()
                .filter(diff -> diff.collapsed() || diff.tooLarge() || diff.truncated())
                .map(diff -> "Diff content is incomplete for " + diff.newPath())
                .distinct()
                .toList();
        String preview = renderPreview(mergeRequest, version, positioned, warnings);
        String digest = digestService.calculate(reference, mergeRequest.headSha(), positioned);
        Instant createdAt = clock.instant();
        Instant expiresAt = createdAt.plus(limits.proposalTtl());
        String proposalId = newProposalId();
        ReviewProposal proposal = new ReviewProposal(
                proposalId,
                digest,
                reference,
                mergeRequest.headSha(),
                createdAt,
                expiresAt,
                preview,
                positioned);
        repository.save(proposal);
        return new PreparedReview(proposalId, digest, mergeRequest.headSha(), expiresAt, warnings, preview);
    }

    private List<ReviewCommentDraft> normalize(List<ReviewCommentDraft> comments) {
        if (comments == null || comments.isEmpty()) {
            throw new ReviewApplicationException("EMPTY_REVIEW", "At least one review comment is required");
        }
        if (comments.size() > limits.maxComments()) {
            throw new ReviewApplicationException(
                    "TOO_MANY_COMMENTS", "Review contains more than " + limits.maxComments() + " comments");
        }
        List<ReviewCommentDraft> normalized = new ArrayList<>(comments.size());
        for (ReviewCommentDraft comment : comments) {
            Objects.requireNonNull(comment, "Review comments must not contain null values");
            String body = normalizeBody(comment.body());
            if (comment instanceof GeneralReviewComment) {
                normalized.add(new GeneralReviewComment(body));
            } else if (comment instanceof InlineReviewComment inline) {
                String path = normalizePath(inline.path());
                if (inline.line() < 1 || inline.side() == null) {
                    throw new ReviewApplicationException("INVALID_COMMENT", "Inline line and side are required");
                }
                normalized.add(new InlineReviewComment(body, path, inline.line(), inline.side()));
            } else {
                throw new ReviewApplicationException("INVALID_COMMENT", "Unsupported review comment type");
            }
        }
        return List.copyOf(normalized);
    }

    private String normalizeBody(String value) {
        if (value == null) {
            throw new ReviewApplicationException("INVALID_COMMENT", "Comment body is required");
        }
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (normalized.isEmpty()) {
            throw new ReviewApplicationException("INVALID_COMMENT", "Comment body must not be blank");
        }
        if (normalized.length() > limits.maxCommentLength()) {
            throw new ReviewApplicationException(
                    "COMMENT_TOO_LONG", "Comment exceeds " + limits.maxCommentLength() + " characters");
        }
        return normalized;
    }

    private String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            throw new ReviewApplicationException("INVALID_COMMENT", "Inline path is required");
        }
        String path = value.replace('\\', '/').strip();
        if (path.startsWith("/") || path.contains("../") || path.equals("..")) {
            throw new ReviewApplicationException("INVALID_COMMENT", "Inline path must be repository-relative");
        }
        return path;
    }

    private DiffVersion currentVersion(MergeRequestRef reference, String headSha) {
        return gitLabClient.getDiffVersions(reference).stream()
                .filter(version -> headSha.equals(version.headSha()))
                .findFirst()
                .orElseThrow(() -> new ReviewApplicationException(
                        "DIFF_VERSION_NOT_FOUND", "No diff version matches the current merge request head"));
    }

    private String renderPreview(
            MergeRequestDetails mergeRequest,
            DiffVersion version,
            List<ReviewCommentDraft> comments,
            List<String> warnings) {
        StringBuilder value = new StringBuilder()
                .append("Review preview for ").append(mergeRequest.webUrl()).append('\n')
                .append("MR: !").append(mergeRequest.reference().iid()).append(" — ").append(mergeRequest.title()).append('\n')
                .append("Head SHA: ").append(mergeRequest.headSha()).append('\n')
                .append("Diff version: ").append(version.id()).append("\n\n");
        for (int index = 0; index < comments.size(); index++) {
            ReviewCommentDraft comment = comments.get(index);
            value.append(index + 1).append(". ");
            if (comment instanceof InlineReviewComment inline) {
                value.append("INLINE ")
                        .append(inline.path()).append(':').append(inline.line())
                        .append(" [").append(inline.side()).append("]\n");
            } else {
                value.append("GENERAL\n");
            }
            value.append(comment.body()).append("\n\n");
        }
        if (!warnings.isEmpty()) {
            value.append("Warnings:\n");
            warnings.forEach(warning -> value.append("- ").append(warning).append('\n'));
        }
        return value.toString().stripTrailing();
    }

    private String newProposalId() {
        byte[] value = new byte[18];
        secureRandom.nextBytes(value);
        return HexFormat.of().formatHex(value);
    }
}
