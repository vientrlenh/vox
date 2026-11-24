package com.sep.vox.interfaces.graphql.input.rubricsystem;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricVersionCommand;
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricVersionCommand;

class UpdateRubricVersionInputTests {

    private final UUID schoolId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();

    @Test
    void should_mark_description_and_effective_to_as_not_provided_when_omitted_for_both_owners() {
        UpdateRubricVersionInput input = inputOf(ArgumentValue.omitted(), ArgumentValue.omitted());

        assertThat(UpdateRubricVersionInput.toUpdateSystemRubricVersionCommand(versionId, input))
            .isEqualTo(new UpdateSystemRubricVersionCommand(versionId, null, null, false, null, null, false,
                null, null, null));
        assertThat(UpdateRubricVersionInput.toUpdateSchoolRubricVersionCommand(schoolId, versionId, input))
            .isEqualTo(new UpdateSchoolRubricVersionCommand(schoolId, versionId, null, null, false, null, null,
                false, null, null, null));
    }

    @Test
    void should_mark_description_and_effective_to_as_provided_on_explicit_null_for_both_owners() {
        UpdateRubricVersionInput input = inputOf(ArgumentValue.ofNullable(null), ArgumentValue.ofNullable(null));

        assertThat(UpdateRubricVersionInput.toUpdateSystemRubricVersionCommand(versionId, input))
            .isEqualTo(new UpdateSystemRubricVersionCommand(versionId, null, null, true, null, null, true,
                null, null, null));
        assertThat(UpdateRubricVersionInput.toUpdateSchoolRubricVersionCommand(schoolId, versionId, input))
            .isEqualTo(new UpdateSchoolRubricVersionCommand(schoolId, versionId, null, null, true, null, null,
                true, null, null, null));
    }

    @Test
    void should_mark_description_and_effective_to_as_provided_when_values_are_sent_for_both_owners() {
        Instant effectiveTo = Instant.parse("2026-12-31T00:00:00Z");
        UpdateRubricVersionInput input = inputOf(ArgumentValue.ofNullable("Text"), ArgumentValue.ofNullable(effectiveTo));

        assertThat(UpdateRubricVersionInput.toUpdateSystemRubricVersionCommand(versionId, input))
            .isEqualTo(new UpdateSystemRubricVersionCommand(versionId, null, "Text", true, null, effectiveTo,
                true, null, null, null));
        assertThat(UpdateRubricVersionInput.toUpdateSchoolRubricVersionCommand(schoolId, versionId, input))
            .isEqualTo(new UpdateSchoolRubricVersionCommand(schoolId, versionId, null, "Text", true, null,
                effectiveTo, true, null, null, null));
    }

    private static UpdateRubricVersionInput inputOf(ArgumentValue<String> description,
            ArgumentValue<Instant> effectiveTo) {
        return new UpdateRubricVersionInput(ArgumentValue.omitted(), description, ArgumentValue.omitted(),
            effectiveTo, ArgumentValue.omitted(), ArgumentValue.omitted(), ArgumentValue.omitted());
    }
}
