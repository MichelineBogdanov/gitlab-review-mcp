package ru.bogdanov.gitlabreviewmcp.infrastructure.mcp;

/**
 * Safe structured MCP error.
 *
 * @param code stable error code
 * @param message safe message without a stack trace
 * @param httpStatus GitLab HTTP status, if available
 * @param gitLabRequestId GitLab request identifier, if available
 */
public record ToolError(String code, String message, Integer httpStatus, String gitLabRequestId) {
}
