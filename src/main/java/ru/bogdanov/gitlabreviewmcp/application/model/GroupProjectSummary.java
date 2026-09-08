package ru.bogdanov.gitlabreviewmcp.application.model;

import java.net.URI;
import java.time.Instant;

/**
 * Project returned while listing a GitLab group.
 *
 * @param id GitLab project identifier
 * @param name project name
 * @param pathWithNamespace namespace-qualified project path
 * @param webUrl full project URL
 * @param description project description
 * @param defaultBranch current default branch
 * @param archived whether the project is archived
 * @param emptyRepo whether the repository is empty
 * @param visibility GitLab project visibility
 * @param lastActivityAt last project activity timestamp
 */
public record GroupProjectSummary(
        long id,
        String name,
        String pathWithNamespace,
        URI webUrl,
        String description,
        String defaultBranch,
        boolean archived,
        boolean emptyRepo,
        String visibility,
        Instant lastActivityAt) {
}
