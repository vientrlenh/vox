package com.sep.vox.interfaces.graphql.input.supportedlanguage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.supportedlanguage.UpdateSupportedLanguageCommand;

class UpdateSupportedLanguageInputTests {

    private final UUID id = UUID.randomUUID();

    @Test
    void should_mark_every_sent_value_as_provided() {
        UpdateSupportedLanguageInput input = new UpdateSupportedLanguageInput(
            ArgumentValue.ofNullable("fr"), ArgumentValue.ofNullable("French"),
            ArgumentValue.ofNullable("Romance language"), ArgumentValue.ofNullable(false));

        assertThat(UpdateSupportedLanguageInput.toCommand(id, input)).isEqualTo(
            new UpdateSupportedLanguageCommand(id, "fr", true, "French", true, "Romance language", true, false, true));
    }

    @Test
    void should_mark_every_explicit_null_as_provided() {
        UpdateSupportedLanguageInput input = new UpdateSupportedLanguageInput(
            ArgumentValue.ofNullable(null), ArgumentValue.ofNullable(null),
            ArgumentValue.ofNullable(null), ArgumentValue.ofNullable(null));

        assertThat(UpdateSupportedLanguageInput.toCommand(id, input)).isEqualTo(
            new UpdateSupportedLanguageCommand(id, null, true, null, true, null, true, null, true));
    }

    @Test
    void should_mark_every_omitted_field_as_not_provided() {
        UpdateSupportedLanguageInput input = new UpdateSupportedLanguageInput(
            ArgumentValue.omitted(), ArgumentValue.omitted(), ArgumentValue.omitted(), ArgumentValue.omitted());

        assertThat(UpdateSupportedLanguageInput.toCommand(id, input)).isEqualTo(
            new UpdateSupportedLanguageCommand(id, null, false, null, false, null, false, null, false));
    }
}
