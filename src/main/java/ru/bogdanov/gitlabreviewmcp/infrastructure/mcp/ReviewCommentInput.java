package ru.bogdanov.gitlabreviewmcp.infrastructure.mcp;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.Locale;
import ru.bogdanov.gitlabreviewmcp.domain.DiffSide;
import ru.bogdanov.gitlabreviewmcp.domain.GeneralReviewComment;
import ru.bogdanov.gitlabreviewmcp.domain.InlineReviewComment;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewCommentDraft;

/**
 * MCP input for a general or inline proposed review comment.
 */
public final class ReviewCommentInput {

    @JsonPropertyDescription("Comment type: GENERAL or INLINE")
    private String type;

    @JsonPropertyDescription("Markdown comment body")
    private String body;

    @JsonPropertyDescription("Repository-relative path; required only for INLINE")
    private String path;

    @JsonPropertyDescription("Line number; required only for INLINE")
    private Integer line;

    @JsonPropertyDescription("NEW or OLD diff side; required only for INLINE")
    private DiffSide side;

    /** @return comment type */
    public String getType() {
        return type;
    }

    /** @param type comment type */
    public void setType(String type) {
        this.type = type;
    }

    /** @return Markdown body */
    public String getBody() {
        return body;
    }

    /** @param body Markdown body */
    public void setBody(String body) {
        this.body = body;
    }

    /** @return repository path for an inline comment */
    public String getPath() {
        return path;
    }

    /** @param path repository path for an inline comment */
    public void setPath(String path) {
        this.path = path;
    }

    /** @return line for an inline comment */
    public Integer getLine() {
        return line;
    }

    /** @param line line for an inline comment */
    public void setLine(Integer line) {
        this.line = line;
    }

    /** @return diff side for an inline comment */
    public DiffSide getSide() {
        return side;
    }

    /** @param side diff side for an inline comment */
    public void setSide(DiffSide side) {
        this.side = side;
    }

    /** @return validated domain draft */
    public ReviewCommentDraft toDraft() {
        if (type == null) {
            throw new IllegalArgumentException("Comment type is required");
        }
        return switch (type.strip().toUpperCase(Locale.ROOT)) {
            case "GENERAL" -> new GeneralReviewComment(body);
            case "INLINE" -> {
                if (line == null) {
                    throw new IllegalArgumentException("Inline comment line is required");
                }
                yield new InlineReviewComment(body, path, line, side);
            }
            default -> throw new IllegalArgumentException("Comment type must be GENERAL or INLINE");
        };
    }
}
