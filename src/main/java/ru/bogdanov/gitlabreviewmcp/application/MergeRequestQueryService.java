package ru.bogdanov.gitlabreviewmcp.application;

import java.util.Set;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffFile;
import ru.bogdanov.gitlabreviewmcp.application.model.Discussion;
import ru.bogdanov.gitlabreviewmcp.application.model.MergeRequestDetails;
import ru.bogdanov.gitlabreviewmcp.application.model.PageResult;
import ru.bogdanov.gitlabreviewmcp.application.port.GitLabClient;
import ru.bogdanov.gitlabreviewmcp.application.port.MergeRequestReferenceParser;

/**
 * Read-only merge request use cases exposed to MCP tools.
 */
public final class MergeRequestQueryService {

    private final MergeRequestReferenceParser referenceParser;
    private final GitLabClient gitLabClient;

    /**
     * @param referenceParser secure URL parser
     * @param gitLabClient GitLab API port
     */
    public MergeRequestQueryService(MergeRequestReferenceParser referenceParser, GitLabClient gitLabClient) {
        this.referenceParser = referenceParser;
        this.gitLabClient = gitLabClient;
    }

    /** @param mergeRequestUrl full MR URL @return current metadata */
    public MergeRequestDetails getMergeRequest(String mergeRequestUrl) {
        return gitLabClient.getMergeRequest(referenceParser.parse(mergeRequestUrl));
    }

    /**
     * @param mergeRequestUrl full MR URL
     * @param paths optional paths
     * @param cursor optional cursor
     * @return diff page
     */
    public PageResult<DiffFile> getDiffs(String mergeRequestUrl, Set<String> paths, String cursor) {
        return gitLabClient.getDiffs(referenceParser.parse(mergeRequestUrl), paths == null ? Set.of() : paths, cursor);
    }

    /**
     * @param mergeRequestUrl full MR URL
     * @param cursor optional cursor
     * @return discussion page
     */
    public PageResult<Discussion> getDiscussions(String mergeRequestUrl, String cursor) {
        return gitLabClient.getDiscussions(referenceParser.parse(mergeRequestUrl), cursor);
    }
}
