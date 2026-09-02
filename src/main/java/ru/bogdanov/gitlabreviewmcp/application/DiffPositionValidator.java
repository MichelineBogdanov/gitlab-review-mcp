package ru.bogdanov.gitlabreviewmcp.application;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffFile;
import ru.bogdanov.gitlabreviewmcp.domain.DiffSide;
import ru.bogdanov.gitlabreviewmcp.domain.InlineReviewComment;

/**
 * Validates single-line positions against unified diff hunks.
 */
public final class DiffPositionValidator {

    private static final Pattern HUNK = Pattern.compile("^@@ -(\\d+)(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@.*$");

    /**
     * Ensures an inline comment targets a line represented by the current diff.
     *
     * @param comment inline comment
     * @param diffs current merge request diffs
     */
    public void validate(InlineReviewComment comment, List<DiffFile> diffs) {
        normalize(comment, diffs);
    }

    /**
     * Validates a position and resolves both paths required by GitLab for renamed files.
     *
     * @param comment inline comment
     * @param diffs current merge request diffs
     * @return normalized comment with old and new paths
     */
    public InlineReviewComment normalize(InlineReviewComment comment, List<DiffFile> diffs) {
        DiffFile file = diffs.stream()
                .filter(candidate -> matchesPath(comment, candidate))
                .findFirst()
                .orElseThrow(() -> new ReviewApplicationException(
                        "INVALID_DIFF_POSITION", "File is not present in the current merge request diff: " + comment.path()));
        Set<Line> lines = parse(file.diff());
        if (!lines.contains(new Line(comment.side(), comment.line()))) {
            throw new ReviewApplicationException(
                    "INVALID_DIFF_POSITION",
                    "Line is not present on the requested side of the current diff: "
                            + comment.path() + ':' + comment.line());
        }
        return new InlineReviewComment(
                comment.body(), comment.path(), comment.line(), comment.side(), file.oldPath(), file.newPath());
    }

    private boolean matchesPath(InlineReviewComment comment, DiffFile file) {
        return comment.side() == DiffSide.NEW
                ? comment.path().equals(file.newPath())
                : comment.path().equals(file.oldPath());
    }

    private Set<Line> parse(String diff) {
        Set<Line> lines = new HashSet<>();
        int oldLine = 0;
        int newLine = 0;
        boolean inHunk = false;
        for (String value : diff.split("\\n", -1)) {
            Matcher matcher = HUNK.matcher(value);
            if (matcher.matches()) {
                oldLine = Integer.parseInt(matcher.group(1));
                newLine = Integer.parseInt(matcher.group(2));
                inHunk = true;
                continue;
            }
            if (!inHunk || value.startsWith("\\ No newline")) {
                continue;
            }
            if (value.startsWith("+") && !value.startsWith("+++")) {
                lines.add(new Line(DiffSide.NEW, newLine++));
            } else if (value.startsWith("-") && !value.startsWith("---")) {
                lines.add(new Line(DiffSide.OLD, oldLine++));
            } else {
                lines.add(new Line(DiffSide.OLD, oldLine++));
                lines.add(new Line(DiffSide.NEW, newLine++));
            }
        }
        return lines;
    }

    private record Line(DiffSide side, int number) {
    }
}
