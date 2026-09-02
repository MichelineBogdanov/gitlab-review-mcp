package ru.bogdanov.gitlabreviewmcp.domain;

import java.net.URI;

/**
 * Canonical identity of a merge request.
 *
 * @param webUrl canonical browser URL
 * @param projectPath decoded project path
 * @param iid project-local merge request identifier
 */
public record MergeRequestRef(URI webUrl, String projectPath, long iid) {
}
