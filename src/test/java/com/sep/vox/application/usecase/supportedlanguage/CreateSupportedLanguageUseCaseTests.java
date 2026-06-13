package com.sep.vox.application.usecase.supportedlanguage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.CreateSupportedLanguageCommand;
import com.sep.vox.application.port.input.usecase.supportedlanguage.CreateSupportedLanguageUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.valueobject.LanguageCode;

class CreateSupportedLanguageUseCaseTests {

    private SupportedLanguageRepository supportedLanguageRepository;
    private UserContextPort userContextPort;
    private CreateSupportedLanguageUseCase useCase;

    @BeforeEach
    void setUp() {
        supportedLanguageRepository = mock(SupportedLanguageRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new CreateSupportedLanguageUseCase(supportedLanguageRepository, userContextPort);
    }

    @Test
    void create_should_save_active_language_with_normalized_values_and_audit_fields() {
        var userId = UUID.randomUUID();
        var savedId = UUID.randomUUID();
        var command = new CreateSupportedLanguageCommand("  en  ", "  English   Language  ", "  Main   language  ");

        when(supportedLanguageRepository.findByCode("EN")).thenReturn(Optional.empty());
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(supportedLanguageRepository.save(any(SupportedLanguage.class))).thenAnswer(invocation -> {
            var language = invocation.getArgument(0, SupportedLanguage.class);
            language.setId(savedId);
            return language;
        });

        var response = useCase.execute(command);

        assertThat(response.supportedLanguageId()).isEqualTo(savedId);
        var captor = ArgumentCaptor.forClass(SupportedLanguage.class);
        verify(supportedLanguageRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getCode().value()).isEqualTo("EN");
        assertThat(saved.getName()).isEqualTo("English Language");
        assertThat(saved.getDescription()).isEqualTo("Main language");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo(userId);
        assertThat(saved.getUpdatedBy()).isEqualTo(userId);
    }

    @Test
    void create_should_throw_when_code_already_exists() {
        var command = new CreateSupportedLanguageCommand("en", "English", null);
        when(supportedLanguageRepository.findByCode("EN")).thenReturn(Optional.of(existingLanguage()));

        assertThrows(DuplicatedException.class, () -> useCase.execute(command));

        verify(supportedLanguageRepository).findByCode("EN");
        verify(supportedLanguageRepository, never()).save(any());
    }

    @Test
    void create_should_throw_when_code_contains_invalid_characters() {
        var command = new CreateSupportedLanguageCommand("en-us", "English", null);
        when(supportedLanguageRepository.findByCode("EN-US")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));

        verify(supportedLanguageRepository, never()).save(any());
    }

    private static SupportedLanguage existingLanguage() {
        var language = new SupportedLanguage();
        language.setId(UUID.randomUUID());
        language.setCode(new LanguageCode("EN"));
        return language;
    }
}
