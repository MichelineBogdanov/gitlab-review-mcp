package ru.bogdanov.gitlabreviewmcp.domain;

/**
 * General discussion comment on the merge request overview.
 *
 * @param body Markdown comment text
 */
public record GeneralReviewComment(String body) implements ReviewCommentDraft {
}
