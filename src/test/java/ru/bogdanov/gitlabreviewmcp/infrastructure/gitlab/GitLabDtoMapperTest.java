package ru.bogdanov.gitlabreviewmcp.infrastructure.gitlab;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class GitLabDtoMapperTest {

    private final GitLabDtoMapper mapper = Mappers.getMapper(GitLabDtoMapper.class);

    @Test
    void normalizesNullableWireValues() {
        DiffFileDto source = new DiffFileDto(
                "src/Old.java", "src/New.java", null, null, null, null, null, null, null);

        var result = mapper.diff(source, true);

        assertThat(result.diff()).isEmpty();
        assertThat(result.newFile()).isFalse();
        assertThat(result.renamedFile()).isFalse();
        assertThat(result.deletedFile()).isFalse();
        assertThat(result.generatedFile()).isFalse();
        assertThat(result.collapsed()).isFalse();
        assertThat(result.tooLarge()).isFalse();
        assertThat(result.truncated()).isTrue();
    }

    @Test
    void mapsNullDiscussionNotesToEmptyList() {
        var result = mapper.discussion(new DiscussionDto("discussion", null, null));

        assertThat(result.individualNote()).isFalse();
        assertThat(result.notes()).isEmpty();
    }

    @Test
    void mapsBlankOptionalUrlToNull() {
        var result = mapper.user(new UserDto(1, "reviewer", "Reviewer", " "));

        assertThat(result.webUrl()).isNull();
    }
}
