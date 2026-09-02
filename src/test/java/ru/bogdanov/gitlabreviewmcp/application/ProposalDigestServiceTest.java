package ru.bogdanov.gitlabreviewmcp.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.bogdanov.gitlabreviewmcp.domain.DiffSide;
import ru.bogdanov.gitlabreviewmcp.domain.GeneralReviewComment;
import ru.bogdanov.gitlabreviewmcp.domain.InlineReviewComment;
import ru.bogdanov.gitlabreviewmcp.domain.MergeRequestRef;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewCommentDraft;
import tools.jackson.databind.json.JsonMapper;

class ProposalDigestServiceTest {

    private final ProposalDigestService service = new ProposalDigestService(JsonMapper.builder().build());
    private final MergeRequestRef reference = new MergeRequestRef(
            URI.create("https://gitlab.example.com/g/p/-/merge_requests/7"), "g/p", 7);

    @Test
    void digestIsStableForIdenticalCanonicalContent() {
        List<ReviewCommentDraft> comments = List.of(
                new GeneralReviewComment("general"),
                new InlineReviewComment("inline", "src/A.java", 12, DiffSide.NEW));

        assertThat(service.calculate(reference, "head", comments))
                .isEqualTo(service.calculate(reference, "head", List.copyOf(comments)))
                .matches("[0-9a-f]{64}");
    }

    @Test
    void digestChangesWithPositionOrHead() {
        String initial = service.calculate(
                reference, "head-1", List.of(new InlineReviewComment("body", "A", 1, DiffSide.NEW)));

        assertThat(service.calculate(
                reference, "head-2", List.of(new InlineReviewComment("body", "A", 1, DiffSide.NEW))))
                .isNotEqualTo(initial);
        assertThat(service.calculate(
                reference, "head-1", List.of(new InlineReviewComment("body", "A", 1, DiffSide.OLD))))
                .isNotEqualTo(initial);
    }
}
