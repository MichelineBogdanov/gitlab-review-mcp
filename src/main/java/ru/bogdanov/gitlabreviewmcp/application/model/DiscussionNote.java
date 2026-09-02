package ru.bogdanov.gitlabreviewmcp.application.model;

import java.time.Instant;

/**
 * One note in a merge request discussion.
 *
 * @param id note identifier
 * @param body Markdown body
 * @param author author
 * @param createdAt creation time
 * @param system whether GitLab generated the note
 * @param resolvable whether the note can be resolved
 * @param resolved whether the note is resolved
 * @param position code position, if present
 */
public record DiscussionNote(
        long id,
        String body,
        GitLabUser author,
        Instant createdAt,
        boolean system,
        boolean resolvable,
        boolean resolved,
        DiscussionPosition position) {
}
