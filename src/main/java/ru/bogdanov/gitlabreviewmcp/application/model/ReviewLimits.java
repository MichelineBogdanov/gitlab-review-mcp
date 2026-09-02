package ru.bogdanov.gitlabreviewmcp.application.model;

import java.time.Duration;

/**
 * Framework-independent review proposal limits.
 *
 * @param proposalTtl proposal lifetime
 * @param maxComments maximum comments per proposal
 * @param maxCommentLength maximum comment length
 */
public record ReviewLimits(Duration proposalTtl, int maxComments, int maxCommentLength) {
}
