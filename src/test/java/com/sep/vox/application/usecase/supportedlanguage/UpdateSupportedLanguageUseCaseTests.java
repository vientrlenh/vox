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
import org.springframework.dao.DataIntegrityViolationException;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateSupportedLanguageCommand;
import com.sep.vox.application.port.input.usecase.supportedlanguage.UpdateSupportedLanguageUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.valueobject.LanguageCode;

class UpdateSupportedLanguageUseCaseTests {

    private SupportedLanguageRepository supportedLanguageRepository;
    private UserContextPort userContextPort;
    private UpdateSupportedLanguageUseCase useCase;

    @BeforeEach
    void setUp() {
        supportedLanguageRepository = mock(SupportedLanguageRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new UpdateSupportedLanguageUseCase(supportedLanguageRepository, userContextPort);
    }

    @Test
    void update_should_call_single_update_statement_with_normalized_values_and_audit_fields() {
        var id = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var command = new UpdateSupportedLanguageCommand(
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
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(supportedLanguageRepository.updateMutableFields(
            eq(id),
            eq("FR"),
            eq(true),
            eq("French Language"),
            eq(true),
            eq("New description"),
            eq(true),
            eq(false),
            eq(true),
            any(Instant.class),
            eq(userId)
        )).thenReturn(1);

        var result = useCase.execute(command);

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
            eq(true),
            any(Instant.class),
            eq(userId)
        );
    }

    @Test
    void update_should_clear_description_when_description_is_provided_as_null() {
        var id = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var command = new UpdateSupportedLanguageCommand(id, null, false, null, false, null, true, null, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(supportedLanguageRepository.updateMutableFields(
            eq(id),
            isNull(),
            eq(false),
            isNull(),
            eq(false),
            isNull(),
            eq(true),
            isNull(),
            eq(false),
            any(Instant.class),
            eq(userId)
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
            eq(false),
            any(Instant.class),
            eq(userId)
        );
    }

    @Test
    void update_should_throw_when_code_belongs_to_another_language() {
        var id = UUID.randomUUID();
        var otherId = UUID.randomUUID();
        var command = new UpdateSupportedLanguageCommand(id, "fr", true, null, false, null, false, null, false);
        when(supportedLanguageRepository.findByCode("FR")).thenReturn(Optional.of(language(otherId, "FR", "French", null, true)));

        var exception = assertThrows(DuplicatedException.class, () -> useCase.execute(command));

        assertThat(exception).hasMessage("Ngôn ngữ đã tồn tại với mã: FR");
        verify(supportedLanguageRepository, never()).updateMutableFields(any(), any(), any(Boolean.class), any(), any(Boolean.class),
            any(), any(Boolean.class), any(), any(Boolean.class), any(), any());
    }

    @Test
    void update_should_allow_same_code_on_same_language() {
        var id = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var existing = language(id, "EN", "English", null, true);
        var command = new UpdateSupportedLanguageCommand(id, "en", true, null, false, null, false, null, false);
        when(supportedLanguageRepository.findByCode("EN")).thenReturn(Optional.of(existing));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(supportedLanguageRepository.updateMutableFields(
            eq(id),
            eq("EN"),
            eq(true),
            isNull(),
            eq(false),
            isNull(),
            eq(false),
            isNull(),
            eq(false),
            any(Instant.class),
            eq(userId)
        )).thenReturn(1);

        var result = useCase.execute(command);

        assertThat(result.supportedLanguageId()).isEqualTo(id);
    }

    @Test
    void update_should_throw_when_no_field_is_provided() {
        var id = UUID.randomUUID();
        var command = new UpdateSupportedLanguageCommand(id, null, false, null, false, null, false, null, false);

        var exception = assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));

        assertThat(exception).hasMessage("Cần cung cấp ít nhất một trường để cập nhật");
        verify(supportedLanguageRepository, never()).updateMutableFields(any(), any(), any(Boolean.class), any(), any(Boolean.class),
            any(), any(Boolean.class), any(), any(Boolean.class), any(), any());
    }

    @Test
    void update_should_throw_when_name_is_blank() {
        var id = UUID.randomUUID();
        var command = new UpdateSupportedLanguageCommand(id, null, false, "   ", true, null, false, null, false);

        var exception = assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));

        assertThat(exception).hasMessage("Tên ngôn ngữ không được để trống");
        verify(supportedLanguageRepository, never()).updateMutableFields(any(), any(), any(Boolean.class), any(), any(Boolean.class),
            any(), any(Boolean.class), any(), any(Boolean.class), any(), any());
    }

    @Test
    void update_should_throw_when_code_is_invalid() {
        var id = UUID.randomUUID();
        var command = new UpdateSupportedLanguageCommand(id, "en-us", true, null, false, null, false, null, false);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));

        verify(supportedLanguageRepository, never()).updateMutableFields(any(), any(), any(Boolean.class), any(), any(Boolean.class),
            any(), any(Boolean.class), any(), any(Boolean.class), any(), any());
    }

    @Test
    void update_should_throw_when_description_is_too_long() {
        var id = UUID.randomUUID();
        var command = new UpdateSupportedLanguageCommand(id, null, false, null, false, "a".repeat(2049), true, null, false);

        var exception = assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));

        assertThat(exception).hasMessage("Mô tả ngôn ngữ không được vượt quá 2048 ký tự");
        verify(supportedLanguageRepository, never()).updateMutableFields(any(), any(), any(Boolean.class), any(), any(Boolean.class),
            any(), any(Boolean.class), any(), any(Boolean.class), any(), any());
    }

    @Test
    void update_should_throw_when_update_statement_returns_zero() {
        var id = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var command = new UpdateSupportedLanguageCommand(id, "FR", true, null, false, null, false, null, false);
        when(supportedLanguageRepository.findByCode("FR")).thenReturn(Optional.empty());
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(supportedLanguageRepository.updateMutableFields(
            eq(id),
            eq("FR"),
            eq(true),
            isNull(),
            eq(false),
            isNull(),
            eq(false),
            isNull(),
            eq(false),
            any(Instant.class),
            eq(userId)
        )).thenReturn(0);

        var exception = assertThrows(NotFoundException.class, () -> useCase.execute(command));

        assertThat(exception).hasMessage("Không tìm thấy ngôn ngữ");
    }

    @Test
    void update_should_rethrow_database_duplicate_as_duplicated_exception() {
        var id = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var command = new UpdateSupportedLanguageCommand(id, "FR", true, null, false, null, false, null, false);
        when(supportedLanguageRepository.findByCode("FR")).thenReturn(Optional.empty());
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(supportedLanguageRepository.updateMutableFields(
            eq(id),
            eq("FR"),
            eq(true),
            isNull(),
            eq(false),
            isNull(),
            eq(false),
            isNull(),
            eq(false),
            any(Instant.class),
            eq(userId)
        )).thenThrow(new DataIntegrityViolationException("duplicate"));

        var exception = assertThrows(DuplicatedException.class, () -> useCase.execute(command));

        assertThat(exception).hasMessage("Ngôn ngữ đã tồn tại với mã: FR");
    }

    private static SupportedLanguage language(UUID id, String code, String name, String description, boolean active) {
        var now = Instant.now();
        return new SupportedLanguage(
            id,
            new LanguageCode(code),
            name,
            description,
            active,
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }
}
