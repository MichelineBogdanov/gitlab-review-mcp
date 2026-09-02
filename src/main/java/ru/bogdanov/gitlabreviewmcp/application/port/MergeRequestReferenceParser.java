package ru.bogdanov.gitlabreviewmcp.application.port;

import ru.bogdanov.gitlabreviewmcp.domain.MergeRequestRef;

/**
 * Parses and validates a merge request browser URL.
 */
public interface MergeRequestReferenceParser {

    /**
     * Parses a full URL and enforces the configured GitLab origin.
     *
     * @param mergeRequestUrl full browser URL
     * @return canonical reference
     */
    MergeRequestRef parse(String mergeRequestUrl);
}
