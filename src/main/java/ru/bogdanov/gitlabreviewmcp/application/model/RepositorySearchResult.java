package ru.bogdanov.gitlabreviewmcp.application.model;

/**
 * Match returned by GitLab project code search.
 *
 * @param path repository-relative file path
 * @param ref matched repository reference
 * @param startLine first line of the returned snippet
 * @param data GitLab-provided matching snippet
 */
public record RepositorySearchResult(String path, String ref, int startLine, String data) {
}
