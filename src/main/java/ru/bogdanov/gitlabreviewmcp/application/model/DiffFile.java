package ru.bogdanov.gitlabreviewmcp.application.model;

/**
 * One changed file returned by the GitLab merge request diffs endpoint.
 *
 * @param oldPath path on the old side
 * @param newPath path on the new side
 * @param diff unified diff text
 * @param newFile whether the file was added
 * @param renamedFile whether the file was renamed
 * @param deletedFile whether the file was deleted
 * @param generatedFile whether GitLab marks the file as generated
 * @param collapsed whether GitLab collapsed the diff
 * @param tooLarge whether GitLab omitted an oversized diff
 * @param truncated whether the local MCP size limit truncated the diff
 */
public record DiffFile(
        String oldPath,
        String newPath,
        String diff,
        boolean newFile,
        boolean renamedFile,
        boolean deletedFile,
        boolean generatedFile,
        boolean collapsed,
        boolean tooLarge,
        boolean truncated) {
}
