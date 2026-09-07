package ru.bogdanov.gitlabreviewmcp.application.port;

import ru.bogdanov.gitlabreviewmcp.domain.ProjectRef;

/**
 * Parses and validates a full GitLab project URL.
 */
public interface ProjectReferenceParser {

    /**
     * @param projectUrl full project URL
     * @return trusted project reference
     */
    ProjectRef parse(String projectUrl);
}
