package ru.bogdanov.gitlabreviewmcp.application.model;

/**
 * Code position associated with a GitLab discussion.
 *
 * @param oldPath old path
 * @param newPath new path
 * @param oldLine old-side line
 * @param newLine new-side line
 * @param baseSha base SHA
 * @param startSha start SHA
 * @param headSha head SHA
 */
public record DiscussionPosition(
        String oldPath,
        String newPath,
        Integer oldLine,
        Integer newLine,
        String baseSha,
        String startSha,
        String headSha) {
}
