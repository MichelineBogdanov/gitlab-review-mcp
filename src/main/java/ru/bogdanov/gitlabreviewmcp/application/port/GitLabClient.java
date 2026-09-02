package ru.bogdanov.gitlabreviewmcp.application.port;

import java.util.List;
import java.util.Set;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffFile;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffVersion;
import ru.bogdanov.gitlabreviewmcp.application.model.Discussion;
import ru.bogdanov.gitlabreviewmcp.application.model.GitLabConnectionInfo;
import ru.bogdanov.gitlabreviewmcp.application.model.MergeRequestDetails;
import ru.bogdanov.gitlabreviewmcp.application.model.PageResult;
import ru.bogdanov.gitlabreviewmcp.domain.MergeRequestRef;
import ru.bogdanov.gitlabreviewmcp.domain.PublicationReceipt;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewCommentDraft;

/**
 * Application port for the narrow GitLab API surface exposed by this MCP server.
 */
public interface GitLabClient {

    /** @return verified connection information */
    GitLabConnectionInfo checkConnection();

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
