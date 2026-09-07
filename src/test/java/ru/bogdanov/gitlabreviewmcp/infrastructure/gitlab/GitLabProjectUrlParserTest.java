package ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;
import ru.bogdanov.gitlabreviewmcp.application.ReviewApplicationException;

class GitLabProjectUrlParserTest {

    private final GitLabProjectUrlParser parser =
            new GitLabProjectUrlParser(URI.create("https://gitlab.example.com/gitlab"));

    @Test
    void parsesNestedProjectAndNormalizesTrailingSlash() {
        var result = parser.parse("https://gitlab.example.com/gitlab/platform/backend/service/");

        assertThat(result.projectPath()).isEqualTo("platform/backend/service");
        assertThat(result.webUrl()).hasToString(
                "https://gitlab.example.com/gitlab/platform/backend/service");
    }

    @Test
    void decodesProjectPathWithoutTreatingPlusAsSpace() {
        var result = parser.parse("https://gitlab.example.com/gitlab/group/a+b");

        assertThat(result.projectPath()).isEqualTo("group/a+b");
    }

    @Test
    void rejectsNonProjectUrlsAndAnotherOrigin() {
        assertThatThrownBy(() -> parser.parse(
                "https://gitlab.example.com/gitlab/group/project/-/blob/main/README.md"))
                .isInstanceOf(ReviewApplicationException.class);
        assertThatThrownBy(() -> parser.parse("https://evil.example.com/gitlab/group/project"))
                .isInstanceOf(ReviewApplicationException.class);
        assertThatThrownBy(() -> parser.parse("https://gitlab.example.com/gitlab/project"))
                .isInstanceOf(ReviewApplicationException.class);
    }

    @Test
    void rejectsQueryFragmentAndCredentials() {
        assertThatThrownBy(() -> parser.parse("https://gitlab.example.com/gitlab/group/project?x=1"))
                .isInstanceOf(ReviewApplicationException.class);
        assertThatThrownBy(() -> parser.parse("https://user@gitlab.example.com/gitlab/group/project"))
                .isInstanceOf(ReviewApplicationException.class);
        assertThatThrownBy(() -> parser.parse("https://gitlab.example.com/gitlab/group/project#readme"))
                .isInstanceOf(ReviewApplicationException.class);
    }
}
