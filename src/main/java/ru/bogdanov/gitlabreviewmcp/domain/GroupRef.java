package ru.bogdanov.gitlabreviewmcp.domain;

import java.net.URI;

/**
 * Trusted reference to a group on the configured GitLab instance.
 *
 * @param webUrl canonical group URL
 * @param groupPath decoded namespace-qualified group path
 */
public record GroupRef(URI webUrl, String groupPath) {
}
