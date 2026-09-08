package ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;
import ru.bogdanov.gitlabreviewmcp.application.ReviewApplicationException;

class GitLabGroupUrlParserTest {

    private final GitLabGroupUrlParser parser =
            new GitLabGroupUrlParser(URI.create("https://gitlab.example.com/gitlab"));

    @Test
    void parsesNestedGroupAndNormalizesTrailingSlash() {
        var result = parser.parse("https://gitlab.example.com/gitlab/platform/backend/");

        assertThat(result.groupPath()).isEqualTo("platform/backend");
        assertThat(result.webUrl()).hasToString("https://gitlab.example.com/gitlab/platform/backend");
    }

    @Test
    void parsesTopLevelGroupAndPreservesPlus() {
        var result = parser.parse("https://gitlab.example.com/gitlab/a+b");

        assertThat(result.groupPath()).isEqualTo("a+b");
    }

    @Test
    void rejectsResourceUrlsAndAnotherOrigin() {
        assertThatThrownBy(() -> parser.parse(
                "https://gitlab.example.com/gitlab/group/-/projects"))
                .isInstanceOf(ReviewApplicationException.class);
        assertThatThrownBy(() -> parser.parse("https://evil.example.com/gitlab/group"))
                .isInstanceOf(ReviewApplicationException.class);
        assertThatThrownBy(() -> parser.parse("https://gitlab.example.com/gitlab/"))
                .isInstanceOf(ReviewApplicationException.class);
    }

    @Test
    void rejectsQueryFragmentAndCredentials() {
        assertThatThrownBy(() -> parser.parse("https://gitlab.example.com/gitlab/group?x=1"))
                .isInstanceOf(ReviewApplicationException.class);
        assertThatThrownBy(() -> parser.parse("https://user@gitlab.example.com/gitlab/group"))
                .isInstanceOf(ReviewApplicationException.class);
        assertThatThrownBy(() -> parser.parse("https://gitlab.example.com/gitlab/group#projects"))
                .isInstanceOf(ReviewApplicationException.class);
    }
}
