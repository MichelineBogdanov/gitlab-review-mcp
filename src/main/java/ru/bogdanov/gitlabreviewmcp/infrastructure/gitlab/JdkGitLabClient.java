package ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.bogdanov.gitlabreviewmcp.application.GitLabClientException;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffFile;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffVersion;
import ru.bogdanov.gitlabreviewmcp.application.model.Discussion;
import ru.bogdanov.gitlabreviewmcp.application.model.GitLabConnectionInfo;
import ru.bogdanov.gitlabreviewmcp.application.model.MergeRequestDetails;
import ru.bogdanov.gitlabreviewmcp.application.model.PageResult;
import ru.bogdanov.gitlabreviewmcp.application.port.GitLabClient;
import ru.bogdanov.gitlabreviewmcp.configuration.GitLabProperties;
import ru.bogdanov.gitlabreviewmcp.domain.DiffSide;
import ru.bogdanov.gitlabreviewmcp.domain.InlineReviewComment;
import ru.bogdanov.gitlabreviewmcp.domain.MergeRequestRef;
import ru.bogdanov.gitlabreviewmcp.domain.PublicationReceipt;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewCommentDraft;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * GitLab REST API v4 adapter for merge request review operations.
 */
public final class JdkGitLabClient implements GitLabClient {

    private static final int PAGE_SIZE = 100;
    private static final TypeReference<List<DiffVersionDto>> VERSION_LIST = new TypeReference<>() { };
    private static final TypeReference<List<DiffFileDto>> DIFF_LIST = new TypeReference<>() { };
    private static final TypeReference<List<DiscussionDto>> DISCUSSION_LIST = new TypeReference<>() { };

    private final GitLabProperties properties;
    private final GitLabHttpTransport transport;
    private final ObjectMapper objectMapper;
    private final GitLabDtoMapper mapper;
    private final String apiRoot;

    /**
     * Creates the REST adapter.
     *
     * @param properties validated GitLab settings
     * @param transport authenticated HTTP transport
     * @param objectMapper tolerant JSON mapper
     * @param mapper wire DTO mapper
     */
    public JdkGitLabClient(
            GitLabProperties properties,
            GitLabHttpTransport transport,
            ObjectMapper objectMapper,
            GitLabDtoMapper mapper) {
        this.properties = properties;
        this.transport = transport;
        this.objectMapper = objectMapper;
        this.mapper = mapper;
        this.apiRoot = stripTrailingSlash(properties.getBaseUrl().toString()) + "/api/v4";
    }

    /** {@inheritDoc} */
    @Override
    public GitLabConnectionInfo checkConnection() {
        GitLabHttpResponse versionResponse = get(uri("/version"));
        VersionDto version = read(versionResponse, VersionDto.class);
        GitLabHttpResponse userResponse = get(uri("/user"));
        UserDto userDto = read(userResponse, UserDto.class);
        return new GitLabConnectionInfo(
                "OK",
                version.version(),
                userDto.username(),
                Set.of("READ_MERGE_REQUEST", "READ_DIFF", "READ_DISCUSSIONS", "PUBLISH_DISCUSSIONS"),
                requestId(userResponse));
    }

    /** {@inheritDoc} */
    @Override
    public MergeRequestDetails getMergeRequest(MergeRequestRef reference) {
        GitLabHttpResponse response = get(mrUri(reference, ""));
        return mapper.mergeRequest(reference, read(response, MergeRequestDto.class), requestId(response));
    }

    /** {@inheritDoc} */
    @Override
    public List<DiffVersion> getDiffVersions(MergeRequestRef reference) {
        GitLabHttpResponse response = get(withQuery(mrUri(reference, "/versions"), "per_page=100"));
        return read(response, VERSION_LIST).stream().map(mapper::version).toList();
    }

    /** {@inheritDoc} */
    @Override
    public PageResult<DiffFile> getDiffs(MergeRequestRef reference, Set<String> paths, String cursor) {
        int page = decodeCursor(cursor);
        GitLabHttpResponse response = get(withQuery(
                mrUri(reference, "/diffs"), "per_page=" + PAGE_SIZE + "&page=" + page));
        List<DiffFileDto> values = read(response, DIFF_LIST);
        long remaining = properties.getMaxDiffSize().toBytes();
        boolean truncated = false;
        List<DiffFile> result = new ArrayList<>();
        for (DiffFileDto value : values) {
            if (paths != null && !paths.isEmpty() && !paths.contains(value.oldPath()) && !paths.contains(value.newPath())) {
                continue;
            }
            String diff = value.diff() == null ? "" : value.diff();
            long size = diff.getBytes(StandardCharsets.UTF_8).length;
            boolean itemTruncated = size > remaining;
            truncated |= itemTruncated;
            remaining = Math.max(0, remaining - size);
            DiffFileDto bounded = itemTruncated
                    ? new DiffFileDto(
                            value.oldPath(), value.newPath(), "", value.newFile(), value.renamedFile(),
                            value.deletedFile(), value.generatedFile(), value.collapsed(), value.tooLarge())
                    : value;
            result.add(mapper.diff(bounded, itemTruncated));
        }
        String nextPage = response.headers().firstValue("X-Next-Page").filter(value -> !value.isBlank()).orElse(null);
        List<String> warnings = truncated
                ? List.of("Diff content exceeded the configured aggregate size limit")
                : List.of();
        return new PageResult<>(
                List.copyOf(result), nextPage == null ? null : encodeCursor(Integer.parseInt(nextPage)), truncated,
                warnings, requestId(response));
    }

    /** {@inheritDoc} */
    @Override
    public List<DiffFile> getAllDiffs(MergeRequestRef reference) {
        List<DiffFile> result = new ArrayList<>();
        long totalDiffBytes = 0;
        String cursor = null;
        do {
            PageResult<DiffFile> page = getDiffs(reference, Set.of(), cursor);
            if (page.truncated()) {
                throw new GitLabClientException(
                        "DIFF_TOO_LARGE",
                        "Merge request diff exceeds the configured size limit",
                        null,
                        page.gitLabRequestId(),
                        false);
            }
            result.addAll(page.items());
            totalDiffBytes += page.items().stream()
                    .mapToLong(diff -> diff.diff().getBytes(StandardCharsets.UTF_8).length)
                    .sum();
            if (totalDiffBytes > properties.getMaxDiffSize().toBytes()) {
                throw new GitLabClientException(
                        "DIFF_TOO_LARGE",
                        "Merge request diff exceeds the configured size limit",
                        null,
                        page.gitLabRequestId(),
                        false);
            }
            if (result.size() > properties.getMaxFiles()) {
                throw new GitLabClientException(
                        "TOO_MANY_DIFF_FILES",
                        "Merge request exceeds the configured changed-file limit",
                        null,
                        page.gitLabRequestId(),
                        false);
            }
            cursor = page.nextCursor();
        } while (cursor != null);
        return List.copyOf(result);
    }

    /** {@inheritDoc} */
    @Override
    public PageResult<Discussion> getDiscussions(MergeRequestRef reference, String cursor) {
        int page = decodeCursor(cursor);
        GitLabHttpResponse response = get(withQuery(
                mrUri(reference, "/discussions"), "per_page=" + PAGE_SIZE + "&page=" + page));
        List<Discussion> discussions = read(response, DISCUSSION_LIST).stream().map(mapper::discussion).toList();
        String nextPage = response.headers().firstValue("X-Next-Page").filter(value -> !value.isBlank()).orElse(null);
        return new PageResult<>(
                discussions,
                nextPage == null ? null : encodeCursor(Integer.parseInt(nextPage)),
                false,
                List.of(),
                requestId(response));
    }

    /** {@inheritDoc} */
    @Override
    public PublicationReceipt createDiscussion(
            MergeRequestRef reference,
            ReviewCommentDraft comment,
            DiffVersion version) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("body", comment.body());
        if (comment instanceof InlineReviewComment inline) {
            form.put("position[position_type]", "text");
            form.put("position[base_sha]", version.baseSha());
            form.put("position[start_sha]", version.startSha());
            form.put("position[head_sha]", version.headSha());
            form.put("position[old_path]", inline.oldPath());
            form.put("position[new_path]", inline.newPath());
            form.put(inline.side() == DiffSide.NEW ? "position[new_line]" : "position[old_line]",
                    Integer.toString(inline.line()));
        }
        GitLabHttpResponse response = transport.postForm(
                mrUri(reference, "/discussions"),
                formEncode(form),
                properties.getReadTimeout(),
                properties.getMaxResponseSize().toBytes());
        ensureSuccess(response);
        CreatedDiscussionDto created;
        try {
            created = read(response, CreatedDiscussionDto.class);
        } catch (GitLabClientException exception) {
            throw ambiguousWriteResponse(response, "GitLab accepted the write but returned malformed JSON", exception);
        }
        if (created.id() == null || created.id().isBlank() || created.notes() == null || created.notes().isEmpty()) {
            throw ambiguousWriteResponse(response, "GitLab accepted the write but omitted its receipt", null);
        }
        long noteId = created.notes().getFirst().id();
        return new PublicationReceipt(created.id(), noteId, reference.webUrl());
    }

    private GitLabClientException ambiguousWriteResponse(
            GitLabHttpResponse response,
            String message,
            RuntimeException cause) {
        GitLabClientException exception = new GitLabClientException(
                "INVALID_GITLAB_WRITE_RESPONSE", message, response.statusCode(), requestId(response), true);
        if (cause != null) {
            exception.initCause(cause);
        }
        return exception;
    }

    private GitLabHttpResponse get(URI uri) {
        GitLabHttpResponse response = transport.get(
                uri, properties.getReadTimeout(), properties.getMaxResponseSize().toBytes());
        ensureSuccess(response);
        return response;
    }

    private void ensureSuccess(GitLabHttpResponse response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new GitLabClientException(
                    errorCode(response.statusCode()),
                    "GitLab API returned HTTP " + response.statusCode(),
                    response.statusCode(),
                    requestId(response),
                    false);
        }
    }

    private <T> T read(GitLabHttpResponse response, Class<T> type) {
        try {
            return objectMapper.readValue(response.body(), type);
        } catch (JacksonException exception) {
            GitLabClientException clientException = new GitLabClientException(
                    "INVALID_GITLAB_RESPONSE", "GitLab returned malformed JSON", response.statusCode(),
                    requestId(response), false);
            clientException.initCause(exception);
            throw clientException;
        }
    }

    private <T> T read(GitLabHttpResponse response, TypeReference<T> type) {
        try {
            return objectMapper.readValue(response.body(), type);
        } catch (JacksonException exception) {
            GitLabClientException clientException = new GitLabClientException(
                    "INVALID_GITLAB_RESPONSE", "GitLab returned malformed JSON", response.statusCode(),
                    requestId(response), false);
            clientException.initCause(exception);
            throw clientException;
        }
    }

    private URI mrUri(MergeRequestRef reference, String suffix) {
        return uri("/projects/" + encode(reference.projectPath()) + "/merge_requests/" + reference.iid() + suffix);
    }

    private URI uri(String apiPath) {
        return URI.create(apiRoot + apiPath);
    }

    private URI withQuery(URI uri, String query) {
        return URI.create(uri + "?" + query);
    }

    private String formEncode(Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(java.util.stream.Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String requestId(GitLabHttpResponse response) {
        return response.headers().firstValue("X-Request-Id").orElse(null);
    }

    private String errorCode(int status) {
        return switch (status) {
            case 401 -> "GITLAB_UNAUTHORIZED";
            case 403 -> "GITLAB_FORBIDDEN";
            case 404 -> "GITLAB_NOT_FOUND";
            case 409 -> "GITLAB_CONFLICT";
            case 422 -> "GITLAB_VALIDATION_FAILED";
            case 429 -> "GITLAB_RATE_LIMITED";
            default -> status >= 500 ? "GITLAB_SERVER_ERROR" : "GITLAB_HTTP_ERROR";
        };
    }

    private int decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 1;
        }
        try {
            String value = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.US_ASCII);
            int page = Integer.parseInt(value);
            if (page < 1) {
                throw new NumberFormatException();
            }
            return page;
        } catch (IllegalArgumentException exception) {
            throw new GitLabClientException("INVALID_CURSOR", "Pagination cursor is invalid", null, null, false);
        }
    }

    private String encodeCursor(int page) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Integer.toString(page).getBytes(StandardCharsets.US_ASCII));
    }

    private static String stripTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
