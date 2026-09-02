package ru.bogdanov.gitlabreviewmcp.application.model;

import java.util.List;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewProposalStatus;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewPublicationItem;

/**
 * Current publication result for a retained proposal.
 *
 * @param proposalId proposal identifier
 * @param status aggregate status
 * @param items per-comment publication state and receipts
 */
public record PublishedReview(
        String proposalId,
        ReviewProposalStatus status,
        List<ReviewPublicationItem> items) {
}
