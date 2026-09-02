package ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;
import ru.bogdanov.gitlabreviewmcp.application.ReviewApplicationException;

class GitLabMergeRequestUrlParserTest {

    private final GitLabMergeRequestUrlParser parser =
            new GitLabMergeRequestUrlParser(URI.create("https://gitlab.example.com/gitlab"));

    @Test
    void parsesNestedProjectAndPrefix() {
        var result = parser.parse(
                "https://gitlab.example.com/gitlab/platform/backend/service/-/merge_requests/42");

        assertThat(result.projectPath()).isEqualTo("platform/backend/service");
        assertThat(result.iid()).isEqualTo(42);
        assertThat(result.webUrl()).hasToString(
                "https://gitlab.example.com/gitlab/platform/backend/service/-/merge_requests/42");
    }

    @Test
    void decodesProjectPathWithoutTreatingPlusAsSpace() {
        var result = parser.parse("https://gitlab.example.com/gitlab/group/a+b/-/merge_requests/1");

        assertThat(result.projectPath()).isEqualTo("group/a+b");
    }

    @Test
    void rejectsAnotherOriginAndPort() {
        assertThatThrownBy(() -> parser.parse("https://evil.example.com/gitlab/group/p/-/merge_requests/1"))
                .isInstanceOf(ReviewApplicationException.class);
        assertThatThrownBy(() -> parser.parse("https://gitlab.example.com:8443/gitlab/group/p/-/merge_requests/1"))
                .isInstanceOf(ReviewApplicationException.class);
    }

    @Test
    void rejectsQueryFragmentCredentialsAndMalformedPaths() {
        assertThatThrownBy(() -> parser.parse(
                "https://gitlab.example.com/gitlab/group/p/-/merge_requests/1?x=1"))
                .isInstanceOf(ReviewApplicationException.class);
        assertThatThrownBy(() -> parser.parse(
                "https://user@gitlab.example.com/gitlab/group/p/-/merge_requests/1"))
                .isInstanceOf(ReviewApplicationException.class);
        assertThatThrownBy(() -> parser.parse(
                "https://gitlab.example.com/gitlab/group/p/-/merge_requests/x"))
                .isInstanceOf(ReviewApplicationException.class);
    }
}
