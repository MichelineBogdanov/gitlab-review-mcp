package ru.bogdanov.gitlabreviewmcp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffFile;
import ru.bogdanov.gitlabreviewmcp.domain.DiffSide;
import ru.bogdanov.gitlabreviewmcp.domain.InlineReviewComment;

class DiffPositionValidatorTest {

    private final DiffPositionValidator validator = new DiffPositionValidator();
    private final DiffFile diff = new DiffFile(
            "src/Old.java",
            "src/New.java",
            "@@ -10,3 +10,4 @@\n context\n-old\n+new\n+added\n context2\n",
            false,
            true,
            false,
            false,
            false,
            false,
            false);

    @Test
    void mapsOldAndNewLineCoordinates() {
        assertThatCode(() -> validator.validate(
                new InlineReviewComment("old", "src/Old.java", 11, DiffSide.OLD), List.of(diff)))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(
                new InlineReviewComment("new", "src/New.java", 12, DiffSide.NEW), List.of(diff)))
                .doesNotThrowAnyException();
        assertThat(validator.normalize(
                        new InlineReviewComment("new", "src/New.java", 12, DiffSide.NEW), List.of(diff)))
                .extracting(InlineReviewComment::oldPath, InlineReviewComment::newPath)
                .containsExactly("src/Old.java", "src/New.java");
    }

    @Test
    void rejectsLineOutsideCurrentDiff() {
        assertThatThrownBy(() -> validator.validate(
                new InlineReviewComment("invalid", "src/New.java", 99, DiffSide.NEW), List.of(diff)))
                .isInstanceOf(ReviewApplicationException.class)
                .hasMessageContaining("not present");
    }
}
