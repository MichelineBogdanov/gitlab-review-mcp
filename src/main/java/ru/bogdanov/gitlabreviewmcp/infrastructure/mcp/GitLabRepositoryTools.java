package ru.bogdanov.gitlabreviewmcp.infrastructure.mcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.bogdanov.gitlabreviewmcp.application.RepositoryQueryService;
import ru.bogdanov.gitlabreviewmcp.application.model.GroupProjectSummary;
import ru.bogdanov.gitlabreviewmcp.application.model.PageResult;
import ru.bogdanov.gitlabreviewmcp.application.model.ProjectDetails;
import ru.bogdanov.gitlabreviewmcp.application.model.RepositoryFileContent;
import ru.bogdanov.gitlabreviewmcp.application.model.RepositorySearchResult;
import ru.bogdanov.gitlabreviewmcp.application.model.RepositoryTreeEntry;

/**
 * Read-only MCP adapter for remote repository analysis.
 */
@Component
@ConditionalOnProperty(prefix = "gitlab-review-mcp", name = "mode", havingValue = "REPOSITORY")
public final class GitLabRepositoryTools {

    private final RepositoryQueryService queryService;
    private final McpToolExecutor executor;

    /**
     * @param queryService remote repository queries
     * @param executor safe tool executor
     */
    public GitLabRepositoryTools(RepositoryQueryService queryService, McpToolExecutor executor) {
        this.queryService = queryService;
        this.executor = executor;
    }

    /**
     * Lists projects belonging to a GitLab group.
     *
     * @param groupUrl full group root URL
     * @param includeSubgroups whether projects from descendant groups should be included
     * @param cursor optional pagination cursor
     * @return group project page
     */
    @McpTool(
            name = "gitlab_list_group_projects",
            description = "Lists one page of projects in a GitLab group and returns project URLs. "
                    + "Projects shared into the group are excluded. Follow nextCursor until absent.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = true))
    public ToolResponse<PageResult<GroupProjectSummary>> listGroupProjects(
            @McpToolParam(required = true, description = "Full group root URL on the configured GitLab origin")
            String groupUrl,
            @McpToolParam(required = false, description = "Include projects from descendant groups; defaults to true")
            Boolean includeSubgroups,
            @McpToolParam(required = false, description = "Opaque cursor from the previous group response")
            String cursor) {
        return executor.execute(() -> queryService.listGroupProjects(
                groupUrl, includeSubgroups == null || includeSubgroups, cursor));
    }

    /**
     * Reads current project metadata and its default branch.
     *
     * @param projectUrl full project root URL
     * @return project metadata response
     */
    @McpTool(
            name = "gitlab_get_project",
            description = "Reads metadata and the current default branch of a remote GitLab project.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = true))
    public ToolResponse<ProjectDetails> getProject(
            @McpToolParam(required = true, description = "Full project root URL on the configured GitLab origin")
            String projectUrl) {
        return executor.execute(() -> queryService.getProject(projectUrl));
    }

    /**
     * Reads one page of the remote project default branch tree.
     *
     * @param projectUrl full project root URL
     * @param path optional repository-relative directory
     * @param recursive whether descendants should be included
     * @param cursor optional pagination cursor
     * @return repository tree response
     */
    @McpTool(
            name = "gitlab_get_repository_tree",
            description = "Lists one bounded page of files and directories in the remote project's current "
                    + "default branch. Follow nextCursor until absent.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = true))
    public ToolResponse<PageResult<RepositoryTreeEntry>> getRepositoryTree(
            @McpToolParam(required = true, description = "Full project root URL on the configured GitLab origin")
            String projectUrl,
            @McpToolParam(required = false, description = "Optional repository-relative directory path") String path,
            @McpToolParam(required = false, description = "Include all descendants; false lists one directory level")
            Boolean recursive,
            @McpToolParam(required = false, description = "Opaque cursor from the previous tree response")
            String cursor) {
        return executor.execute(() -> queryService.getTree(
                projectUrl, path, Boolean.TRUE.equals(recursive), cursor));
    }

    /**
     * Reads a bounded line range from a remote repository text file.
     *
     * @param projectUrl full project root URL
     * @param filePath repository-relative file path
     * @param startLine optional one-based first line
     * @param lineCount optional number of lines
     * @return repository file response
     */
    @McpTool(
            name = "gitlab_get_repository_file",
            description = "Reads a bounded UTF-8 text slice from a file in the remote project's current default "
                    + "branch. Continue from nextStartLine when present.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = true))
    public ToolResponse<RepositoryFileContent> getRepositoryFile(
            @McpToolParam(required = true, description = "Full project root URL on the configured GitLab origin")
            String projectUrl,
            @McpToolParam(required = true, description = "Repository-relative file path") String filePath,
            @McpToolParam(required = false, description = "One-based first line; defaults to 1") Integer startLine,
            @McpToolParam(required = false, description = "Number of lines; defaults to the configured maximum")
            Integer lineCount) {
        return executor.execute(() -> queryService.getFile(projectUrl, filePath, startLine, lineCount));
    }

    /**
     * Searches code in the remote project.
     *
     * @param projectUrl full project root URL
     * @param query GitLab blob search query
     * @param cursor optional pagination cursor
     * @return code search response
     */
    @McpTool(
            name = "gitlab_search_repository_code",
            description = "Searches file names and contents in the remote GitLab project using blob search. "
                    + "GitLab search filters such as filename, path and extension are supported.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = true))
    public ToolResponse<PageResult<RepositorySearchResult>> searchRepositoryCode(
            @McpToolParam(required = true, description = "Full project root URL on the configured GitLab origin")
            String projectUrl,
            @McpToolParam(required = true, description = "Text query, optionally with GitLab blob search filters")
            String query,
            @McpToolParam(required = false, description = "Opaque cursor from the previous search response")
            String cursor) {
        return executor.execute(() -> queryService.searchCode(projectUrl, query, cursor));
    }
}
