package ru.bogdanov.gitlabreviewmcp.infrastructure.mcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;
import ru.bogdanov.gitlabreviewmcp.application.GitLabConnectionService;
import ru.bogdanov.gitlabreviewmcp.application.model.GitLabConnectionInfo;
import ru.bogdanov.gitlabreviewmcp.configuration.GitLabReviewMcpProperties;

/**
 * MCP connection diagnostics available in every server mode.
 */
@Component
public final class GitLabConnectionTools {

    private final GitLabConnectionService connectionService;
    private final GitLabReviewMcpProperties properties;
    private final McpToolExecutor executor;

    /**
     * @param connectionService connection diagnostics
     * @param properties configured server mode
     * @param executor safe tool executor
     */
    public GitLabConnectionTools(
            GitLabConnectionService connectionService,
            GitLabReviewMcpProperties properties,
            McpToolExecutor executor) {
        this.connectionService = connectionService;
        this.properties = properties;
        this.executor = executor;
    }

    /** @return structured GitLab connection diagnostics for the active mode */
    @McpTool(
            name = "gitlab_check_connection",
            description = "Checks the configured GitLab version, authenticated user and active server capabilities.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = true))
    public ToolResponse<GitLabConnectionInfo> checkConnection() {
        return executor.execute(() -> withActiveCapabilities(connectionService.checkConnection()));
    }

    private GitLabConnectionInfo withActiveCapabilities(GitLabConnectionInfo value) {
        return new GitLabConnectionInfo(
                value.status(),
                value.gitLabVersion(),
                value.authenticatedUser(),
                properties.getMode().capabilities(),
                value.gitLabRequestId());
    }
}
