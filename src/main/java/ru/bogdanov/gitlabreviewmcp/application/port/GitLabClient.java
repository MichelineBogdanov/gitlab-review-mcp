package ru.bogdanov.gitlabreviewmcp.application.port;

import java.util.List;
import java.util.Set;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffFile;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffVersion;
import ru.bogdanov.gitlabreviewmcp.application.model.Discussion;
import ru.bogdanov.gitlabreviewmcp.application.model.GitLabConnectionInfo;
import ru.bogdanov.gitlabreviewmcp.application.model.GroupProjectSummary;
import ru.bogdanov.gitlabreviewmcp.application.model.MergeRequestDetails;
import ru.bogdanov.gitlabreviewmcp.application.model.PageResult;
import ru.bogdanov.gitlabreviewmcp.application.model.ProjectDetails;
import ru.bogdanov.gitlabreviewmcp.application.model.RepositoryFileContent;
import ru.bogdanov.gitlabreviewmcp.application.model.RepositorySearchResult;
import ru.bogdanov.gitlabreviewmcp.application.model.RepositoryTreeEntry;
import ru.bogdanov.gitlabreviewmcp.domain.GroupRef;
import ru.bogdanov.gitlabreviewmcp.domain.MergeRequestRef;
import ru.bogdanov.gitlabreviewmcp.domain.ProjectRef;
import ru.bogdanov.gitlabreviewmcp.domain.PublicationReceipt;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewCommentDraft;

/**
 * Application port for the narrow GitLab API surface exposed by this MCP server.
 */
public interface GitLabClient {

    /** @return verified connection information */
    GitLabConnectionInfo checkConnection();

    /**
     * Returns one page of projects belonging to a group.
     *
     * @param reference group reference
     * @param includeSubgroups whether projects from descendant groups should be included
     * @param cursor optional pagination cursor
     * @return group project page
     */
    PageResult<GroupProjectSummary> getGroupProjects(
            GroupRef reference, boolean includeSubgroups, String cursor);

    /** @param reference project reference @return current project metadata */
    ProjectDetails getProject(ProjectRef reference);

    /**
     * Returns one page of the current default branch tree.
     *
     * @param reference project reference
     * @param path optional directory path
     * @param recursive whether descendants should be included
     * @param cursor optional pagination cursor
     * @return repository tree page
     */
    PageResult<RepositoryTreeEntry> getRepositoryTree(
            ProjectRef reference, String path, boolean recursive, String cursor);

    /**
     * Reads a bounded text slice from the current default branch.
     *
     * @param reference project reference
     * @param filePath repository-relative file path
     * @param startLine optional one-based first line
     * @param lineCount optional number of lines
     * @return file content slice
     */
    RepositoryFileContent getRepositoryFile(
            ProjectRef reference, String filePath, Integer startLine, Integer lineCount);

    /**
     * Searches the current project code using GitLab blob search.
     *
     * @param reference project reference
     * @param query search query
     * @param cursor optional pagination cursor
     * @return search result page
     */
    PageResult<RepositorySearchResult> searchRepositoryCode(ProjectRef reference, String query, String cursor);

    /** @param reference merge request reference @return current metadata */
    MergeRequestDetails getMergeRequest(MergeRequestRef reference);

    /** @param reference merge request reference @return available diff versions */
    List<DiffVersion> getDiffVersions(MergeRequestRef reference);

    /**
     * Returns one bounded page of diffs.
     *
     * @param reference merge request reference
     * @param paths optional path filter
     * @param cursor optional opaque cursor
     * @return diff page
     */
    PageResult<DiffFile> getDiffs(MergeRequestRef reference, Set<String> paths, String cursor);

    /** @param reference merge request reference @return all diffs needed for validation */
    List<DiffFile> getAllDiffs(MergeRequestRef reference);

    /**
     * Returns one bounded discussion page.
     *
     * @param reference merge request reference
     * @param cursor optional opaque cursor
     * @return discussion page
     */
    PageResult<Discussion> getDiscussions(MergeRequestRef reference, String cursor);

    /**
     * Creates a new overview or inline discussion.
     *
     * @param reference merge request reference
     * @param comment normalized comment
     * @param version current diff version
     * @return publication receipt
     */
    PublicationReceipt createDiscussion(
            MergeRequestRef reference,
            ReviewCommentDraft comment,
            DiffVersion version);
}
