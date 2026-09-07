package ru.bogdanov.gitlabreviewmcp.domain;

import java.net.URI;

/**
 * Trusted reference to a project on the configured GitLab instance.
 *
 * @param webUrl canonical project URL
 * @param projectPath decoded namespace-qualified project path
 */
public record ProjectRef(URI webUrl, String projectPath) {
}
