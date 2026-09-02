package ru.bogdanov.gitlabreviewmcp.domain;

/**
 * Comment attached to one line in the merge request diff.
 *
 * @param body Markdown comment text
 * @param path repository-relative file path
 * @param line line number
 * @param side diff side
 * @param oldPath path on the old side of the current diff
 * @param newPath path on the new side of the current diff
 */
public record InlineReviewComment(
        String body,
        String path,
        int line,
        DiffSide side,
        String oldPath,
        String newPath)
        implements ReviewCommentDraft {

    /**
     * Creates an unresolved inline comment before current diff paths are known.
     *
     * @param body Markdown comment text
     * @param path repository-relative selected path
     * @param line selected line
     * @param side selected diff side
     */
    public InlineReviewComment(String body, String path, int line, DiffSide side) {
        this(body, path, line, side, path, path);
    }
}
