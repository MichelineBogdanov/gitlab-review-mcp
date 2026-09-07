package ru.bogdanov.gitlabreviewmcp.infrastructure.mcp;

import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.bogdanov.gitlabreviewmcp.application.GitLabClientException;
import ru.bogdanov.gitlabreviewmcp.application.ReviewApplicationException;

/**
 * Converts application failures to safe, structured MCP responses.
 */
@Component
public final class McpToolExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpToolExecutor.class);

    /**
     * Executes an MCP operation without exposing stack traces or GitLab response bodies.
     *
     * @param operation application operation
     * @param <T> result type
     * @return structured tool response
     */
    public <T> ToolResponse<T> execute(Supplier<T> operation) {
        try {
            return ToolResponse.success(operation.get());
        } catch (GitLabClientException exception) {
            return ToolResponse.failure(new ToolError(
                    exception.code(), exception.getMessage(), exception.httpStatus(), exception.gitLabRequestId()));
        } catch (ReviewApplicationException exception) {
            return ToolResponse.failure(new ToolError(exception.code(), exception.getMessage(), null, null));
        } catch (IllegalArgumentException | NullPointerException exception) {
            return ToolResponse.failure(new ToolError("INVALID_INPUT", exception.getMessage(), null, null));
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected MCP tool failure: {}", exception.getClass().getSimpleName());
            return ToolResponse.failure(new ToolError("INTERNAL_ERROR", "Unexpected internal error", null, null));
        }
    }
}
