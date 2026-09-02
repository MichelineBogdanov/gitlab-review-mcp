package ru.bogdanov.gitlabreviewmcp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffVersion;
import ru.bogdanov.gitlabreviewmcp.application.model.GitLabUser;
import ru.bogdanov.gitlabreviewmcp.application.model.MergeRequestDetails;
import ru.bogdanov.gitlabreviewmcp.application.port.ReviewProposalRepository;
import ru.bogdanov.gitlabreviewmcp.domain.GeneralReviewComment;
import ru.bogdanov.gitlabreviewmcp.domain.MergeRequestRef;
import ru.bogdanov.gitlabreviewmcp.domain.PublicationReceipt;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewCommentDraft;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewProposal;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewProposalStatus;
import ru.bogdanov.gitlabreviewmcp.infrastructure.proposal.InMemoryReviewProposalRepository;

class ReviewPublicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final MergeRequestRef REFERENCE = new MergeRequestRef(
            URI.create("https://gitlab.example.com/g/p/-/merge_requests/1"), "g/p", 1);

    @Test
    void retriesOnlyUnpublishedCommentsAfterDeterministicFailure() {
        RecordingClient client = new RecordingClient();
        client.failSecondOnce = true;
        ReviewProposal proposal = proposal();
        ReviewProposalRepository repository = repository(proposal);
        ReviewPublicationService service = new ReviewPublicationService(client, repository, clock());

        assertThat(service.publish("proposal", "digest").status()).isEqualTo(ReviewProposalStatus.PARTIAL);
        assertThat(service.publish("proposal", "digest").status()).isEqualTo(ReviewProposalStatus.PUBLISHED);

        assertThat(client.publishedBodies).containsExactly("one", "two", "two");
        assertThat(proposal.items().get(0).receipt().discussionId()).isEqualTo("discussion-1");
    }

    @Test
    void doesNotRetryAmbiguousPostOutcome() {
        RecordingClient client = new RecordingClient();
        client.unknownFirst = true;
        ReviewProposal proposal = proposal();
        ReviewPublicationService service = new ReviewPublicationService(client, repository(proposal), clock());

        assertThat(service.publish("proposal", "digest").status()).isEqualTo(ReviewProposalStatus.UNKNOWN);
        assertThatThrownBy(() -> service.publish("proposal", "digest"))
                .isInstanceOf(ReviewApplicationException.class)
                .hasMessageContaining("unknown outcome");
        assertThat(client.publishedBodies).containsExactly("one");
    }

    @Test
    void rejectsStaleHeadBeforeAnyPost() {
        RecordingClient client = new RecordingClient();
        client.head = "changed";
        ReviewPublicationService service = new ReviewPublicationService(client, repository(proposal()), clock());

        assertThatThrownBy(() -> service.publish("proposal", "digest"))
                .isInstanceOf(ReviewApplicationException.class)
                .hasMessageContaining("head changed");
        assertThat(client.publishedBodies).isEmpty();
    }

    private ReviewProposalRepository repository(ReviewProposal proposal) {
        var repository = new InMemoryReviewProposalRepository(10, clock());
        repository.save(proposal);
        return repository;
    }

    private ReviewProposal proposal() {
        return new ReviewProposal(
                "proposal",
                "digest",
                REFERENCE,
                "head",
                NOW,
                NOW.plusSeconds(900),
                "preview",
                List.of(new GeneralReviewComment("one"), new GeneralReviewComment("two")));
    }

    private Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    private static final class RecordingClient extends StubGitLabClient {

        private final List<String> publishedBodies = new ArrayList<>();
        private String head = "head";
        private boolean failSecondOnce;
        private boolean unknownFirst;

        @Override
        public MergeRequestDetails getMergeRequest(MergeRequestRef reference) {
            return new MergeRequestDetails(
                    reference,
                    "Title",
                    "Description",
                    "opened",
                    "feature",
                    "main",
                    head,
                    new GitLabUser(1, "author", "Author", URI.create("https://gitlab.example.com/author")),
                    reference.webUrl(),
                    "request");
        }

        @Override
        public List<DiffVersion> getDiffVersions(MergeRequestRef reference) {
            return List.of(new DiffVersion(1, "base", "start", "head"));
        }

        @Override
        public PublicationReceipt createDiscussion(
                MergeRequestRef reference,
                ReviewCommentDraft comment,
                DiffVersion version) {
            publishedBodies.add(comment.body());
            int attempt = publishedBodies.size();
            if (unknownFirst && attempt == 1) {
                throw new GitLabClientException("TIMEOUT", "unknown outcome", null, null, true);
            }
            if (failSecondOnce && attempt == 2) {
                throw new GitLabClientException("HTTP", "validation failed", 422, "request", false);
            }
            return new PublicationReceipt("discussion-" + attempt, attempt, reference.webUrl());
        }
    }
}
