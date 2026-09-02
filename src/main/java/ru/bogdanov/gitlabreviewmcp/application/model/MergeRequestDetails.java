package ru.bogdanov.gitlabreviewmcp.application.model;

import java.net.URI;
import ru.bogdanov.gitlabreviewmcp.domain.MergeRequestRef;

/**
 * Merge request metadata required by code review workflows.
 *
 * @param reference canonical merge request reference
 * @param title title
 * @param description description
 * @param state state
 * @param sourceBranch source branch
 * @param targetBranch target branch
 * @param headSha current head commit SHA
 * @param author author
 * @param webUrl browser URL
 * @param gitLabRequestId GitLab request identifier
 */
public record MergeRequestDetails(
        MergeRequestRef reference,
        String title,
        String description,
        String state,
        String sourceBranch,
        String targetBranch,
        String headSha,
        GitLabUser author,
        URI webUrl,
        String gitLabRequestId) {
}
