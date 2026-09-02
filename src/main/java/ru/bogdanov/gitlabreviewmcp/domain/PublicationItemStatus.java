package ru.bogdanov.gitlabreviewmcp.domain;

/**
 * Publication state of one proposed comment.
 */
public enum PublicationItemStatus {
    /** The comment can be safely submitted. */
    PENDING,
    /** GitLab confirmed creation of the discussion. */
    PUBLISHED,
    /** It is unknown whether GitLab accepted the write. */
    UNKNOWN
}
