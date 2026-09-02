package ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab;

import java.net.URI;
import java.net.http.HttpHeaders;

/**
 * Bounded raw HTTP response from GitLab.
 *
 * @param statusCode HTTP status
 * @param headers response headers
 * @param body bounded response body
 * @param uri final response URI
 */
public record GitLabHttpResponse(int statusCode, HttpHeaders headers, byte[] body, URI uri) {
}
