package ru.bogdanov.gitlabreviewmcp.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReviewProposalTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void validatesDigestAndHeadBeforeTransition() {
        ReviewProposal proposal = proposal();

        assertThatThrownBy(() -> proposal.beginPublication("wrong", "head", CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> proposal.beginPublication("digest", "new-head", CREATED_AT))
                .isInstanceOf(IllegalStateException.class);
        assertThat(proposal.status()).isEqualTo(ReviewProposalStatus.PREPARED);
    }

    @Test
    void calculatesPublishedAndUnknownStates() {
        ReviewProposal published = proposal();
        published.beginPublication("digest", "head", CREATED_AT);
        published.markPublished(0, new PublicationReceipt("d1", 1, published.mergeRequest().webUrl()));
        published.markPublished(1, new PublicationReceipt("d2", 2, published.mergeRequest().webUrl()));
        published.finishPublication();

        assertThat(published.status()).isEqualTo(ReviewProposalStatus.PUBLISHED);
        assertThat(published.beginPublication("digest", "head", CREATED_AT)).isFalse();
        assertThatThrownBy(() -> published.beginPublication("wrong", "head", CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class);

        ReviewProposal unknown = proposal();
        unknown.beginPublication("digest", "head", CREATED_AT);
        unknown.markUnknown(0, "timeout");
        unknown.finishPublication();

        assertThat(unknown.status()).isEqualTo(ReviewProposalStatus.UNKNOWN);
        assertThatThrownBy(() -> unknown.beginPublication("digest", "head", CREATED_AT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void expiresPreparedProposalAtTtlBoundary() {
        ReviewProposal proposal = proposal();

        assertThat(proposal.expireIfRequired(CREATED_AT.plusSeconds(900)))
                .isEqualTo(ReviewProposalStatus.EXPIRED);
    }

    private ReviewProposal proposal() {
        MergeRequestRef reference = new MergeRequestRef(
                URI.create("https://gitlab.example.com/g/p/-/merge_requests/1"), "g/p", 1);
        return new ReviewProposal(
                "proposal",
                "digest",
                reference,
                "head",
                CREATED_AT,
                CREATED_AT.plusSeconds(900),
                "preview",
                List.of(new GeneralReviewComment("one"), new GeneralReviewComment("two")));
    }
}
