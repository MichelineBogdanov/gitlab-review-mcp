package ru.bogdanov.gitlabreviewmcp.infrastructure.mcp;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import ru.bogdanov.gitlabreviewmcp.application.GitLabClientException;
import ru.bogdanov.gitlabreviewmcp.application.GitLabConnectionService;
import ru.bogdanov.gitlabreviewmcp.application.MergeRequestQueryService;
import ru.bogdanov.gitlabreviewmcp.application.ReviewApplicationException;
import ru.bogdanov.gitlabreviewmcp.application.ReviewPreparationService;
import ru.bogdanov.gitlabreviewmcp.application.ReviewPublicationService;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffFile;
import ru.bogdanov.gitlabreviewmcp.application.model.Discussion;
import ru.bogdanov.gitlabreviewmcp.application.model.GitLabConnectionInfo;
import ru.bogdanov.gitlabreviewmcp.application.model.MergeRequestDetails;
import ru.bogdanov.gitlabreviewmcp.application.model.PageResult;
import ru.bogdanov.gitlabreviewmcp.application.model.PreparedReview;
import ru.bogdanov.gitlabreviewmcp.application.model.PublishedReview;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewCommentDraft;

/**
 * Thin MCP adapter exposing the review application use cases.
 */
@Component
public final class GitLabReviewTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitLabReviewTools.class);

    private final GitLabConnectionService connectionService;
    private final MergeRequestQueryService queryService;
    private final ReviewPreparationService preparationService;
    private final ReviewPublicationService publicationService;

    /**
     * Creates the tool adapter.
     *
     * @param connectionService connection diagnostics
     * @param queryService merge request queries
     * @param preparationService review preparation
     * @param publicationService approved publication
     */
    public GitLabReviewTools(
            GitLabConnectionService connectionService,
            MergeRequestQueryService queryService,
            ReviewPreparationService preparationService,
            ReviewPublicationService publicationService) {
        this.connectionService = connectionService;
        this.queryService = queryService;
        this.preparationService = preparationService;
        this.publicationService = publicationService;
    }

    /** @return structured GitLab connection diagnostics */
    @McpTool(
            name = "gitlab_check_connection",
            description = "Checks the configured GitLab version and authenticated user without changing GitLab.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = true))
    public ToolResponse<GitLabConnectionInfo> checkConnection() {
        return execute(connectionService::checkConnection);
    }

    /**
     * Reads current merge request metadata.
     *
     * @param mergeRequestUrl full URL on the configured GitLab origin
     * @return metadata response
     */
    @McpTool(
            name = "gitlab_get_merge_request",
            description = "Reads merge request metadata and the current head SHA from a full GitLab MR URL.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = true))
    public ToolResponse<MergeRequestDetails> getMergeRequest(
            @McpToolParam(required = true, description = "Full merge request URL on the configured GitLab origin")
            String mergeRequestUrl) {
        return execute(() -> queryService.getMergeRequest(mergeRequestUrl));
    }

    /**
     * Reads a bounded merge request diff page.
     *
     * @param mergeRequestUrl full URL on the configured GitLab origin
     * @param paths optional repository-relative path filter
     * @param cursor optional cursor returned by the preceding call
     * @return diff response
     */
    @McpTool(
            name = "gitlab_get_merge_request_diff",
            description = "Reads one bounded page of current merge request file diffs. Follow nextCursor until absent.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = true))
    public ToolResponse<PageResult<DiffFile>> getMergeRequestDiff(
            @McpToolParam(required = true, description = "Full merge request URL on the configured GitLab origin")
            String mergeRequestUrl,
            @McpToolParam(required = false, description = "Optional repository-relative paths") Set<String> paths,
            @McpToolParam(required = false, description = "Opaque cursor from the previous diff response") String cursor) {
        return execute(() -> queryService.getDiffs(mergeRequestUrl, paths, cursor));
    }

    /**
     * Reads a bounded discussion page including replies and positions.
     *
     * @param mergeRequestUrl full URL on the configured GitLab origin
     * @param cursor optional cursor returned by the preceding call
     * @return discussion response
     */
    @McpTool(
            name = "gitlab_get_merge_request_discussions",
            description = "Reads one bounded page of merge request discussions, replies, positions and resolved state.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = true))
    public ToolResponse<PageResult<Discussion>> getMergeRequestDiscussions(
            @McpToolParam(required = true, description = "Full merge request URL on the configured GitLab origin")
            String mergeRequestUrl,
            @McpToolParam(required = false, description = "Opaque cursor from the previous discussion response")
            String cursor) {
        return execute(() -> queryService.getDiscussions(mergeRequestUrl, cursor));
    }

    /**
     * Validates proposed comments and retains an immutable preview without writing to GitLab.
     *
     * @param mergeRequestUrl full URL on the configured GitLab origin
     * @param comments proposed review comments
     * @return proposal identifier, digest, expiration and complete preview
     */
    @McpTool(
            name = "gitlab_prepare_review",
            description = "Validates comments against the current diff and creates an in-memory immutable preview. "
                    + "This tool never posts to GitLab. Show the complete returned preview to the user before publishing.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false, destructiveHint = false, idempotentHint = false, openWorldHint = true))
    public ToolResponse<PreparedReview> prepareReview(
            @McpToolParam(required = true, description = "Full merge request URL on the configured GitLab origin")
            String mergeRequestUrl,
            @McpToolParam(required = true, description = "GENERAL or INLINE comments to include in the preview")
            List<ReviewCommentInput> comments) {
        return execute(() -> {
            if (comments == null) {
                throw new IllegalArgumentException("Comments are required");
            }
            List<ReviewCommentDraft> drafts = comments.stream().map(ReviewCommentInput::toDraft).toList();
            return preparationService.prepare(mergeRequestUrl, drafts);
        });
    }

    /**
     * Publishes the exact previously prepared proposal.
     *
     * @param proposalId proposal identifier returned by prepare
     * @param expectedDigest digest shown with the preview
     * @return publication status and GitLab receipts
     */
    @McpTool(
            name = "gitlab_publish_review",
            description = "Publishes only the exact in-memory proposal identified by proposalId and expectedDigest. "
                    + "Invoke only after the user has seen the complete preview and explicitly approved publication.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false, destructiveHint = false, idempotentHint = false, openWorldHint = true))
    public ToolResponse<PublishedReview> publishReview(
            @McpToolParam(required = true, description = "Proposal identifier returned by gitlab_prepare_review")
            String proposalId,
            @McpToolParam(required = true, description = "Digest displayed in the approved preview")
            String expectedDigest) {
        return execute(() -> publicationService.publish(proposalId, expectedDigest));
    }

    private <T> ToolResponse<T> execute(Supplier<T> operation) {
        try {
            return ToolResponse.success(operation.get());
        } catch (GitLabClientException exception) {
            return ToolResponse.failure(new ToolError(
                    exception.code(), exception.getMessage(), exception.httpStatus(), exception.gitLabRequestId()));
        } catch (ReviewApplicationException exception) {
            return ToolResponse.failure(new ToolError(exception.code(), exception.getMessage(), null, null));
        } catch (IllegalArgumentException | NullPointerException exception) {
            return ToolResponse.failure(new ToolError("INVALID_INPUT", exception.getMessage(), null, null));
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected MCP tool failure: {}", exception.getClass().getSimpleName());
            return ToolResponse.failure(new ToolError("INTERNAL_ERROR", "Unexpected internal error", null, null));
        }
    }
}
