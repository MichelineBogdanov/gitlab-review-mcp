package ru.bogdanov.gitlabreviewmcp.application.model;

/**
 * File or directory in a GitLab repository tree.
 *
 * @param id Git object identifier
 * @param name entry name
 * @param type GitLab entry type, normally {@code blob} or {@code tree}
 * @param path repository-relative path
 * @param mode Git file mode
 */
public record RepositoryTreeEntry(String id, String name, String type, String path, String mode) {
}
