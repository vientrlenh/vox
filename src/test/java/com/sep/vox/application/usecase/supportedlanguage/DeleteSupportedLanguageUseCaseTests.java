package com.sep.vox.application.usecase.supportedlanguage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.DeleteSupportedLanguageCommand;
import com.sep.vox.application.port.input.usecase.supportedlanguage.DeleteSupportedLanguageUseCase;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.valueobject.LanguageCode;

class DeleteSupportedLanguageUseCaseTests {

    private SupportedLanguageRepository supportedLanguageRepository;
    private DeleteSupportedLanguageUseCase useCase;

    @BeforeEach
    void setUp() {
        supportedLanguageRepository = mock(SupportedLanguageRepository.class);
        useCase = new DeleteSupportedLanguageUseCase(supportedLanguageRepository);
    }

    @Test
    void delete_should_soft_delete_active_language() {
        UUID id = UUID.randomUUID();
        SupportedLanguage language = language(id, true);
        when(supportedLanguageRepository.findById(id)).thenReturn(Optional.of(language));
        when(supportedLanguageRepository.save(any(SupportedLanguage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(new DeleteSupportedLanguageCommand(id));

        ArgumentCaptor<SupportedLanguage> captor = ArgumentCaptor.forClass(SupportedLanguage.class);
        verify(supportedLanguageRepository).save(captor.capture());
        SupportedLanguage saved = captor.getValue();
        assertThat(saved.isActive()).isFalse();
    }

    @Test
    void delete_should_keep_inactive_language_inactive() {
        UUID id = UUID.randomUUID();
        SupportedLanguage language = language(id, false);
        when(supportedLanguageRepository.findById(id)).thenReturn(Optional.of(language));
        when(supportedLanguageRepository.save(any(SupportedLanguage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(new DeleteSupportedLanguageCommand(id));

        ArgumentCaptor<SupportedLanguage> captor = ArgumentCaptor.forClass(SupportedLanguage.class);
        verify(supportedLanguageRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    void delete_should_throw_when_language_not_found() {
        UUID id = UUID.randomUUID();
        when(supportedLanguageRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> useCase.execute(new DeleteSupportedLanguageCommand(id)));

        assertThat(exception).hasMessage("Không tìm thấy ngôn ngữ");
        verify(supportedLanguageRepository, never()).save(any());
    }

    private static SupportedLanguage language(UUID id, boolean active) {
        return SupportedLanguage.builder()
            .id(id)
            .code(new LanguageCode("EN"))
            .name("English")
            .description("English language")
            .active(active)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(UUID.randomUUID().toString())
            .updatedBy(UUID.randomUUID().toString())
            .build();
    }
}
