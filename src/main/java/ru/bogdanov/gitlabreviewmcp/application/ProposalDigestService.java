package ru.bogdanov.gitlabreviewmcp.application;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import ru.bogdanov.gitlabreviewmcp.domain.GeneralReviewComment;
import ru.bogdanov.gitlabreviewmcp.domain.InlineReviewComment;
import ru.bogdanov.gitlabreviewmcp.domain.MergeRequestRef;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewCommentDraft;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Produces a deterministic SHA-256 digest of proposal content.
 */
public final class ProposalDigestService {

    private final ObjectMapper objectMapper;

    /** @param objectMapper Jackson mapper used for canonical JSON */
    public ProposalDigestService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Calculates a digest over immutable review content.
     *
     * @param reference target merge request
     * @param headSha target commit
     * @param comments normalized comments
     * @return lowercase SHA-256 hex
     */
    public String calculate(MergeRequestRef reference, String headSha, List<ReviewCommentDraft> comments) {
        Map<String, Object> root = new java.util.TreeMap<>();
        root.put("headSha", headSha);
        root.put("iid", reference.iid());
        root.put("mergeRequestUrl", reference.webUrl().toString());
        root.put("projectPath", reference.projectPath());
        root.put("comments", comments.stream().map(this::canonicalComment).toList());
        try {
            return java.util.HexFormat.of().formatHex(sha256(objectMapper.writeValueAsBytes(root)));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Cannot canonicalize review proposal", exception);
        }
    }

    private Map<String, Object> canonicalComment(ReviewCommentDraft comment) {
        Map<String, Object> value = new java.util.TreeMap<>();
        value.put("body", comment.body());
        if (comment instanceof GeneralReviewComment) {
            value.put("kind", "GENERAL");
        } else if (comment instanceof InlineReviewComment inline) {
            value.put("kind", "INLINE");
            value.put("line", inline.line());
            value.put("newPath", inline.newPath());
            value.put("oldPath", inline.oldPath());
            value.put("path", inline.path());
            value.put("side", inline.side().name());
        }
        return value;
    }

    private static byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
