package ru.bogdanov.gitlabreviewmcp.domain;

import java.net.URI;

/**
 * GitLab identifiers returned after a discussion is created.
 *
 * @param discussionId discussion identifier
 * @param noteId note identifier
 * @param webUrl merge request URL
 */
public record PublicationReceipt(String discussionId, long noteId, URI webUrl) {
}
