package ru.bogdanov.gitlabreviewmcp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffFile;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffVersion;
import ru.bogdanov.gitlabreviewmcp.application.model.GitLabUser;
import ru.bogdanov.gitlabreviewmcp.application.model.MergeRequestDetails;
import ru.bogdanov.gitlabreviewmcp.application.model.ReviewLimits;
import ru.bogdanov.gitlabreviewmcp.domain.DiffSide;
import ru.bogdanov.gitlabreviewmcp.domain.GeneralReviewComment;
import ru.bogdanov.gitlabreviewmcp.domain.InlineReviewComment;
import ru.bogdanov.gitlabreviewmcp.domain.MergeRequestRef;
import ru.bogdanov.gitlabreviewmcp.domain.PublicationReceipt;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewCommentDraft;
import ru.bogdanov.gitlabreviewmcp.infrastructure.proposal.InMemoryReviewProposalRepository;
import tools.jackson.databind.json.JsonMapper;

class ReviewPreparationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final MergeRequestRef REFERENCE = new MergeRequestRef(
            URI.create("https://gitlab.example.com/g/p/-/merge_requests/1"), "g/p", 1);

    @Test
    void preparesCompletePreviewWithoutPosting() {
        PreparationClient client = new PreparationClient();
        var repository = new InMemoryReviewProposalRepository(100, clock());
        ReviewPreparationService service = service(client, repository);

        var result = service.prepare(
                REFERENCE.webUrl().toString(),
                List.of(
                        new GeneralReviewComment("  general\r\nbody  "),
                        new InlineReviewComment("inline", "src\\A.java", 2, DiffSide.NEW)));

        assertThat(result.digest()).matches("[0-9a-f]{64}");
        assertThat(result.expiresAt()).isEqualTo(NOW.plusSeconds(900));
        assertThat(result.preview())
                .contains("GENERAL", "general\nbody", "INLINE src/A.java:2 [NEW]", "Head SHA: head");
        assertThat(repository.findById(result.proposalId())).isPresent();
        assertThat(client.postCount).isZero();
    }

    @Test
    void rejectsPositionMissingFromCurrentDiff() {
        PreparationClient client = new PreparationClient();
        ReviewPreparationService service = service(
                client, new InMemoryReviewProposalRepository(100, clock()));

        assertThatThrownBy(() -> service.prepare(
                REFERENCE.webUrl().toString(),
                List.of(new InlineReviewComment("body", "src/A.java", 99, DiffSide.NEW))))
                .isInstanceOf(ReviewApplicationException.class)
                .hasMessageContaining("not present");
        assertThat(client.postCount).isZero();
    }

    private ReviewPreparationService service(
            PreparationClient client,
            InMemoryReviewProposalRepository repository) {
        return new ReviewPreparationService(
                ignored -> REFERENCE,
                client,
                repository,
                new ProposalDigestService(JsonMapper.builder().build()),
                new DiffPositionValidator(),
                new ReviewLimits(Duration.ofMinutes(15), 50, 10_000),
                clock(),
                new SecureRandom());
    }

    private Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    private static final class PreparationClient extends StubGitLabClient {

        private int postCount;

        @Override
        public MergeRequestDetails getMergeRequest(MergeRequestRef reference) {
            return new MergeRequestDetails(
                    reference,
                    "Title",
                    "Description",
                    "opened",
                    "feature",
                    "main",
                    "head",
                    new GitLabUser(1, "author", "Author", URI.create("https://gitlab.example.com/author")),
                    reference.webUrl(),
                    "request");
        }

        @Override
        public List<DiffVersion> getDiffVersions(MergeRequestRef reference) {
            return List.of(new DiffVersion(5, "base", "start", "head"));
        }

        @Override
        public List<DiffFile> getAllDiffs(MergeRequestRef reference) {
            return List.of(new DiffFile(
                    "src/A.java", "src/A.java", "@@ -1,2 +1,2 @@\n one\n-two\n+two changed\n",
                    false, false, false, false, false, false, false));
        }

        @Override
        public PublicationReceipt createDiscussion(
                MergeRequestRef reference,
                ReviewCommentDraft comment,
                DiffVersion version) {
            postCount++;
            throw new AssertionError("prepare must not post");
        }
    }
}
