package ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import ru.bogdanov.gitlabreviewmcp.application.ReviewApplicationException;
import ru.bogdanov.gitlabreviewmcp.application.port.ProjectReferenceParser;
import ru.bogdanov.gitlabreviewmcp.domain.ProjectRef;

/**
 * Strict parser that accepts project roots only from the configured GitLab origin and URL prefix.
 */
public final class GitLabProjectUrlParser implements ProjectReferenceParser {

    private final URI baseUrl;
    private final String basePath;

    /** @param baseUrl configured GitLab base URL */
    public GitLabProjectUrlParser(URI baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.basePath = normalizePrefix(this.baseUrl.getPath());
    }

    /** {@inheritDoc} */
    @Override
    public ProjectRef parse(String projectUrl) {
        final URI candidate;
        try {
            candidate = URI.create(projectUrl);
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
        String rawProject = rawPath.substring(basePath.length() + 1);
        String projectPath = decodePath(rawProject);
        if (!isProjectPath(projectPath)) {
            throw invalidUrl();
        }
        return new ProjectRef(URI.create(baseUrl + "/" + rawProject), projectPath);
    }

    private static boolean isProjectPath(String value) {
        if (value.isBlank() || value.startsWith("/") || value.endsWith("/") || value.contains("/-/")) {
            return false;
        }
        String[] segments = value.split("/", -1);
        if (segments.length < 2) {
            return false;
        }
        for (String segment : segments) {
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
                "INVALID_PROJECT_URL",
                "Expected a full project root URL on the configured GitLab origin");
    }
}
