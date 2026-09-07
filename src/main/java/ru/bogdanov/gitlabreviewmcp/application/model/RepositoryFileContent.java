package ru.bogdanov.gitlabreviewmcp.application.model;

/**
 * Bounded text slice of a file from the current default branch.
 *
 * @param fileName file name
 * @param filePath repository-relative path
 * @param ref resolved GitLab reference
 * @param blobId blob identifier
 * @param commitId commit that supplied the file
 * @param lastCommitId last commit that changed the file
 * @param size full file size in bytes
 * @param totalLines total number of text lines
 * @param startLine first returned one-based line, or zero for an empty file
 * @param endLine last returned one-based line, or zero for an empty file
 * @param content returned text slice
 * @param nextStartLine next one-based line, or {@code null} when complete
 * @param gitLabRequestId GitLab request identifier
 */
public record RepositoryFileContent(
        String fileName,
        String filePath,
        String ref,
        String blobId,
        String commitId,
        String lastCommitId,
        long size,
        int totalLines,
        int startLine,
        int endLine,
        String content,
        Integer nextStartLine,
        String gitLabRequestId) {
}
