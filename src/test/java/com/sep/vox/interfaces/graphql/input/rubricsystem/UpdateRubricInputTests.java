package com.sep.vox.interfaces.graphql.input.rubricsystem;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricCommand;
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricCommand;

class UpdateRubricInputTests {

    private final UUID schoolId = UUID.randomUUID();
    private final UUID rubricId = UUID.randomUUID();

    @Test
    void should_mark_description_as_provided_when_sent_for_both_owners() {
        UpdateRubricInput input = new UpdateRubricInput(ArgumentValue.ofNullable("Name"), ArgumentValue.ofNullable("Text"));

        assertThat(UpdateRubricInput.toUpdateSystemRubricCommand(rubricId, input))
            .isEqualTo(new UpdateSystemRubricCommand(rubricId, "Name", "Text", true));
        assertThat(UpdateRubricInput.toUpdateSchoolRubricCommand(schoolId, rubricId, input))
            .isEqualTo(new UpdateSchoolRubricCommand(schoolId, rubricId, "Name", "Text", true));
    }

    @Test
    void should_mark_description_as_provided_on_explicit_null_and_not_provided_when_omitted() {
        UpdateRubricInput explicitNull = new UpdateRubricInput(ArgumentValue.omitted(), ArgumentValue.ofNullable(null));
        UpdateRubricInput omitted = new UpdateRubricInput(ArgumentValue.omitted(), ArgumentValue.omitted());

        assertThat(UpdateRubricInput.toUpdateSchoolRubricCommand(schoolId, rubricId, explicitNull))
            .isEqualTo(new UpdateSchoolRubricCommand(schoolId, rubricId, null, null, true));
        assertThat(UpdateRubricInput.toUpdateSchoolRubricCommand(schoolId, rubricId, omitted))
            .isEqualTo(new UpdateSchoolRubricCommand(schoolId, rubricId, null, null, false));
    }
}
