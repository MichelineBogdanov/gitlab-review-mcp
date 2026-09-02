package ru.bogdanov.gitlabreviewmcp.application.port;

import java.time.Instant;
import java.util.Optional;
import ru.bogdanov.gitlabreviewmcp.domain.ReviewProposal;

/**
 * Ephemeral repository for immutable review proposals.
 */
public interface ReviewProposalRepository {

    /** @param proposal proposal to retain */
    void save(ReviewProposal proposal);

    /** @param proposalId proposal identifier @return proposal when present */
    Optional<ReviewProposal> findById(String proposalId);

    /** @param now current time @return number of removed proposals */
    int deleteExpired(Instant now);

    /** @return current number of retained proposals */
    int size();
}
