package ru.bogdanov.gitlabreviewmcp.application;

import java.time.Clock;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffVersion;
import ru.bogdanov.gitlabreviewmcp.application.model.MergeRequestDetails;
import ru.bogdanov.gitlabreviewmcp.application.model.PublishedReview;
import ru.bogdanov.gitlabreviewmcp.application.port.GitLabClient;
import ru.bogdanov.gitlabreviewmcp.application.port.ReviewProposalRepository;
import ru.bogdanov.gitlabreviewmcp.domain.PublicationItemStatus;
import ru.bogdanov.gitlabreviewmcp.domain.PublicationReceipt;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewProposal;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewPublicationItem;

/**
 * Publishes only retained and explicitly approved review proposal content.
 */
public final class ReviewPublicationService {

    private final GitLabClient gitLabClient;
    private final ReviewProposalRepository repository;
    private final Clock clock;

    /**
     * Creates the publication service.
     *
     * @param gitLabClient GitLab API port
     * @param repository proposal repository
     * @param clock application clock
     */
    public ReviewPublicationService(GitLabClient gitLabClient, ReviewProposalRepository repository, Clock clock) {
        this.gitLabClient = gitLabClient;
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Publishes pending comments from an approved proposal.
     *
     * @param proposalId retained proposal identifier
     * @param expectedDigest digest displayed in the approved preview
     * @return aggregate and per-comment publication state
     */
    public PublishedReview publish(String proposalId, String expectedDigest) {
        ReviewProposal proposal = repository.findById(proposalId)
                .orElseThrow(() -> new ReviewApplicationException(
                        "PROPOSAL_NOT_FOUND", "Review proposal was not found or has expired"));
        MergeRequestDetails current = gitLabClient.getMergeRequest(proposal.mergeRequest());
        DiffVersion version = gitLabClient.getDiffVersions(proposal.mergeRequest()).stream()
                .filter(candidate -> proposal.headSha().equals(candidate.headSha()))
                .findFirst()
                .orElseThrow(() -> new ReviewApplicationException(
                        "DIFF_VERSION_NOT_FOUND", "No diff version matches the approved merge request head"));
        try {
            if (!proposal.beginPublication(expectedDigest, current.headSha(), clock.instant())) {
                return snapshot(proposal);
            }
        } catch (IllegalArgumentException exception) {
            throw new ReviewApplicationException("DIGEST_MISMATCH", exception.getMessage());
        } catch (IllegalStateException exception) {
            throw new ReviewApplicationException("PROPOSAL_NOT_PUBLISHABLE", exception.getMessage());
        }

        for (ReviewPublicationItem item : proposal.items()) {
            if (item.status() != PublicationItemStatus.PENDING) {
                continue;
            }
            try {
                PublicationReceipt receipt = gitLabClient.createDiscussion(
                        proposal.mergeRequest(), item.comment(), version);
                proposal.markPublished(item.index(), receipt);
            } catch (GitLabClientException exception) {
                if (exception.ambiguousWrite()) {
                    proposal.markUnknown(item.index(), exception.getMessage());
                    break;
                }
                proposal.markPendingFailure(item.index(), exception.getMessage());
                break;
            }
        }
        proposal.finishPublication();
        return snapshot(proposal);
    }

    private PublishedReview snapshot(ReviewProposal proposal) {
        return new PublishedReview(proposal.proposalId(), proposal.status(), proposal.items());
    }
}
