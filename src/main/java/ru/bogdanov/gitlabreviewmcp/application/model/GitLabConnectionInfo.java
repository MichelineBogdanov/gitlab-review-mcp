package ru.bogdanov.gitlabreviewmcp.application.model;

import java.util.Set;

/**
 * Verified identity and capabilities of the configured GitLab connection.
 *
 * @param status connection status
 * @param gitLabVersion GitLab version string
 * @param authenticatedUser authenticated username
 * @param capabilities enabled MCP capabilities
 * @param gitLabRequestId last GitLab request identifier
 */
public record GitLabConnectionInfo(
        String status,
        String gitLabVersion,
        String authenticatedUser,
        Set<String> capabilities,
        String gitLabRequestId) {
}
