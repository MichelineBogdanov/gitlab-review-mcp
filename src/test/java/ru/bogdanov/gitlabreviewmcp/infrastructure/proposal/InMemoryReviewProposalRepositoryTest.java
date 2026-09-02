package ru.bogdanov.gitlabreviewmcp.infrastructure.proposal;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.bogdanov.gitlabreviewmcp.domain.GeneralReviewComment;
import ru.bogdanov.gitlabreviewmcp.domain.MergeRequestRef;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewProposal;

class InMemoryReviewProposalRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void removesExpiredProposalsAndEvictsOldestAtCapacity() {
        var repository = new InMemoryReviewProposalRepository(2, Clock.fixed(NOW, ZoneOffset.UTC));
        repository.save(proposal("old", NOW.minusSeconds(2), NOW.plusSeconds(30)));
        repository.save(proposal("new", NOW.minusSeconds(1), NOW.plusSeconds(30)));
        repository.save(proposal("newest", NOW, NOW.plusSeconds(30)));

        assertThat(repository.findById("old")).isEmpty();
        assertThat(repository.findById("new")).isPresent();
        assertThat(repository.findById("newest")).isPresent();

        repository.save(proposal("expired", NOW.minusSeconds(30), NOW));
        assertThat(repository.findById("expired")).isEmpty();
    }

    private ReviewProposal proposal(String id, Instant createdAt, Instant expiresAt) {
        MergeRequestRef reference = new MergeRequestRef(
                URI.create("https://gitlab.example.com/g/p/-/merge_requests/1"), "g/p", 1);
        return new ReviewProposal(
                id,
                "digest",
                reference,
                "head",
                createdAt,
                expiresAt,
                "preview",
                List.of(new GeneralReviewComment("body")));
    }
}
