package com.sep.vox.application.usecase.supportedlanguage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sep.vox.application.exception.DataConflictException;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.supportedlanguage.UpdateSupportedLanguageCommand;
import com.sep.vox.application.port.input.usecase.supportedlanguage.UpdateSupportedLanguageUseCase;
import com.sep.vox.application.response.input.supportedlanguage.UpdateSupportedLanguageResponse;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.valueobject.LanguageCode;

class UpdateSupportedLanguageUseCaseTests {

    private SupportedLanguageRepository supportedLanguageRepository;
    private UpdateSupportedLanguageUseCase useCase;

    @BeforeEach
    void setUp() {
        supportedLanguageRepository = mock(SupportedLanguageRepository.class);
        useCase = new UpdateSupportedLanguageUseCase(supportedLanguageRepository);
    }

    @Test
    void update_should_call_single_update_statement_with_normalized_values_and_audit_fields() {
        UUID id = UUID.randomUUID();
        UpdateSupportedLanguageCommand command = new UpdateSupportedLanguageCommand(
            id,
            " fr ",
            true,
            "  French   Language  ",
            true,
            "  New   description  ",
            true,
            false,
            true
        );
        when(supportedLanguageRepository.findByCode("FR")).thenReturn(Optional.empty());
        when(supportedLanguageRepository.updateMutableFields(
            eq(id),
            eq("FR"),
            eq(true),
            eq("French Language"),
            eq(true),
            eq("New description"),
            eq(true),
            eq(false),
            eq(true)
        )).thenReturn(1);

        UpdateSupportedLanguageResponse result = useCase.execute(command);

        assertThat(result.supportedLanguageId()).isEqualTo(id);
        verify(supportedLanguageRepository).updateMutableFields(
            eq(id),
            eq("FR"),
            eq(true),
            eq("French Language"),
            eq(true),
            eq("New description"),
            eq(true),
            eq(false),
            eq(true)
        );
    }

    @Test
    void update_should_clear_description_when_description_is_provided_as_null() {
        UUID id = UUID.randomUUID();
        UpdateSupportedLanguageCommand command = new UpdateSupportedLanguageCommand(id, null, false, null, false, null, true, null, false);
        when(supportedLanguageRepository.updateMutableFields(
            eq(id),
            isNull(),
            eq(false),
            isNull(),
            eq(false),
            isNull(),
            eq(true),
            isNull(),
            eq(false)
        )).thenReturn(1);

        useCase.execute(command);

        verify(supportedLanguageRepository).updateMutableFields(
            eq(id),
            isNull(),
            eq(false),
            isNull(),
            eq(false),
            isNull(),
            eq(true),
            isNull(),
            eq(false)
        );
    }

    @Test
    void update_should_throw_when_code_belongs_to_another_language() {
        UUID id = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UpdateSupportedLanguageCommand command = new UpdateSupportedLanguageCommand(id, "fr", true, null, false, null, false, null, false);
        when(supportedLanguageRepository.findByCode("FR")).thenReturn(Optional.of(language(otherId, "FR", "French", null, true)));

        ResourceDuplicatedException exception = assertThrows(ResourceDuplicatedException.class, () -> useCase.execute(command));

        assertThat(exception).hasMessage("Language already exists with code: FR");
        verify(supportedLanguageRepository, never()).updateMutableFields(any(), any(), any(Boolean.class), any(), any(Boolean.class),
            any(), any(Boolean.class), any(), any(Boolean.class));
    }

    @Test
    void update_should_allow_same_code_on_same_language() {
        UUID id = UUID.randomUUID();
        SupportedLanguage existing = language(id, "EN", "English", null, true);
        UpdateSupportedLanguageCommand command = new UpdateSupportedLanguageCommand(id, "en", true, null, false, null, false, null, false);
        when(supportedLanguageRepository.findByCode("EN")).thenReturn(Optional.of(existing));
        when(supportedLanguageRepository.updateMutableFields(
            eq(id),
            eq("EN"),
            eq(true),
            isNull(),
            eq(false),
            isNull(),
            eq(false),
            isNull(),
            eq(false)
        )).thenReturn(1);

        UpdateSupportedLanguageResponse result = useCase.execute(command);

        assertThat(result.supportedLanguageId()).isEqualTo(id);
    }

    @Test
    void update_should_throw_when_no_field_is_provided() {
        UUID id = UUID.randomUUID();
        UpdateSupportedLanguageCommand command = new UpdateSupportedLanguageCommand(id, null, false, null, false, null, false, null, false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));

        assertThat(exception).hasMessage("At least one field must be provided to update");
        verify(supportedLanguageRepository, never()).updateMutableFields(any(), any(), any(Boolean.class), any(), any(Boolean.class),
            any(), any(Boolean.class), any(), any(Boolean.class));
    }

    @Test
    void update_should_throw_when_name_is_blank() {
        UUID id = UUID.randomUUID();
        UpdateSupportedLanguageCommand command = new UpdateSupportedLanguageCommand(id, null, false, "   ", true, null, false, null, false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));

        assertThat(exception).hasMessage("Language name must not be empty");
        verify(supportedLanguageRepository, never()).updateMutableFields(any(), any(), any(Boolean.class), any(), any(Boolean.class),
            any(), any(Boolean.class), any(), any(Boolean.class));
    }

    @Test
    void update_should_throw_when_code_is_invalid() {
        UUID id = UUID.randomUUID();
        UpdateSupportedLanguageCommand command = new UpdateSupportedLanguageCommand(id, "en-us", true, null, false, null, false, null, false);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));

        verify(supportedLanguageRepository, never()).updateMutableFields(any(), any(), any(Boolean.class), any(), any(Boolean.class),
            any(), any(Boolean.class), any(), any(Boolean.class));
    }

    @Test
    void update_should_throw_when_description_is_too_long() {
        UUID id = UUID.randomUUID();
        UpdateSupportedLanguageCommand command = new UpdateSupportedLanguageCommand(id, null, false, null, false, "a".repeat(2049), true, null, false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));

        assertThat(exception).hasMessage("Language description must not exceed 2048 characters");
        verify(supportedLanguageRepository, never()).updateMutableFields(any(), any(), any(Boolean.class), any(), any(Boolean.class),
            any(), any(Boolean.class), any(), any(Boolean.class));
    }

    @Test
    void update_should_throw_when_update_statement_returns_zero() {
        UUID id = UUID.randomUUID();
        UpdateSupportedLanguageCommand command = new UpdateSupportedLanguageCommand(id, "FR", true, null, false, null, false, null, false);
        when(supportedLanguageRepository.findByCode("FR")).thenReturn(Optional.empty());
        when(supportedLanguageRepository.updateMutableFields(
            eq(id),
            eq("FR"),
            eq(true),
            isNull(),
            eq(false),
            isNull(),
            eq(false),
            isNull(),
            eq(false)
        )).thenReturn(0);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> useCase.execute(command));

        assertThat(exception).hasMessage("Language not found");
    }

    @Test
    void update_should_rethrow_database_duplicate_as_duplicated_exception() {
        UUID id = UUID.randomUUID();
        UpdateSupportedLanguageCommand command = new UpdateSupportedLanguageCommand(id, "FR", true, null, false, null, false, null, false);
        when(supportedLanguageRepository.findByCode("FR")).thenReturn(Optional.empty());
        when(supportedLanguageRepository.updateMutableFields(
            eq(id),
            eq("FR"),
            eq(true),
            isNull(),
            eq(false),
            isNull(),
            eq(false),
            isNull(),
            eq(false)
        )).thenThrow(new DataConflictException("duplicate", null));

        ResourceDuplicatedException exception = assertThrows(ResourceDuplicatedException.class, () -> useCase.execute(command));

        assertThat(exception).hasMessage("Language already exists with code: FR");
    }

    private static SupportedLanguage language(UUID id, String code, String name, String description, boolean active) {
        return SupportedLanguage.builder()
            .id(id)
            .code(new LanguageCode(code))
            .name(name)
            .description(description)
            .active(active)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(UUID.randomUUID().toString())
            .updatedBy(UUID.randomUUID().toString())
            .build();
    }
}
