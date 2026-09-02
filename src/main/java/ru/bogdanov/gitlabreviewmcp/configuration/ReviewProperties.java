package ru.bogdanov.gitlabreviewmcp.configuration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Limits and expiration settings for review proposals.
 *
 * @param proposalTtl lifetime of an unpublished proposal
 * @param proposalMaxCount maximum number of proposals retained in memory
 * @param maxComments maximum number of comments in a proposal
 * @param maxCommentLength maximum comment length
 */
@Validated
@ConfigurationProperties(prefix = "review")
public record ReviewProperties(
        @NotNull Duration proposalTtl,
        @Min(1) @Max(1000) int proposalMaxCount,
        @Min(1) @Max(100) int maxComments,
        @Min(1) @Max(100_000) int maxCommentLength) {

    /**
     * Applies safe defaults when individual settings are omitted.
     */
    public ReviewProperties {
        proposalTtl = proposalTtl == null ? Duration.ofMinutes(15) : proposalTtl;
        proposalMaxCount = proposalMaxCount == 0 ? 100 : proposalMaxCount;
        maxComments = maxComments == 0 ? 50 : maxComments;
        maxCommentLength = maxCommentLength == 0 ? 10_000 : maxCommentLength;
    }
}
