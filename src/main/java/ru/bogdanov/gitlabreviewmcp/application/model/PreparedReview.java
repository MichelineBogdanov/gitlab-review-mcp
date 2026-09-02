package ru.bogdanov.gitlabreviewmcp.application.model;

import java.time.Instant;
import java.util.List;

/**
 * Immutable result returned after a review has been validated and retained.
 *
 * @param proposalId random proposal identifier
 * @param digest canonical content digest
 * @param headSha merge request head used for validation
 * @param expiresAt proposal expiration time
 * @param warnings non-fatal preparation warnings
 * @param preview complete human-readable preview
 */
public record PreparedReview(
        String proposalId,
        String digest,
        String headSha,
        Instant expiresAt,
        List<String> warnings,
        String preview) {
}
