package ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

/**
 * Retry decorator for idempotent GitLab GET requests.
 */
public final class RetryingGitLabHttpTransport implements GitLabHttpTransport {

    private static final Set<Integer> RETRYABLE = Set.of(429, 502, 503, 504);
    private final GitLabHttpTransport delegate;
    private final int maxRetries;

    /**
     * Creates the decorator.
     *
     * @param delegate raw transport
     * @param maxRetries maximum additional attempts
     */
    public RetryingGitLabHttpTransport(GitLabHttpTransport delegate, int maxRetries) {
        this.delegate = delegate;
        this.maxRetries = maxRetries;
    }

    /** {@inheritDoc} */
    @Override
    public GitLabHttpResponse get(URI uri, Duration timeout, long maxBytes) {
        GitLabHttpResponse response = delegate.get(uri, timeout, maxBytes);
        for (int attempt = 0; attempt < maxRetries && RETRYABLE.contains(response.statusCode()); attempt++) {
            pause(retryDelay(response, attempt));
            response = delegate.get(uri, timeout, maxBytes);
        }
        return response;
    }

    /** {@inheritDoc} */
    @Override
    public GitLabHttpResponse postForm(URI uri, String formBody, Duration timeout, long maxBytes) {
        return delegate.postForm(uri, formBody, timeout, maxBytes);
    }

    private Duration retryDelay(GitLabHttpResponse response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .flatMap(this::parseRetryAfter)
                .orElse(Duration.ofMillis(250L * (1L << attempt)));
    }

    private java.util.Optional<Duration> parseRetryAfter(String value) {
        try {
            return java.util.Optional.of(Duration.ofSeconds(Math.min(30, Math.max(0, Long.parseLong(value.strip())))));
        } catch (NumberFormatException exception) {
            return java.util.Optional.empty();
        }
    }

    private void pause(Duration delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
