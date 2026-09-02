package ru.bogdanov.gitlabreviewmcp.application;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ru.bogdanov.gitlabreviewmcp.application.model.GitLabConnectionInfo;
import ru.bogdanov.gitlabreviewmcp.application.port.GitLabClient;

/**
 * Verifies GitLab connectivity and the minimum supported version.
 */
public final class GitLabConnectionService {

    private static final Pattern VERSION = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+).*$");
    private static final SemanticVersion MINIMUM = new SemanticVersion(17, 2, 1);

    private final GitLabClient gitLabClient;

    /** @param gitLabClient GitLab API port */
    public GitLabConnectionService(GitLabClient gitLabClient) {
        this.gitLabClient = gitLabClient;
    }

    /** @return verified connection information */
    public GitLabConnectionInfo checkConnection() {
        GitLabConnectionInfo info = gitLabClient.checkConnection();
        SemanticVersion current = parse(info.gitLabVersion());
        if (current.compareTo(MINIMUM) < 0) {
            throw new ReviewApplicationException(
                    "UNSUPPORTED_GITLAB_VERSION",
                    "GitLab 17.2.1 or newer is required; connected version is " + info.gitLabVersion());
        }
        return info;
    }

    private SemanticVersion parse(String value) {
        Matcher matcher = VERSION.matcher(value == null ? "" : value);
        if (!matcher.matches()) {
            throw new ReviewApplicationException("INVALID_GITLAB_VERSION", "GitLab returned an unknown version format");
        }
        return new SemanticVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)));
    }

    private record SemanticVersion(int major, int minor, int patch) implements Comparable<SemanticVersion> {
        @Override
        public int compareTo(SemanticVersion other) {
            int majorResult = Integer.compare(major, other.major);
            if (majorResult != 0) {
                return majorResult;
            }
            int minorResult = Integer.compare(minor, other.minor);
            return minorResult != 0 ? minorResult : Integer.compare(patch, other.patch);
        }
    }
}
