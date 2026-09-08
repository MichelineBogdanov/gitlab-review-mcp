package ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import ru.bogdanov.gitlabreviewmcp.application.ReviewApplicationException;
import ru.bogdanov.gitlabreviewmcp.application.port.GroupReferenceParser;
import ru.bogdanov.gitlabreviewmcp.domain.GroupRef;

/**
 * Strict parser that accepts group roots only from the configured GitLab origin and URL prefix.
 */
public final class GitLabGroupUrlParser implements GroupReferenceParser {

    private final URI baseUrl;
    private final String basePath;

    /**
     * Creates the parser for one trusted GitLab instance.
     *
     * @param baseUrl configured GitLab base URL
     */
    public GitLabGroupUrlParser(URI baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.basePath = normalizePrefix(this.baseUrl.getPath());
    }

    /** {@inheritDoc} */
    @Override
    public GroupRef parse(String groupUrl) {
        final URI candidate;
        try {
            candidate = URI.create(groupUrl);
        } catch (RuntimeException exception) {
            throw invalidUrl();
        }
        if (!GitLabMergeRequestUrlParser.sameOrigin(baseUrl, candidate)
                || candidate.getRawUserInfo() != null
                || candidate.getRawQuery() != null
                || candidate.getRawFragment() != null) {
            throw invalidUrl();
        }
        String rawPath = stripTrailingSlash(candidate.getRawPath());
        if (rawPath == null || !rawPath.startsWith(basePath + "/")) {
            throw invalidUrl();
        }
        String rawGroup = rawPath.substring(basePath.length() + 1);
        String groupPath = decodePath(rawGroup);
        if (!isGroupPath(groupPath)) {
            throw invalidUrl();
        }
        return new GroupRef(URI.create(baseUrl + "/" + rawGroup), groupPath);
    }

    private static boolean isGroupPath(String value) {
        if (value.isBlank() || value.startsWith("/") || value.endsWith("/") || value.contains("/-/")) {
            return false;
        }
        for (String segment : value.split("/", -1)) {
            if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
                return false;
            }
        }
        return true;
    }

    private static URI normalizeBaseUrl(URI value) {
        return URI.create(stripTrailingSlash(value.toString()));
    }

    private static String normalizePrefix(String value) {
        if (value == null || value.isBlank() || "/".equals(value)) {
            return "";
        }
        return stripTrailingSlash(value);
    }

    private static String stripTrailingSlash(String value) {
        String result = value;
        while (result != null && result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
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
                "INVALID_GROUP_URL",
                "Expected a full group root URL on the configured GitLab origin");
    }
}
