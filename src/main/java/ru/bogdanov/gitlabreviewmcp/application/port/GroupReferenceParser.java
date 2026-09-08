package ru.bogdanov.gitlabreviewmcp.application.port;

import ru.bogdanov.gitlabreviewmcp.domain.GroupRef;

/**
 * Parses and validates a full GitLab group URL.
 */
public interface GroupReferenceParser {

    /**
     * Parses a group URL on the configured GitLab origin.
     *
     * @param groupUrl full group URL
     * @return trusted group reference
     */
    GroupRef parse(String groupUrl);
}
