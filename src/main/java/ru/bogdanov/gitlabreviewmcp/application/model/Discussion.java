package ru.bogdanov.gitlabreviewmcp.application.model;

import java.util.List;

/**
 * Threaded GitLab discussion.
 *
 * @param id discussion identifier
 * @param individualNote whether this is a standalone note
 * @param notes ordered notes and replies
 */
public record Discussion(String id, boolean individualNote, List<DiscussionNote> notes) {
}
