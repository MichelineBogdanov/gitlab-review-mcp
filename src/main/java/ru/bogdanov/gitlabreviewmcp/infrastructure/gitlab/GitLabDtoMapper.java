package ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab;

import java.net.URI;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffFile;
import ru.bogdanov.gitlabreviewmcp.application.model.DiffVersion;
import ru.bogdanov.gitlabreviewmcp.application.model.Discussion;
import ru.bogdanov.gitlabreviewmcp.application.model.DiscussionNote;
import ru.bogdanov.gitlabreviewmcp.application.model.DiscussionPosition;
import ru.bogdanov.gitlabreviewmcp.application.model.GitLabUser;
import ru.bogdanov.gitlabreviewmcp.application.model.MergeRequestDetails;
import ru.bogdanov.gitlabreviewmcp.domain.MergeRequestRef;

/**
 * Maps tolerant GitLab wire DTOs to application models using generated code.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GitLabDtoMapper {

    /**
     * Maps a GitLab user.
     *
     * @param value wire DTO
     * @return application user
     */
    GitLabUser user(UserDto value);

    /**
     * Maps merge request metadata while retaining its trusted parsed reference.
     *
     * @param reference trusted reference
     * @param value wire DTO
     * @param requestId GitLab request identifier
     * @return merge request metadata
     */
    @Mapping(target = "reference", source = "reference")
    @Mapping(target = "title", source = "value.title")
    @Mapping(target = "description", source = "value.description")
    @Mapping(target = "state", source = "value.state")
    @Mapping(target = "sourceBranch", source = "value.sourceBranch")
    @Mapping(target = "targetBranch", source = "value.targetBranch")
    @Mapping(target = "headSha", source = "value.sha")
    @Mapping(target = "author", source = "value.author")
    @Mapping(target = "webUrl", source = "value.webUrl")
    @Mapping(target = "gitLabRequestId", source = "requestId")
    MergeRequestDetails mergeRequest(MergeRequestRef reference, MergeRequestDto value, String requestId);

    /**
     * Maps a diff version.
     *
     * @param value wire DTO
     * @return diff version
     */
    DiffVersion version(DiffVersionDto value);

    /**
     * Maps a changed file and attaches the local truncation state.
     *
     * @param value wire DTO
     * @param truncated local truncation flag
     * @return changed file
     */
    @Mapping(target = "diff", source = "value.diff", defaultValue = "")
    @Mapping(target = "newFile", source = "value.newFile", qualifiedByName = "truth")
    @Mapping(target = "renamedFile", source = "value.renamedFile", qualifiedByName = "truth")
    @Mapping(target = "deletedFile", source = "value.deletedFile", qualifiedByName = "truth")
    @Mapping(target = "generatedFile", source = "value.generatedFile", qualifiedByName = "truth")
    @Mapping(target = "collapsed", source = "value.collapsed", qualifiedByName = "truth")
    @Mapping(target = "tooLarge", source = "value.tooLarge", qualifiedByName = "truth")
    @Mapping(target = "truncated", source = "truncated")
    DiffFile diff(DiffFileDto value, boolean truncated);

    /**
     * Maps a discussion and all of its replies.
     *
     * @param value wire DTO
     * @return discussion
     */
    @Mapping(target = "individualNote", source = "individualNote", qualifiedByName = "truth")
    @Mapping(target = "notes", source = "notes", qualifiedByName = "notes")
    Discussion discussion(DiscussionDto value);

    /**
     * Maps one note or reply.
     *
     * @param value wire DTO
     * @return discussion note
     */
    @Mapping(target = "system", source = "system", qualifiedByName = "truth")
    @Mapping(target = "resolvable", source = "resolvable", qualifiedByName = "truth")
    @Mapping(target = "resolved", source = "resolved", qualifiedByName = "truth")
    DiscussionNote note(NoteDto value);

    /**
     * Maps an optional inline position.
     *
     * @param value wire DTO
     * @return discussion position or {@code null}
     */
    DiscussionPosition position(PositionDto value);

    /**
     * Converts an optional GitLab URL.
     *
     * @param value URL text
     * @return parsed URL or {@code null}
     */
    default URI uri(String value) {
        return value == null || value.isBlank() ? null : URI.create(value);
    }

    /**
     * Normalizes nullable GitLab boolean values.
     *
     * @param value nullable value
     * @return {@code true} only for {@link Boolean#TRUE}
     */
    @Named("truth")
    default boolean truth(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    /**
     * Normalizes a nullable GitLab notes collection.
     *
     * @param values wire notes
     * @return immutable mapped notes
     */
    @Named("notes")
    default List<DiscussionNote> notes(List<NoteDto> values) {
        return values == null ? List.of() : values.stream().map(this::note).toList();
    }
}
