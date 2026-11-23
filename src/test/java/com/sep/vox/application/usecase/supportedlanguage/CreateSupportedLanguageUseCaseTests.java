package com.sep.vox.application.usecase.supportedlanguage;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.port.input.command.supportedlanguage.CreateSupportedLanguageCommand;
import com.sep.vox.application.port.input.usecase.supportedlanguage.CreateSupportedLanguageUseCase;
import com.sep.vox.application.response.input.supportedlanguage.CreateSupportedLanguageResponse;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.valueobject.LanguageCode;

class CreateSupportedLanguageUseCaseTests {

    private SupportedLanguageRepository supportedLanguageRepository;
    private CreateSupportedLanguageUseCase useCase;

    @BeforeEach
    void setUp() {
        supportedLanguageRepository = mock(SupportedLanguageRepository.class);
        useCase = new CreateSupportedLanguageUseCase(supportedLanguageRepository);
    }

    @Test
    void create_should_save_active_language_with_normalized_values() {
        UUID savedId = UUID.randomUUID();
        CreateSupportedLanguageCommand command = new CreateSupportedLanguageCommand("  en  ", "  English   Language  ", "  Main   language  ");

        when(supportedLanguageRepository.findByCode("EN")).thenReturn(Optional.empty());
        when(supportedLanguageRepository.save(any(SupportedLanguage.class))).thenAnswer(invocation -> {
            SupportedLanguage language = invocation.getArgument(0, SupportedLanguage.class);
            language.setId(savedId);
            return language;
        });

        CreateSupportedLanguageResponse response = useCase.execute(command);

        assertThat(response.supportedLanguageId()).isEqualTo(savedId);
        ArgumentCaptor<SupportedLanguage> captor = ArgumentCaptor.forClass(SupportedLanguage.class);
        verify(supportedLanguageRepository).save(captor.capture());
        SupportedLanguage saved = captor.getValue();
        assertThat(saved.getCode().value()).isEqualTo("EN");
        assertThat(saved.getName()).isEqualTo("English Language");
        assertThat(saved.getDescription()).isEqualTo("Main language");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void create_should_throw_when_code_already_exists() {
        CreateSupportedLanguageCommand command = new CreateSupportedLanguageCommand("en", "English", null);
        when(supportedLanguageRepository.findByCode("EN")).thenReturn(Optional.of(existingLanguage()));

        ResourceDuplicatedException exception = org.junit.jupiter.api.Assertions.assertThrows(ResourceDuplicatedException.class, () -> useCase.execute(command));

        assertThat(exception).hasMessage("Ngôn ngữ đã tồn tại với mã: EN");
        verify(supportedLanguageRepository).findByCode("EN");
        verify(supportedLanguageRepository, never()).save(any());
    }

    @Test
    void create_should_throw_when_code_contains_invalid_characters() {
        CreateSupportedLanguageCommand command = new CreateSupportedLanguageCommand("en-us", "English", null);
        when(supportedLanguageRepository.findByCode("EN-US")).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));

        verify(supportedLanguageRepository, never()).save(any());
    }

    private static SupportedLanguage existingLanguage() {
        SupportedLanguage language = new SupportedLanguage();
        language.setId(UUID.randomUUID());
        language.setCode(new LanguageCode("EN"));
        return language;
    }

    @Test
    void create_should_throw_duplicated_when_repository_reports_a_data_conflict() {
        CreateSupportedLanguageCommand command = new CreateSupportedLanguageCommand("en", "English", null);
        when(supportedLanguageRepository.findByCode("EN")).thenReturn(Optional.empty());
        when(supportedLanguageRepository.save(any())).thenThrow(new DataConflictException("unique violation", null));

        ResourceDuplicatedException exception = org.junit.jupiter.api.Assertions.assertThrows(ResourceDuplicatedException.class, () -> useCase.execute(command));

        assertThat(exception).hasMessage("Ngôn ngữ đã tồn tại với mã: EN");
    }
}
