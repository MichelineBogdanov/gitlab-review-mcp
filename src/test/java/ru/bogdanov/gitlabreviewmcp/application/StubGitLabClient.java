package ru.bogdanov.gitlabreviewmcp.application;

import java.util.List;
import java.util.Set;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffFile;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffVersion;
import ru.bogdanov.gitlabreviewmcp.application.model.Discussion;
import ru.bogdanov.gitlabreviewmcp.application.model.GitLabConnectionInfo;
import ru.bogdanov.gitlabreviewmcp.application.model.MergeRequestDetails;
import ru.bogdanov.gitlabreviewmcp.application.model.PageResult;
import ru.bogdanov.gitlabreviewmcp.application.model.ProjectDetails;
import ru.bogdanov.gitlabreviewmcp.application.model.RepositoryFileContent;
import ru.bogdanov.gitlabreviewmcp.application.model.RepositorySearchResult;
import ru.bogdanov.gitlabreviewmcp.application.model.RepositoryTreeEntry;
import ru.bogdanov.gitlabreviewmcp.application.port.GitLabClient;
import ru.bogdanov.gitlabreviewmcp.domain.MergeRequestRef;
import ru.bogdanov.gitlabreviewmcp.domain.ProjectRef;
import ru.bogdanov.gitlabreviewmcp.domain.PublicationReceipt;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewCommentDraft;

abstract class StubGitLabClient implements GitLabClient {

    @Override
    public GitLabConnectionInfo checkConnection() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectDetails getProject(ProjectRef reference) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PageResult<RepositoryTreeEntry> getRepositoryTree(
            ProjectRef reference, String path, boolean recursive, String cursor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RepositoryFileContent getRepositoryFile(
            ProjectRef reference, String filePath, Integer startLine, Integer lineCount) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PageResult<RepositorySearchResult> searchRepositoryCode(
            ProjectRef reference, String query, String cursor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MergeRequestDetails getMergeRequest(MergeRequestRef reference) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DiffVersion> getDiffVersions(MergeRequestRef reference) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PageResult<DiffFile> getDiffs(MergeRequestRef reference, Set<String> paths, String cursor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DiffFile> getAllDiffs(MergeRequestRef reference) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PageResult<Discussion> getDiscussions(MergeRequestRef reference, String cursor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PublicationReceipt createDiscussion(
            MergeRequestRef reference,
            ReviewCommentDraft comment,
            DiffVersion version) {
        throw new UnsupportedOperationException();
    }
}
