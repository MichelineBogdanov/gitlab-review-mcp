package ru.bogdanov.gitlabreviewmcp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;
import ru.bogdanov.gitlabreviewmcp.application.model.GitLabConnectionInfo;
import ru.bogdanov.gitlabreviewmcp.application.port.GitLabClient;

class GitLabConnectionServiceTest {

    @Test
    void acceptsMinimumAndEnterpriseEditionSuffix() {
        GitLabConnectionInfo result = new GitLabConnectionService(client("17.2.1-ee.0")).checkConnection();

        assertThat(result.gitLabVersion()).isEqualTo("17.2.1-ee.0");
    }

    @Test
    void rejectsOlderAndMalformedVersions() {
        assertThatThrownBy(() -> new GitLabConnectionService(client("17.2.0-ee")).checkConnection())
                .isInstanceOf(ReviewApplicationException.class)
                .hasMessageContaining("17.2.1");
        assertThatThrownBy(() -> new GitLabConnectionService(client("unknown")).checkConnection())
                .isInstanceOf(ReviewApplicationException.class);
    }

    private GitLabClient client(String version) {
        return new StubGitLabClient() {
            @Override
            public GitLabConnectionInfo checkConnection() {
                return new GitLabConnectionInfo("OK", version, "user", Set.of(), "request");
            }
        };
    }
}
