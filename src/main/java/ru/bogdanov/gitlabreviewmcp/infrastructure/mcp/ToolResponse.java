package ru.bogdanov.gitlabreviewmcp.infrastructure.mcp;

/**
 * Structured success or error envelope returned by all MCP tools.
 *
 * @param success whether the operation succeeded
 * @param data operation result on success
 * @param error safe structured error on failure
 * @param <T> result type
 */
public record ToolResponse<T>(boolean success, T data, ToolError error) {

    /**
     * Creates a success envelope.
     *
     * @param data result payload
     * @param <T> result type
     * @return success envelope
     */
    public static <T> ToolResponse<T> success(T data) {
        return new ToolResponse<>(true, data, null);
    }

    /**
     * Creates a failure envelope.
     *
     * @param error safe error
     * @param <T> result type
     * @return failure envelope
     */
    public static <T> ToolResponse<T> failure(ToolError error) {
        return new ToolResponse<>(false, null, error);
    }
}
