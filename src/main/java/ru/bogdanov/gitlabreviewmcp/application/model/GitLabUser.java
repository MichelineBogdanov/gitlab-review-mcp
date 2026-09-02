package ru.bogdanov.gitlabreviewmcp.application.model;

import java.net.URI;

/**
 * Public GitLab user data used in MCP responses.
 *
 * @param id user identifier
 * @param username username
 * @param name display name
 * @param webUrl profile URL
 */
public record GitLabUser(long id, String username, String name, URI webUrl) {
}
