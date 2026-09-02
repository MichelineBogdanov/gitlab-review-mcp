package ru.bogdanov.gitlabreviewmcp.domain;

/**
 * Lifecycle state of a review proposal.
 */
public enum ReviewProposalStatus {
    /** Validated and awaiting publication approval. */
    PREPARED,
    /** Publication is currently executing. */
    PUBLISHING,
    /** At least one comment is still safely pending. */
    PARTIAL,
    /** Every comment was published. */
    PUBLISHED,
    /** At least one write has an ambiguous outcome. */
    UNKNOWN,
    /** Proposal lifetime has elapsed. */
    EXPIRED
}
