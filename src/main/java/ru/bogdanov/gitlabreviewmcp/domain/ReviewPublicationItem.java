package ru.bogdanov.gitlabreviewmcp.domain;

/**
 * Snapshot of one proposal item and its publication state.
 *
 * @param index zero-based stable index
 * @param comment immutable comment
 * @param status publication state
 * @param receipt successful publication receipt
 * @param lastError last safe diagnostic message
 */
public record ReviewPublicationItem(
        int index,
        ReviewCommentDraft comment,
        PublicationItemStatus status,
        PublicationReceipt receipt,
        String lastError) {
}
