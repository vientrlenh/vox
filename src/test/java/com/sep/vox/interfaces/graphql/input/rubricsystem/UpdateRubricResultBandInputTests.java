package com.sep.vox.interfaces.graphql.input.rubricsystem;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricResultBandCommand;
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricResultBandCommand;

class UpdateRubricResultBandInputTests {

    private final UUID schoolId = UUID.randomUUID();
    private final UUID resultBandId = UUID.randomUUID();

    @Test
    void should_mark_description_as_not_provided_when_omitted_for_both_owners() {
        UpdateRubricResultBandInput input = inputOf(ArgumentValue.omitted());

        assertThat(UpdateRubricResultBandInput.toUpdateSystemRubricResultBandCommand(resultBandId, input))
            .isEqualTo(new UpdateSystemRubricResultBandCommand(resultBandId, null, null, false, null, null, null));
        assertThat(UpdateRubricResultBandInput.toUpdateSchoolRubricResultBandCommand(schoolId, resultBandId, input))
            .isEqualTo(new UpdateSchoolRubricResultBandCommand(schoolId, resultBandId, null, null, false, null,
                null, null));
    }

    @Test
    void should_mark_description_as_provided_on_explicit_null_for_both_owners() {
        UpdateRubricResultBandInput input = inputOf(ArgumentValue.ofNullable(null));

        assertThat(UpdateRubricResultBandInput.toUpdateSystemRubricResultBandCommand(resultBandId, input))
            .isEqualTo(new UpdateSystemRubricResultBandCommand(resultBandId, null, null, true, null, null, null));
        assertThat(UpdateRubricResultBandInput.toUpdateSchoolRubricResultBandCommand(schoolId, resultBandId, input))
            .isEqualTo(new UpdateSchoolRubricResultBandCommand(schoolId, resultBandId, null, null, true, null,
                null, null));
    }

    @Test
    void should_mark_description_as_provided_when_a_value_is_sent_for_both_owners() {
        UpdateRubricResultBandInput input = inputOf(ArgumentValue.ofNullable("Text"));

        assertThat(UpdateRubricResultBandInput.toUpdateSystemRubricResultBandCommand(resultBandId, input))
            .isEqualTo(new UpdateSystemRubricResultBandCommand(resultBandId, null, "Text", true, null, null, null));
        assertThat(UpdateRubricResultBandInput.toUpdateSchoolRubricResultBandCommand(schoolId, resultBandId, input))
            .isEqualTo(new UpdateSchoolRubricResultBandCommand(schoolId, resultBandId, null, "Text", true, null,
                null, null));
    }

    private static UpdateRubricResultBandInput inputOf(ArgumentValue<String> description) {
        return new UpdateRubricResultBandInput(ArgumentValue.omitted(), description, ArgumentValue.omitted(),
            ArgumentValue.omitted(), ArgumentValue.omitted());
    }
}
