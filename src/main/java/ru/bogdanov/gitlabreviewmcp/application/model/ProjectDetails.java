package ru.bogdanov.gitlabreviewmcp.application.model;

import java.time.Instant;
import ru.bogdanov.gitlabreviewmcp.domain.ProjectRef;

/**
 * Current GitLab project metadata.
 *
 * @param reference trusted project reference
 * @param id GitLab project identifier
 * @param name project name
 * @param nameWithNamespace namespace-qualified display name
 * @param description project description
 * @param defaultBranch current default branch
 * @param archived whether the project is archived
 * @param emptyRepo whether the repository is empty
 * @param lastActivityAt last project activity timestamp
 * @param gitLabRequestId GitLab request identifier
 */
public record ProjectDetails(
        ProjectRef reference,
        long id,
        String name,
        String nameWithNamespace,
        String description,
        String defaultBranch,
        boolean archived,
        boolean emptyRepo,
        Instant lastActivityAt,
        String gitLabRequestId) {
}
