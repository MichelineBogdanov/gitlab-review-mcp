package ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import ru.bogdanov.gitlabreviewmcp.application.GitLabClientException;

/**
 * JDK HTTP implementation that never forwards credentials to another origin.
 */
public final class JdkGitLabHttpTransport implements GitLabHttpTransport {

    private static final int MAX_REDIRECTS = 3;
    private final HttpClient httpClient;
    private final URI trustedOrigin;
    private final String trustedPathPrefix;
    private final String token;

    /**
     * Creates the transport.
     *
     * @param httpClient configured JDK client with redirects disabled
     * @param trustedOrigin configured GitLab origin
     * @param token personal access token
     */
    public JdkGitLabHttpTransport(HttpClient httpClient, URI trustedOrigin, String token) {
        this.httpClient = httpClient;
        this.trustedOrigin = trustedOrigin;
        this.trustedPathPrefix = normalizePrefix(trustedOrigin.getPath());
        this.token = token;
    }

    /** {@inheritDoc} */
    @Override
    public GitLabHttpResponse get(URI uri, Duration timeout, long maxBytes) {
        return execute(uri, timeout, maxBytes, "GET", null, false);
    }

    /** {@inheritDoc} */
    @Override
    public GitLabHttpResponse postForm(URI uri, String formBody, Duration timeout, long maxBytes) {
        return execute(uri, timeout, maxBytes, "POST", formBody, true);
    }

    private GitLabHttpResponse execute(
            URI initialUri,
            Duration timeout,
            long maxBytes,
            String method,
            String formBody,
            boolean ambiguousOnIoFailure) {
        URI uri = initialUri;
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            assertTrusted(uri);
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .header("Accept", "application/json")
                    .header("PRIVATE-TOKEN", token);
            if ("POST".equals(method)) {
                builder.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8));
            } else {
                builder.GET();
            }
            try {
                HttpResponse<InputStream> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
                if (isRedirect(response.statusCode())) {
                    response.body().close();
                    if (ambiguousOnIoFailure) {
                        throw new GitLabClientException(
                                "UNEXPECTED_WRITE_REDIRECT",
                                "GitLab redirected a write request; publication outcome was not assumed",
                                response.statusCode(),
                                requestId(response),
                                false);
                    }
                    String location = response.headers().firstValue("Location")
                            .orElseThrow(() -> new GitLabClientException(
                                    "INVALID_REDIRECT", "GitLab redirect has no Location header", response.statusCode(),
                                    requestId(response), false));
                    URI redirected = uri.resolve(location);
                    assertTrusted(redirected);
                    if (redirects == MAX_REDIRECTS) {
                        throw new GitLabClientException(
                                "TOO_MANY_REDIRECTS", "GitLab returned too many redirects", response.statusCode(),
                                requestId(response), false);
                    }
                    uri = redirected;
                    continue;
                }
                byte[] body;
                try {
                    body = readBounded(response.body(), maxBytes);
                } catch (GitLabClientException exception) {
                    if (ambiguousOnIoFailure && response.statusCode() >= 200 && response.statusCode() < 300) {
                        throw new GitLabClientException(
                                "GITLAB_WRITE_RESPONSE_TOO_LARGE",
                                "GitLab accepted the write but returned an oversized response",
                                response.statusCode(),
                                requestId(response),
                                true);
                    }
                    throw exception;
                }
                return new GitLabHttpResponse(response.statusCode(), response.headers(), body, uri);
            } catch (HttpTimeoutException exception) {
                throw new GitLabClientException(
                        "GITLAB_TIMEOUT", "GitLab request timed out", null, null, ambiguousOnIoFailure);
            } catch (IOException exception) {
                throw new GitLabClientException(
                        "GITLAB_IO_ERROR", "GitLab connection failed", null, null, ambiguousOnIoFailure);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new GitLabClientException(
                        "GITLAB_INTERRUPTED", "GitLab request was interrupted", null, null, ambiguousOnIoFailure);
            }
        }
        throw new IllegalStateException("Unreachable redirect state");
    }

    private byte[] readBounded(InputStream input, long maxBytes) throws IOException {
        if (maxBytes > Integer.MAX_VALUE - 1) {
            throw new IllegalArgumentException("Response size limit is too large");
        }
        try (input) {
            byte[] value = input.readNBytes((int) maxBytes + 1);
            if (value.length > maxBytes) {
                throw new GitLabClientException(
                        "RESPONSE_TOO_LARGE", "GitLab response exceeds the configured size limit", null, null, false);
            }
            return value;
        }
    }

    private void assertTrusted(URI uri) {
        String path = uri.getPath() == null ? "" : uri.getPath();
        boolean trustedPath = trustedPathPrefix.isEmpty()
                || path.equals(trustedPathPrefix)
                || path.startsWith(trustedPathPrefix + "/");
        if (!GitLabMergeRequestUrlParser.sameOrigin(trustedOrigin, uri) || !trustedPath) {
            throw new GitLabClientException(
                    "UNTRUSTED_GITLAB_ORIGIN", "Refusing to send credentials to another origin", null, null, false);
        }
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private static String requestId(HttpResponse<?> response) {
        return response.headers().firstValue("X-Request-Id").orElse(null);
    }

    private static String normalizePrefix(String value) {
        if (value == null || value.isBlank() || "/".equals(value)) {
            return "";
        }
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
