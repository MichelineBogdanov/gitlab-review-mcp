package ru.bogdanov.gitlabreviewmcp.domain;

/**
 * Draft comment that can be included in an immutable review proposal.
 */
public sealed interface ReviewCommentDraft permits GeneralReviewComment, InlineReviewComment {

    /**
     * Returns normalized Markdown comment text.
     *
     * @return comment text
     */
    String body();
}
