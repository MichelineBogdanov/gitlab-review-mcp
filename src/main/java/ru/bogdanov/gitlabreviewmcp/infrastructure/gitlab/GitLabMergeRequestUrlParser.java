package ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import ru.bogdanov.gitlabreviewmcp.application.ReviewApplicationException;
import ru.bogdanov.gitlabreviewmcp.application.port.MergeRequestReferenceParser;
import ru.bogdanov.gitlabreviewmcp.domain.MergeRequestRef;

/**
 * Strict parser that accepts merge requests only from the configured GitLab origin and URL prefix.
 */
public final class GitLabMergeRequestUrlParser implements MergeRequestReferenceParser {

    private static final String MARKER = "/-/merge_requests/";
    private final URI baseUrl;
    private final String basePath;

    /** @param baseUrl configured GitLab base URL */
    public GitLabMergeRequestUrlParser(URI baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.basePath = normalizePrefix(this.baseUrl.getPath());
    }

    /** {@inheritDoc} */
    @Override
    public MergeRequestRef parse(String mergeRequestUrl) {
        final URI candidate;
        try {
            candidate = URI.create(mergeRequestUrl);
        } catch (RuntimeException exception) {
            throw invalidUrl();
        }
        if (!sameOrigin(baseUrl, candidate)
                || candidate.getRawUserInfo() != null
                || candidate.getRawQuery() != null
                || candidate.getRawFragment() != null) {
            throw invalidUrl();
        }
        String rawPath = candidate.getRawPath();
        if (rawPath == null || !rawPath.startsWith(basePath + "/")) {
            throw invalidUrl();
        }
        String relative = rawPath.substring(basePath.length());
        int marker = relative.lastIndexOf(MARKER);
        if (marker <= 0 || marker + MARKER.length() >= relative.length()) {
            throw invalidUrl();
        }
        String iidValue = relative.substring(marker + MARKER.length());
        if (!iidValue.chars().allMatch(Character::isDigit)) {
            throw invalidUrl();
        }
        long iid;
        try {
            iid = Long.parseLong(iidValue);
        } catch (NumberFormatException exception) {
            throw invalidUrl();
        }
        if (iid < 1) {
            throw invalidUrl();
        }
        String rawProject = relative.substring(1, marker);
        String projectPath = decodePath(rawProject);
        if (projectPath.isBlank() || projectPath.contains("..") || projectPath.startsWith("/") || projectPath.endsWith("/")) {
            throw invalidUrl();
        }
        URI canonical = URI.create(baseUrl.toString() + "/" + rawProject + MARKER + iid);
        return new MergeRequestRef(canonical, projectPath, iid);
    }

    /**
     * Checks that two URLs share the exact scheme, host and effective port.
     *
     * @param expected configured origin
     * @param actual candidate origin
     * @return whether the origins match
     */
    public static boolean sameOrigin(URI expected, URI actual) {
        return expected.getScheme() != null
                && actual.getScheme() != null
                && expected.getScheme().equalsIgnoreCase(actual.getScheme())
                && expected.getHost() != null
                && actual.getHost() != null
                && expected.getHost().equalsIgnoreCase(actual.getHost())
                && effectivePort(expected) == effectivePort(actual);
    }

    private static URI normalizeBaseUrl(URI value) {
        String text = value.toString();
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return URI.create(text);
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

    private static int effectivePort(URI value) {
        if (value.getPort() >= 0) {
            return value.getPort();
        }
        return "https".equals(value.getScheme().toLowerCase(Locale.ROOT)) ? 443 : 80;
    }

    private static String decodePath(String value) {
        try {
            return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw invalidUrl();
        }
    }

    private static ReviewApplicationException invalidUrl() {
        return new ReviewApplicationException(
                "INVALID_MERGE_REQUEST_URL",
                "Expected a full merge request URL on the configured GitLab origin");
    }
}
