package ru.bogdanov.gitlabreviewmcp.domain;

/**
 * Side of a merge request diff targeted by an inline comment.
 */
public enum DiffSide {
    /** The old or deleted side. */
    OLD,
    /** The new or added side. */
    NEW
}
