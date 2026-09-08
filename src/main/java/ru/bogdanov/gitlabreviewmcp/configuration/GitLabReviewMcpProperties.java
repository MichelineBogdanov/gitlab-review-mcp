package ru.bogdanov.gitlabreviewmcp.configuration;

import jakarta.validation.constraints.NotNull;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Selects the mutually exclusive MCP tool set exposed by the server process.
 */
@Validated
@ConfigurationProperties(prefix = "gitlab-review-mcp")
public final class GitLabReviewMcpProperties {

    @NotNull
    private Mode mode = Mode.REVIEW;

    /** @return configured server mode */
    public Mode getMode() {
        return mode;
    }

    /** @param mode configured server mode */
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /**
     * MCP server operating mode and its externally exposed capabilities.
     */
    public enum Mode {
        /** Merge request review tools; Codex reads source code from its local workspace. */
        REVIEW(Set.of("READ_MERGE_REQUEST", "READ_DIFF", "READ_DISCUSSIONS", "PUBLISH_DISCUSSIONS")),

        /** Read-only project and repository tools backed exclusively by GitLab APIs. */
        REPOSITORY(Set.of(
                "LIST_GROUP_PROJECTS",
                "READ_PROJECT",
                "READ_REPOSITORY_TREE",
                "READ_REPOSITORY_FILE",
                "SEARCH_REPOSITORY_CODE"));

        private final Set<String> capabilities;

        Mode(Set<String> capabilities) {
            this.capabilities = capabilities;
        }

        /** @return immutable capabilities exposed in this mode */
        public Set<String> capabilities() {
            return capabilities;
        }
    }
}
