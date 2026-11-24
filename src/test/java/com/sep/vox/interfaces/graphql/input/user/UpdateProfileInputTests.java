package com.sep.vox.interfaces.graphql.input.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.user.UpdateProfileCommand;

class UpdateProfileInputTests {

    @Test
    void should_mark_every_sent_value_as_provided() {
        UpdateProfileInput input = new UpdateProfileInput(
            ArgumentValue.ofNullable("Nguyen Van A"), ArgumentValue.ofNullable("0911000001"),
            ArgumentValue.ofNullable("Ha Noi"), ArgumentValue.ofNullable(LocalDate.of(2008, 5, 1)),
            ArgumentValue.ofNullable("https://cdn.example.com/a.png"));

        UpdateProfileCommand command = UpdateProfileInput.toCommand(input);

        assertThat(command).isEqualTo(new UpdateProfileCommand(
            "Nguyen Van A", true, "0911000001", true, "Ha Noi", true,
            LocalDate.of(2008, 5, 1), true, "https://cdn.example.com/a.png", true));
    }

    @Test
    void should_mark_every_explicit_null_as_provided() {
        ArgumentValue<String> sentNull = ArgumentValue.ofNullable(null);
        UpdateProfileInput input = new UpdateProfileInput(
            sentNull, sentNull, sentNull, ArgumentValue.ofNullable(null), sentNull);

        UpdateProfileCommand command = UpdateProfileInput.toCommand(input);

        assertThat(command).isEqualTo(new UpdateProfileCommand(
            null, true, null, true, null, true, null, true, null, true));
    }

    @Test
    void should_mark_every_omitted_field_as_not_provided() {
        UpdateProfileInput input = new UpdateProfileInput(
            ArgumentValue.omitted(), ArgumentValue.omitted(), ArgumentValue.omitted(),
            ArgumentValue.omitted(), ArgumentValue.omitted());

        UpdateProfileCommand command = UpdateProfileInput.toCommand(input);

        assertThat(command).isEqualTo(new UpdateProfileCommand(
            null, false, null, false, null, false, null, false, null, false));
    }
}
