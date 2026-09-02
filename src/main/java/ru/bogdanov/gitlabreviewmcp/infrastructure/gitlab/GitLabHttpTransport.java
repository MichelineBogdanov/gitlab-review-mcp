package ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab;

import java.net.URI;
import java.time.Duration;

/**
 * HTTP boundary that enforces bounded bodies and safe redirect handling.
 */
public interface GitLabHttpTransport {

    /**
     * Executes an idempotent authenticated request.
     *
     * @param uri request URI
     * @param timeout response timeout
     * @param maxBytes maximum response bytes
     * @return raw response
     */
    GitLabHttpResponse get(URI uri, Duration timeout, long maxBytes);

    /**
     * Executes a non-retried authenticated form POST.
     *
     * @param uri request URI
     * @param formBody encoded form body
     * @param timeout response timeout
     * @param maxBytes maximum response bytes
     * @return raw response
     */
    GitLabHttpResponse postForm(URI uri, String formBody, Duration timeout, long maxBytes);
}
