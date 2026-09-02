package ru.bogdanov.gitlabreviewmcp.application.model;

import java.util.List;

/**
 * Bounded page returned to an MCP tool.
 *
 * @param items page items
 * @param nextCursor opaque cursor for the next page
 * @param truncated whether local limits truncated content
 * @param warnings safe warnings
 * @param gitLabRequestId GitLab request identifier
 * @param <T> item type
 */
public record PageResult<T>(
        List<T> items,
        String nextCursor,
        boolean truncated,
        List<String> warnings,
        String gitLabRequestId) {
}
