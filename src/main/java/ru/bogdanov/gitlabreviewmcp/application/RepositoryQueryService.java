package ru.bogdanov.gitlabreviewmcp.application;

import ru.bogdanov.gitlabreviewmcp.application.model.PageResult;
import ru.bogdanov.gitlabreviewmcp.application.model.ProjectDetails;
import ru.bogdanov.gitlabreviewmcp.application.model.RepositoryFileContent;
import ru.bogdanov.gitlabreviewmcp.application.model.RepositorySearchResult;
import ru.bogdanov.gitlabreviewmcp.application.model.RepositoryTreeEntry;
import ru.bogdanov.gitlabreviewmcp.application.port.GitLabClient;
import ru.bogdanov.gitlabreviewmcp.application.port.ProjectReferenceParser;

/**
 * Read-only use cases for inspecting a remote GitLab repository default branch.
 */
public final class RepositoryQueryService {

    private final ProjectReferenceParser referenceParser;
    private final GitLabClient gitLabClient;

    /**
     * @param referenceParser secure project URL parser
     * @param gitLabClient GitLab API port
     */
    public RepositoryQueryService(ProjectReferenceParser referenceParser, GitLabClient gitLabClient) {
        this.referenceParser = referenceParser;
        this.gitLabClient = gitLabClient;
    }

    /** @param projectUrl full project URL @return current project metadata */
    public ProjectDetails getProject(String projectUrl) {
        return gitLabClient.getProject(referenceParser.parse(projectUrl));
    }

    /**
     * @param projectUrl full project URL
     * @param path optional directory path
     * @param recursive whether descendants should be included
     * @param cursor optional pagination cursor
     * @return repository tree page
     */
    public PageResult<RepositoryTreeEntry> getTree(
            String projectUrl, String path, boolean recursive, String cursor) {
        return gitLabClient.getRepositoryTree(referenceParser.parse(projectUrl), path, recursive, cursor);
    }

    /**
     * @param projectUrl full project URL
     * @param filePath repository-relative file path
     * @param startLine optional one-based first line
     * @param lineCount optional number of lines
     * @return bounded file content
     */
    public RepositoryFileContent getFile(
            String projectUrl, String filePath, Integer startLine, Integer lineCount) {
        return gitLabClient.getRepositoryFile(
                referenceParser.parse(projectUrl), filePath, startLine, lineCount);
    }

    /**
     * @param projectUrl full project URL
     * @param query GitLab code search query
     * @param cursor optional pagination cursor
     * @return code search result page
     */
    public PageResult<RepositorySearchResult> searchCode(String projectUrl, String query, String cursor) {
        return gitLabClient.searchRepositoryCode(referenceParser.parse(projectUrl), query, cursor);
    }
}
