package com.sep.vox.interfaces.graphql.input.schoolclass;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.schoolclass.UpdateSchoolClassCommand;
import com.sep.vox.domain.model.school.SchoolClassStatus;

class UpdateSchoolClassInputTests {

    private final UUID id = UUID.randomUUID();

    @Test
    void should_mark_every_sent_value_as_provided() {
        UpdateSchoolClassInput input = new UpdateSchoolClassInput(
            ArgumentValue.ofNullable("English 02"), ArgumentValue.ofNullable("Evening class"),
            ArgumentValue.ofNullable(SchoolClassStatus.INACTIVE));

        assertThat(UpdateSchoolClassInput.toCommand(id, input)).isEqualTo(new UpdateSchoolClassCommand(
            id, "English 02", true, "Evening class", true, SchoolClassStatus.INACTIVE, true));
    }

    @Test
    void should_mark_every_explicit_null_as_provided() {
        UpdateSchoolClassInput input = new UpdateSchoolClassInput(
            ArgumentValue.ofNullable(null), ArgumentValue.ofNullable(null), ArgumentValue.ofNullable(null));

        assertThat(UpdateSchoolClassInput.toCommand(id, input)).isEqualTo(
            new UpdateSchoolClassCommand(id, null, true, null, true, null, true));
    }

    @Test
    void should_mark_every_omitted_field_as_not_provided() {
        UpdateSchoolClassInput input = new UpdateSchoolClassInput(
            ArgumentValue.omitted(), ArgumentValue.omitted(), ArgumentValue.omitted());

        assertThat(UpdateSchoolClassInput.toCommand(id, input)).isEqualTo(
            new UpdateSchoolClassCommand(id, null, false, null, false, null, false));
    }
}
