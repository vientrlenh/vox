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

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteSupportedLanguageCommand;
import com.sep.vox.application.port.input.usecase.supportedlanguage.DeleteSupportedLanguageUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.valueobject.LanguageCode;

class DeleteSupportedLanguageUseCaseTests {

    private SupportedLanguageRepository supportedLanguageRepository;
    private UserContextPort userContextPort;
    private DeleteSupportedLanguageUseCase useCase;

    @BeforeEach
    void setUp() {
        supportedLanguageRepository = mock(SupportedLanguageRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new DeleteSupportedLanguageUseCase(supportedLanguageRepository, userContextPort);
    }

    @Test
    void delete_should_soft_delete_active_language_and_update_audit_fields() {
        var id = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var language = language(id, true);
        when(supportedLanguageRepository.findById(id)).thenReturn(Optional.of(language));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(supportedLanguageRepository.save(any(SupportedLanguage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(new DeleteSupportedLanguageCommand(id));

        var captor = ArgumentCaptor.forClass(SupportedLanguage.class);
        verify(supportedLanguageRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.isActive()).isFalse();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUpdatedBy()).isEqualTo(userId);
    }

    @Test
    void delete_should_update_audit_fields_when_language_is_already_inactive() {
        var id = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var language = language(id, false);
        when(supportedLanguageRepository.findById(id)).thenReturn(Optional.of(language));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(supportedLanguageRepository.save(any(SupportedLanguage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(new DeleteSupportedLanguageCommand(id));

        var captor = ArgumentCaptor.forClass(SupportedLanguage.class);
        verify(supportedLanguageRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(userId);
    }

    @Test
    void delete_should_throw_when_language_not_found() {
        var id = UUID.randomUUID();
        when(supportedLanguageRepository.findById(id)).thenReturn(Optional.empty());

        var exception = assertThrows(NotFoundException.class, () -> useCase.execute(new DeleteSupportedLanguageCommand(id)));

        assertThat(exception).hasMessage("Không tìm thấy ngôn ngữ");
        verify(supportedLanguageRepository, never()).save(any());
    }

    @Test
    void delete_should_not_save_when_current_user_cannot_be_resolved() {
        var id = UUID.randomUUID();
        when(supportedLanguageRepository.findById(id)).thenReturn(Optional.of(language(id, true)));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenThrow(new IllegalStateException("unauthenticated"));

        assertThrows(IllegalStateException.class, () -> useCase.execute(new DeleteSupportedLanguageCommand(id)));

        verify(supportedLanguageRepository, never()).save(any());
    }

    private static SupportedLanguage language(UUID id, boolean active) {
        var now = Instant.now();
        return new SupportedLanguage(
            id,
            new LanguageCode("EN"),
            "English",
            "English language",
            active,
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }
}
