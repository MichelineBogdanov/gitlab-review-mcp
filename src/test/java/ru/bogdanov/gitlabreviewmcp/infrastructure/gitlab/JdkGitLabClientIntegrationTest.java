package ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import ru.bogdanov.gitlabreviewmcp.application.GitLabClientException;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffVersion;
import ru.bogdanov.gitlabreviewmcp.configuration.GitLabProperties;
import ru.bogdanov.gitlabreviewmcp.domain.DiffSide;
import ru.bogdanov.gitlabreviewmcp.domain.InlineReviewComment;
import ru.bogdanov.gitlabreviewmcp.domain.MergeRequestRef;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

class JdkGitLabClientIntegrationTest {

    private WireMockServer server;
    private JdkGitLabClient client;
    private MergeRequestRef reference;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(0);
        server.start();
        URI baseUrl = URI.create("http://localhost:" + server.port());
        GitLabProperties properties = new GitLabProperties();
        properties.setBaseUrl(baseUrl);
        properties.setToken("test-token");
        properties.setReadTimeout(Duration.ofSeconds(2));
        properties.setMaxResponseSize(DataSize.ofMegabytes(1));
        properties.setMaxDiffSize(DataSize.ofMegabytes(1));
        properties.setMaxRetries(1);
        GitLabHttpTransport raw = new JdkGitLabHttpTransport(
                HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(),
                baseUrl,
                properties.getToken());
        client = new JdkGitLabClient(
                properties,
                new RetryingGitLabHttpTransport(raw, properties.getMaxRetries()),
                JsonMapper.builder()
                        .findAndAddModules()
                        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .build(),
                new GitLabDtoMapper());
        reference = new MergeRequestRef(
                URI.create(baseUrl + "/group/project/-/merge_requests/7"), "group/project", 7);
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void mapsConnectionMetadataDiffsAndDiscussionsWithUnknownFields() {
        server.stubFor(get(urlEqualTo("/api/v4/version"))
                .willReturn(okJson("{\"version\":\"17.2.1-ee.0\",\"future\":true}")));
        server.stubFor(get(urlEqualTo("/api/v4/user"))
                .willReturn(okJson(userJson()).withHeader("X-Request-Id", "user-request")));
        server.stubFor(get(urlEqualTo(mrPath()))
                .willReturn(okJson(mrJson()).withHeader("X-Request-Id", "mr-request")));
        server.stubFor(get(urlEqualTo(mrPath() + "/versions?per_page=100"))
                .willReturn(okJson("[{\"id\":3,\"base_commit_sha\":\"base\","
                        + "\"start_commit_sha\":\"start\",\"head_commit_sha\":\"head\",\"future\":1}]")));
        server.stubFor(get(urlEqualTo(mrPath() + "/diffs?per_page=100&page=1"))
                .willReturn(okJson("[{\"old_path\":\"src/A.java\",\"new_path\":\"src/A.java\","
                        + "\"diff\":\"@@ -1 +1 @@\\n-old\\n+new\\n\",\"new_file\":false,"
                        + "\"renamed_file\":false,\"deleted_file\":false,\"unknown\":true}]")));
        server.stubFor(get(urlEqualTo(mrPath() + "/discussions?per_page=100&page=1"))
                .willReturn(okJson("[{\"id\":\"discussion-1\",\"individual_note\":false,\"notes\":[{"
                        + "\"id\":11,\"body\":\"review\",\"author\":" + userJson() + ","
                        + "\"created_at\":\"2026-01-01T00:00:00Z\",\"system\":false,"
                        + "\"resolvable\":true,\"resolved\":false,\"position\":{"
                        + "\"old_path\":\"src/A.java\",\"new_path\":\"src/A.java\","
                        + "\"new_line\":1,\"base_sha\":\"base\",\"start_sha\":\"start\","
                        + "\"head_sha\":\"head\"}}]}]")));

        assertThat(client.checkConnection().gitLabRequestId()).isEqualTo("user-request");
        assertThat(client.getMergeRequest(reference).headSha()).isEqualTo("head");
        assertThat(client.getDiffVersions(reference)).containsExactly(new DiffVersion(3, "base", "start", "head"));
        assertThat(client.getDiffs(reference, Set.of("src/A.java"), null).items()).hasSize(1);
        assertThat(client.getDiscussions(reference, null).items().getFirst().notes().getFirst().position().newLine())
                .isEqualTo(1);

        server.verify(getRequestedFor(urlPathEqualTo("/api/v4/user"))
                .withHeader("PRIVATE-TOKEN", equalTo("test-token")));
        server.verify(0, getRequestedFor(urlPathEqualTo(mrPath() + "/changes")));
    }

    @Test
    void followsPaginationAndPostsInlineDiscussionWithCompletePosition() {
        server.stubFor(get(urlEqualTo(mrPath() + "/diffs?per_page=100&page=1"))
                .willReturn(okJson("[]").withHeader("X-Next-Page", "2")));
        server.stubFor(get(urlEqualTo(mrPath() + "/diffs?per_page=100&page=2"))
                .willReturn(okJson("[]")));
        server.stubFor(post(urlEqualTo(mrPath() + "/discussions"))
                .willReturn(okJson("{\"id\":\"discussion-2\",\"notes\":[{\"id\":22}]}")));

        var first = client.getDiffs(reference, Set.of(), null);
        assertThat(first.nextCursor()).isNotBlank();
        assertThat(client.getDiffs(reference, Set.of(), first.nextCursor()).nextCursor()).isNull();

        var receipt = client.createDiscussion(
                reference,
                new InlineReviewComment("Review body", "src/A.java", 2, DiffSide.NEW),
                new DiffVersion(3, "base", "start", "head"));

        assertThat(receipt.discussionId()).isEqualTo("discussion-2");
        server.verify(postRequestedFor(urlEqualTo(mrPath() + "/discussions"))
                .withHeader("PRIVATE-TOKEN", equalTo("test-token"))
                .withRequestBody(containing("body=Review%20body"))
                .withRequestBody(containing("position%5Bbase_sha%5D=base"))
                .withRequestBody(containing("position%5Bnew_line%5D=2")));
    }

    @Test
    void retriesRetryableGetButNeverPost() {
        server.stubFor(get(urlEqualTo("/api/v4/version"))
                .inScenario("retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("available"));
        server.stubFor(get(urlEqualTo("/api/v4/version"))
                .inScenario("retry")
                .whenScenarioStateIs("available")
                .willReturn(okJson("{\"version\":\"18.0.0\"}")));
        server.stubFor(get(urlEqualTo("/api/v4/user")).willReturn(okJson(userJson())));
        server.stubFor(post(urlEqualTo(mrPath() + "/discussions"))
                .willReturn(aResponse().withStatus(503)));

        assertThat(client.checkConnection().gitLabVersion()).isEqualTo("18.0.0");
        assertThatThrownBy(() -> client.createDiscussion(
                reference,
                new InlineReviewComment("body", "A", 1, DiffSide.OLD),
                new DiffVersion(1, "base", "start", "head")))
                .isInstanceOf(GitLabClientException.class);

        server.verify(2, getRequestedFor(urlEqualTo("/api/v4/version")));
        server.verify(1, postRequestedFor(urlEqualTo(mrPath() + "/discussions")));
    }

    @Test
    void mapsAuthenticationErrorWithoutReturningResponseBody() {
        server.stubFor(get(urlEqualTo(mrPath()))
                .willReturn(aResponse().withStatus(401).withBody("secret server details")));

        assertThatThrownBy(() -> client.getMergeRequest(reference))
                .isInstanceOf(GitLabClientException.class)
                .hasMessage("GitLab API returned HTTP 401")
                .hasMessageNotContaining("secret server details");
    }

    @Test
    void marksMalformedSuccessfulPostResponseAsAmbiguous() {
        server.stubFor(post(urlEqualTo(mrPath() + "/discussions"))
                .willReturn(aResponse().withStatus(201).withBody("not-json")));

        assertThatThrownBy(() -> client.createDiscussion(
                reference,
                new InlineReviewComment("body", "A", 1, DiffSide.NEW),
                new DiffVersion(1, "base", "start", "head")))
                .isInstanceOfSatisfying(GitLabClientException.class,
                        exception -> assertThat(exception.ambiguousWrite()).isTrue());

        server.verify(1, postRequestedFor(urlEqualTo(mrPath() + "/discussions")));
    }

    private String mrPath() {
        return "/api/v4/projects/group%2Fproject/merge_requests/7";
    }

    private String userJson() {
        return "{\"id\":1,\"username\":\"reviewer\",\"name\":\"Reviewer\","
                + "\"web_url\":\"http://localhost:" + server.port() + "/reviewer\"}";
    }

    private String mrJson() {
        return "{\"iid\":7,\"title\":\"Title\",\"description\":\"Description\","
                + "\"state\":\"opened\",\"source_branch\":\"feature\",\"target_branch\":\"main\","
                + "\"sha\":\"head\",\"author\":" + userJson() + ","
                + "\"web_url\":\"http://localhost:" + server.port()
                + "/group/project/-/merge_requests/7\",\"future_field\":42}";
    }
}
