package ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab;

import java.net.URI;
import java.util.List;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffFile;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffVersion;
import ru.bogdanov.gitlabreviewmcp.application.model.Discussion;
import ru.bogdanov.gitlabreviewmcp.application.model.DiscussionNote;
import ru.bogdanov.gitlabreviewmcp.application.model.DiscussionPosition;
import ru.bogdanov.gitlabreviewmcp.application.model.GitLabUser;
import ru.bogdanov.gitlabreviewmcp.application.model.MergeRequestDetails;
import ru.bogdanov.gitlabreviewmcp.domain.MergeRequestRef;

/**
 * Maps tolerant GitLab wire DTOs to application models.
 */
public final class GitLabDtoMapper {

    /** @param value wire DTO @return application user */
    public GitLabUser user(UserDto value) {
        return value == null ? null : new GitLabUser(value.id(), value.username(), value.name(), uri(value.webUrl()));
    }

    /** @param reference trusted reference @param value wire DTO @param requestId request ID @return metadata */
    public MergeRequestDetails mergeRequest(MergeRequestRef reference, MergeRequestDto value, String requestId) {
        return new MergeRequestDetails(
                reference,
                value.title(),
                value.description(),
                value.state(),
                value.sourceBranch(),
                value.targetBranch(),
                value.sha(),
                user(value.author()),
                uri(value.webUrl()),
                requestId);
    }

    /** @param value wire DTO @return diff version */
    public DiffVersion version(DiffVersionDto value) {
        return new DiffVersion(value.id(), value.baseSha(), value.startSha(), value.headSha());
    }

    /** @param value wire DTO @param truncated local truncation flag @return changed file */
    public DiffFile diff(DiffFileDto value, boolean truncated) {
        return new DiffFile(
                value.oldPath(), value.newPath(), value.diff() == null ? "" : value.diff(), truth(value.newFile()),
                truth(value.renamedFile()), truth(value.deletedFile()), truth(value.generatedFile()),
                truth(value.collapsed()), truth(value.tooLarge()),
                truncated);
    }

    /** @param value wire DTO @return discussion */
    public Discussion discussion(DiscussionDto value) {
        List<DiscussionNote> notes = value.notes() == null ? List.of() : value.notes().stream().map(this::note).toList();
        return new Discussion(value.id(), truth(value.individualNote()), notes);
    }

    private DiscussionNote note(NoteDto value) {
        return new DiscussionNote(
                value.id(), value.body(), user(value.author()), value.createdAt(), truth(value.system()),
                truth(value.resolvable()), truth(value.resolved()), position(value.position()));
    }

    private DiscussionPosition position(PositionDto value) {
        return value == null ? null : new DiscussionPosition(
                value.oldPath(), value.newPath(), value.oldLine(), value.newLine(), value.baseSha(), value.startSha(),
                value.headSha());
    }

    private URI uri(String value) {
        return value == null || value.isBlank() ? null : URI.create(value);
    }

    private boolean truth(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
