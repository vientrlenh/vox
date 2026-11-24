package com.sep.vox.interfaces.graphql.input.schooluser;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.schooluser.UpdateSchoolUserCommand;

class UpdateSchoolUserInputTests {

    private final UUID schoolId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void should_mark_every_sent_value_as_provided() {
        UpdateSchoolUserInput input = new UpdateSchoolUserInput(
            ArgumentValue.ofNullable("Nguyen Van A"), ArgumentValue.ofNullable("0911000001"),
            ArgumentValue.ofNullable("Ha Noi"), ArgumentValue.ofNullable(LocalDate.of(2008, 5, 1)),
            ArgumentValue.ofNullable("https://cdn.example.com/a.png"));

        assertThat(UpdateSchoolUserInput.toCommand(schoolId, userId, input)).isEqualTo(new UpdateSchoolUserCommand(
            schoolId, userId, "Nguyen Van A", true, "0911000001", true, "Ha Noi", true,
            LocalDate.of(2008, 5, 1), true, "https://cdn.example.com/a.png", true));
    }

    @Test
    void should_mark_every_explicit_null_as_provided() {
        UpdateSchoolUserInput input = new UpdateSchoolUserInput(
            ArgumentValue.ofNullable(null), ArgumentValue.ofNullable(null), ArgumentValue.ofNullable(null),
            ArgumentValue.ofNullable(null), ArgumentValue.ofNullable(null));

        assertThat(UpdateSchoolUserInput.toCommand(schoolId, userId, input)).isEqualTo(new UpdateSchoolUserCommand(
            schoolId, userId, null, true, null, true, null, true, null, true, null, true));
    }

    @Test
    void should_mark_every_omitted_field_as_not_provided() {
        UpdateSchoolUserInput input = new UpdateSchoolUserInput(
            ArgumentValue.omitted(), ArgumentValue.omitted(), ArgumentValue.omitted(),
            ArgumentValue.omitted(), ArgumentValue.omitted());

        assertThat(UpdateSchoolUserInput.toCommand(schoolId, userId, input)).isEqualTo(new UpdateSchoolUserCommand(
            schoolId, userId, null, false, null, false, null, false, null, false, null, false));
    }
}
