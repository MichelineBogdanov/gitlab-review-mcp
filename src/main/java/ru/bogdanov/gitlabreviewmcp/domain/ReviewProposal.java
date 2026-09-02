package ru.bogdanov.gitlabreviewmcp.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate that guarantees immutable review content and atomic publication state changes.
 */
public final class ReviewProposal {

    private final String proposalId;
    private final String digest;
    private final MergeRequestRef mergeRequest;
    private final String headSha;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final String preview;
    private final List<MutableItem> items;
    private ReviewProposalStatus status = ReviewProposalStatus.PREPARED;

    /**
     * Creates a prepared proposal.
     *
     * @param proposalId random proposal identifier
     * @param digest canonical content digest
     * @param mergeRequest target merge request
     * @param headSha target head commit
     * @param createdAt creation timestamp
     * @param expiresAt expiration timestamp
     * @param preview complete human-readable preview
     * @param comments immutable normalized comments
     */
    public ReviewProposal(
            String proposalId,
            String digest,
            MergeRequestRef mergeRequest,
            String headSha,
            Instant createdAt,
            Instant expiresAt,
            String preview,
            List<ReviewCommentDraft> comments) {
        this.proposalId = Objects.requireNonNull(proposalId);
        this.digest = Objects.requireNonNull(digest);
        this.mergeRequest = Objects.requireNonNull(mergeRequest);
        this.headSha = Objects.requireNonNull(headSha);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.preview = Objects.requireNonNull(preview);
        this.items = new ArrayList<>(comments.size());
        for (int index = 0; index < comments.size(); index++) {
            this.items.add(new MutableItem(index, comments.get(index)));
        }
    }

    /** @return proposal identifier */
    public String proposalId() {
        return proposalId;
    }

    /** @return proposal digest */
    public String digest() {
        return digest;
    }

    /** @return target merge request */
    public MergeRequestRef mergeRequest() {
        return mergeRequest;
    }

    /** @return target head SHA */
    public String headSha() {
        return headSha;
    }

    /** @return creation timestamp */
    public Instant createdAt() {
        return createdAt;
    }

    /** @return expiration timestamp */
    public Instant expiresAt() {
        return expiresAt;
    }

    /** @return complete preview */
    public String preview() {
        return preview;
    }

    /** @return current proposal status */
    public synchronized ReviewProposalStatus status() {
        return status;
    }

    /**
     * Starts or resumes publication after validating immutable proposal data.
     *
     * @param expectedDigest digest supplied by the approved tool call
     * @param currentHeadSha current GitLab head SHA
     * @param now current time
     * @return {@code false} when the proposal was already completely published
     */
    public synchronized boolean beginPublication(String expectedDigest, String currentHeadSha, Instant now) {
        expireIfRequired(now);
        if (!constantTimeEquals(digest, expectedDigest)) {
            throw new IllegalArgumentException("Review proposal digest does not match the preview");
        }
        if (status == ReviewProposalStatus.PUBLISHED) {
            return false;
        }
        if (status == ReviewProposalStatus.EXPIRED) {
            throw new IllegalStateException("Review proposal has expired");
        }
        if (status == ReviewProposalStatus.PUBLISHING) {
            throw new IllegalStateException("Review proposal is already being published");
        }
        if (status == ReviewProposalStatus.UNKNOWN) {
            throw new IllegalStateException("Review proposal contains a write with unknown outcome");
        }
        if (!headSha.equals(currentHeadSha)) {
            throw new IllegalStateException("Merge request head changed; prepare a new review");
        }
        status = ReviewProposalStatus.PUBLISHING;
        return true;
    }

    /**
     * Returns immutable item snapshots.
     *
     * @return proposal items
     */
    public synchronized List<ReviewPublicationItem> items() {
        return items.stream().map(MutableItem::snapshot).toList();
    }

    /**
     * Marks one item as published.
     *
     * @param index item index
     * @param receipt GitLab receipt
     */
    public synchronized void markPublished(int index, PublicationReceipt receipt) {
        item(index).markPublished(receipt);
    }

    /**
     * Records a deterministic failure while leaving the item safe to retry.
     *
     * @param index item index
     * @param message safe failure message
     */
    public synchronized void markPendingFailure(int index, String message) {
        item(index).lastError = message;
        status = ReviewProposalStatus.PARTIAL;
    }

    /**
     * Records an ambiguous write that must never be retried automatically.
     *
     * @param index item index
     * @param message safe failure message
     */
    public synchronized void markUnknown(int index, String message) {
        MutableItem item = item(index);
        item.status = PublicationItemStatus.UNKNOWN;
        item.lastError = message;
        status = ReviewProposalStatus.UNKNOWN;
    }

    /**
     * Calculates the aggregate state after a publication attempt.
     */
    public synchronized void finishPublication() {
        if (items.stream().anyMatch(item -> item.status == PublicationItemStatus.UNKNOWN)) {
            status = ReviewProposalStatus.UNKNOWN;
        } else if (items.stream().allMatch(item -> item.status == PublicationItemStatus.PUBLISHED)) {
            status = ReviewProposalStatus.PUBLISHED;
        } else {
            status = ReviewProposalStatus.PARTIAL;
        }
    }

    /**
     * Expires the proposal when required.
     *
     * @param now current time
     * @return current status
     */
    public synchronized ReviewProposalStatus expireIfRequired(Instant now) {
        if (status != ReviewProposalStatus.PUBLISHED && status != ReviewProposalStatus.PUBLISHING
                && !now.isBefore(expiresAt)) {
            status = ReviewProposalStatus.EXPIRED;
        }
        return status;
    }

    private MutableItem item(int index) {
        if (index < 0 || index >= items.size()) {
            throw new IllegalArgumentException("Unknown proposal item: " + index);
        }
        return items.get(index);
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (right == null) {
            return false;
        }
        return java.security.MessageDigest.isEqual(
                left.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                right.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    private static final class MutableItem {

        private final int index;
        private final ReviewCommentDraft comment;
        private PublicationItemStatus status = PublicationItemStatus.PENDING;
        private PublicationReceipt receipt;
        private String lastError;

        private MutableItem(int index, ReviewCommentDraft comment) {
            this.index = index;
            this.comment = Objects.requireNonNull(comment);
        }

        private void markPublished(PublicationReceipt receipt) {
            if (status == PublicationItemStatus.UNKNOWN) {
                throw new IllegalStateException("Cannot overwrite an unknown publication outcome");
            }
            this.status = PublicationItemStatus.PUBLISHED;
            this.receipt = Objects.requireNonNull(receipt);
            this.lastError = null;
        }

        private ReviewPublicationItem snapshot() {
            return new ReviewPublicationItem(index, comment, status, receipt, lastError);
        }
    }
}
