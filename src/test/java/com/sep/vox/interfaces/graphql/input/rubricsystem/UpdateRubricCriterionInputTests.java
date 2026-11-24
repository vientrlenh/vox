package com.sep.vox.interfaces.graphql.input.rubricsystem;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricCriterionCommand;
import com.sep.vox.application.port.input.command.rubricsystem.UpdateSystemRubricCriterionCommand;

class UpdateRubricCriterionInputTests {

    private final UUID schoolId = UUID.randomUUID();
    private final UUID criterionId = UUID.randomUUID();

    @Test
    void should_mark_description_and_examples_as_not_provided_when_omitted_for_both_owners() {
        UpdateRubricCriterionInput input = inputOf(ArgumentValue.omitted(), ArgumentValue.omitted());

        assertThat(UpdateRubricCriterionInput.toUpdateSystemRubricCriterionCommand(criterionId, input))
            .isEqualTo(new UpdateSystemRubricCriterionCommand(criterionId, null, null, false, null, false,
                null, null, null, null, null));
        assertThat(UpdateRubricCriterionInput.toUpdateSchoolRubricCriterionCommand(schoolId, criterionId, input))
            .isEqualTo(new UpdateSchoolRubricCriterionCommand(schoolId, criterionId, null, null, false, null,
                false, null, null, null, null, null));
    }

    @Test
    void should_mark_description_and_examples_as_provided_on_explicit_null_for_both_owners() {
        UpdateRubricCriterionInput input = inputOf(ArgumentValue.ofNullable(null), ArgumentValue.ofNullable(null));

        assertThat(UpdateRubricCriterionInput.toUpdateSystemRubricCriterionCommand(criterionId, input))
            .isEqualTo(new UpdateSystemRubricCriterionCommand(criterionId, null, null, true, null, true,
                null, null, null, null, null));
        assertThat(UpdateRubricCriterionInput.toUpdateSchoolRubricCriterionCommand(schoolId, criterionId, input))
            .isEqualTo(new UpdateSchoolRubricCriterionCommand(schoolId, criterionId, null, null, true, null,
                true, null, null, null, null, null));
    }

    @Test
    void should_mark_description_and_examples_as_provided_when_values_are_sent_for_both_owners() {
        UpdateRubricCriterionInput input = inputOf(ArgumentValue.ofNullable("Text"), ArgumentValue.ofNullable("[]"));

        assertThat(UpdateRubricCriterionInput.toUpdateSystemRubricCriterionCommand(criterionId, input))
            .isEqualTo(new UpdateSystemRubricCriterionCommand(criterionId, null, "Text", true, "[]", true,
                null, null, null, null, null));
        assertThat(UpdateRubricCriterionInput.toUpdateSchoolRubricCriterionCommand(schoolId, criterionId, input))
            .isEqualTo(new UpdateSchoolRubricCriterionCommand(schoolId, criterionId, null, "Text", true, "[]",
                true, null, null, null, null, null));
    }

    private static UpdateRubricCriterionInput inputOf(ArgumentValue<String> description,
            ArgumentValue<String> examplesJson) {
        return new UpdateRubricCriterionInput(ArgumentValue.omitted(), description, examplesJson,
            ArgumentValue.omitted(), ArgumentValue.omitted(), ArgumentValue.omitted(), ArgumentValue.omitted(),
            ArgumentValue.omitted());
    }
}
