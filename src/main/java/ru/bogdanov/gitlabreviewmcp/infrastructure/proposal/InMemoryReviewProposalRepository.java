package ru.bogdanov.gitlabreviewmcp.infrastructure.proposal;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.scheduling.annotation.Scheduled;
import ru.bogdanov.gitlabreviewmcp.application.port.ReviewProposalRepository;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewProposal;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewProposalStatus;

/**
 * Thread-safe bounded repository that retains proposals only for the process lifetime.
 */
public final class InMemoryReviewProposalRepository implements ReviewProposalRepository {

    private final ConcurrentMap<String, ReviewProposal> proposals = new ConcurrentHashMap<>();
    private final int maxCount;
    private final Clock clock;

    /**
     * Creates the repository.
     *
     * @param maxCount maximum retained entries
     * @param clock application clock
     */
    public InMemoryReviewProposalRepository(int maxCount, Clock clock) {
        this.maxCount = maxCount;
        this.clock = clock;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void save(ReviewProposal proposal) {
        deleteExpired(clock.instant());
        while (proposals.size() >= maxCount) {
            proposals.values().stream()
                    .min(Comparator.comparing(ReviewProposal::createdAt))
                    .ifPresent(oldest -> proposals.remove(oldest.proposalId(), oldest));
        }
        proposals.put(proposal.proposalId(), proposal);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<ReviewProposal> findById(String proposalId) {
        ReviewProposal proposal = proposals.get(proposalId);
        if (proposal != null && proposal.expireIfRequired(clock.instant()) == ReviewProposalStatus.EXPIRED) {
            proposals.remove(proposalId, proposal);
            return Optional.empty();
        }
        return Optional.ofNullable(proposal);
    }

    /** {@inheritDoc} */
    @Override
    public int deleteExpired(Instant now) {
        int before = proposals.size();
        proposals.entrySet().removeIf(
                entry -> entry.getValue().expireIfRequired(now) == ReviewProposalStatus.EXPIRED);
        return before - proposals.size();
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return proposals.size();
    }

    /**
     * Periodically removes proposals whose TTL elapsed.
     */
    @Scheduled(fixedDelayString = "${review.cleanup-interval:PT1M}")
    public void purgeExpired() {
        deleteExpired(clock.instant());
    }
}
