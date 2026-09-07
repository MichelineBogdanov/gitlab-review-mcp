package ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record VersionDto(String version) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record UserDto(long id, String username, String name, @JsonProperty("web_url") String webUrl) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record ProjectDto(
        long id,
        String name,
        @JsonProperty("name_with_namespace") String nameWithNamespace,
        String description,
        @JsonProperty("default_branch") String defaultBranch,
        Boolean archived,
        @JsonProperty("empty_repo") Boolean emptyRepo,
        @JsonProperty("last_activity_at") Instant lastActivityAt) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record RepositoryTreeEntryDto(String id, String name, String type, String path, String mode) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record RepositoryFileDto(
        @JsonProperty("file_name") String fileName,
        @JsonProperty("file_path") String filePath,
        long size,
        String encoding,
        String content,
        String ref,
        @JsonProperty("blob_id") String blobId,
        @JsonProperty("commit_id") String commitId,
        @JsonProperty("last_commit_id") String lastCommitId) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record RepositorySearchResultDto(String path, String ref, @JsonProperty("startline") Integer startLine, String data) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record MergeRequestDto(
        long iid,
        String title,
        String description,
        String state,
        @JsonProperty("source_branch") String sourceBranch,
        @JsonProperty("target_branch") String targetBranch,
        String sha,
        UserDto author,
        @JsonProperty("web_url") String webUrl) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record DiffVersionDto(
        long id,
        @JsonProperty("base_commit_sha") String baseSha,
        @JsonProperty("start_commit_sha") String startSha,
        @JsonProperty("head_commit_sha") String headSha) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record DiffFileDto(
        @JsonProperty("old_path") String oldPath,
        @JsonProperty("new_path") String newPath,
        String diff,
        @JsonProperty("new_file") Boolean newFile,
        @JsonProperty("renamed_file") Boolean renamedFile,
        @JsonProperty("deleted_file") Boolean deletedFile,
        @JsonProperty("generated_file") Boolean generatedFile,
        Boolean collapsed,
        @JsonProperty("too_large") Boolean tooLarge) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record DiscussionDto(String id, @JsonProperty("individual_note") Boolean individualNote, List<NoteDto> notes) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record NoteDto(
        long id,
        String body,
        UserDto author,
        @JsonProperty("created_at") Instant createdAt,
        Boolean system,
        Boolean resolvable,
        Boolean resolved,
        PositionDto position) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record PositionDto(
        @JsonProperty("old_path") String oldPath,
        @JsonProperty("new_path") String newPath,
        @JsonProperty("old_line") Integer oldLine,
        @JsonProperty("new_line") Integer newLine,
        @JsonProperty("base_sha") String baseSha,
        @JsonProperty("start_sha") String startSha,
        @JsonProperty("head_sha") String headSha) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record CreatedDiscussionDto(String id, List<NoteDto> notes) {
}
