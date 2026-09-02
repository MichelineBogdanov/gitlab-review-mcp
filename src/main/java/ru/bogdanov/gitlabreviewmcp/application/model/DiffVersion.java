package ru.bogdanov.gitlabreviewmcp.application.model;

/**
 * GitLab diff version coordinates required for inline discussions.
 *
 * @param id diff version identifier
 * @param baseSha base commit SHA
 * @param startSha start commit SHA
 * @param headSha head commit SHA
 */
public record DiffVersion(long id, String baseSha, String startSha, String headSha) {
}
