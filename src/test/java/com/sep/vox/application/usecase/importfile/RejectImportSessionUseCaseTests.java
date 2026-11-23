package com.sep.vox.application.usecase.importfile;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.RejectImportSessionCommand;
import com.sep.vox.application.port.input.usecase.importfile.RejectImportSessionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.RejectImportSessionResponse;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class RejectImportSessionUseCaseTests {

    private ImportSessionRepository importSessionRepository;
    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private UserContextPort userContextPort;
    private SchoolUserRepository schoolUserRepository;
    private RejectImportSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        importSessionRepository = mock(ImportSessionRepository.class);
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userContextPort = mock(UserContextPort.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        useCase = new RejectImportSessionUseCase(importSessionRepository, userRepository, schoolRepository, userContextPort, schoolUserRepository);
    }

    @Test
    void execute_should_cancel_previewed_session_for_current_school() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        ImportSession session = session(sessionId, schoolId, ImportSessionStatus.PREVIEWED, Instant.now().plus(1, ChronoUnit.DAYS));

        mockActiveContext(userId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(importSessionRepository.save(any(ImportSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RejectImportSessionResponse response = useCase.execute(new RejectImportSessionCommand(sessionId, "  User   cancelled  "));

        assertThat(response.importSessionId()).isEqualTo(sessionId);
        assertThat(response.status()).isEqualTo("CANCELLED");
        assertThat(response.reason()).isEqualTo("User cancelled");
        assertThat(session.getStatus()).isEqualTo(ImportSessionStatus.CANCELLED);
        assertThat(session.getFailureReason()).isEqualTo("User cancelled");
    }

    @Test
    void execute_should_store_null_reason_when_reason_blank() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        ImportSession session = session(sessionId, schoolId, ImportSessionStatus.PREVIEWED, Instant.now().plus(1, ChronoUnit.DAYS));

        mockActiveContext(userId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(importSessionRepository.save(any(ImportSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RejectImportSessionResponse response = useCase.execute(new RejectImportSessionCommand(sessionId, " "));

        assertThat(response.reason()).isNull();
        assertThat(session.getFailureReason()).isNull();
    }

    @Test
    void execute_should_throw_when_session_belongs_to_other_school() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        mockActiveContext(userId, schoolId);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(session(sessionId, UUID.randomUUID(), ImportSessionStatus.PREVIEWED, Instant.now().plus(1, ChronoUnit.DAYS))));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new RejectImportSessionCommand(sessionId, null)));
    }

    @Test
    void execute_should_throw_when_session_not_found() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        mockActiveContext(userId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(new RejectImportSessionCommand(sessionId, null)));
    }

    @Test
    void execute_should_throw_when_session_not_previewed() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        mockActiveContext(userId, schoolId);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(session(sessionId, schoolId, ImportSessionStatus.COMPLETED, Instant.now().plus(1, ChronoUnit.DAYS))));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new RejectImportSessionCommand(sessionId, null)));
    }

    @Test
    void execute_should_mark_expired_session_and_throw() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        ImportSession session = session(sessionId, schoolId, ImportSessionStatus.PREVIEWED, Instant.now().minus(1, ChronoUnit.MINUTES));

        mockActiveContext(userId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(importSessionRepository.save(any(ImportSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new RejectImportSessionCommand(sessionId, null)));

        ArgumentCaptor<ImportSession> captor = ArgumentCaptor.forClass(ImportSession.class);
        verify(importSessionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ImportSessionStatus.EXPIRED);
    }

    @Test
    void execute_should_throw_when_current_user_inactive() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        User _user = user(userId, schoolId, UserStatus.INACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new RejectImportSessionCommand(UUID.randomUUID(), null)));
    }

    @Test
    void execute_should_throw_when_current_user_has_no_school() {
        UUID userId = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        User _user1 = user(userId, null, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user1));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new RejectImportSessionCommand(UUID.randomUUID(), null)));
    }

    @Test
    void execute_should_throw_when_school_inactive() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        User _user2 = user(userId, schoolId, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user2));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school(schoolId, false)));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new RejectImportSessionCommand(UUID.randomUUID(), null)));
    }

    @Test
    void execute_should_throw_when_command_missing_session_id() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new RejectImportSessionCommand(null, null)));
    }

    private void mockActiveContext(UUID userId, UUID schoolId) {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        User _user3 = user(userId, schoolId, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user3));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school(schoolId, true)));
    }

    private static ImportSession session(UUID id, UUID schoolId, ImportSessionStatus status, Instant expiresAt) {
        return ImportSession.builder()
            .id(id)
            .schoolId(schoolId)
            .type(ImportType.SCHOOL_CLASS)
            .fileName("classes.csv")
            .originalHeadersJson("[]")
            .suggestedMappingJson("{}")
            .confirmedMappingJson(null)
            .validRows(0L)
            .invalidRows(0L)
            .importedRows(0L)
            .skippedRows(0L)
            .totalRows(1L)
            .failureReason(null)
            .status(status)
            .importedEntityId(null)
            .expiresAt(expiresAt)
            .claimedAt(null)
            .claimedBy(null)
            .leaseExpiresAt(null)
            .attempts(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(UUID.randomUUID().toString())
            .updatedBy(UUID.randomUUID().toString())
            .build();
    }

    private User user(UUID id, UUID schoolId, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        when(schoolUserRepository.findByUserId(id)).thenReturn(
            schoolId != null ? Optional.of(SchoolUser.builder()
                .schoolId(schoolId)
                .userId(id)
                .startDate(java.time.Instant.now())
                .endDate(java.time.Instant.now().plus(36500, ChronoUnit.DAYS))
                .build()) : Optional.empty()
        );
        return user;
    }

    private static School school(UUID id, boolean active) {
        School school = new School();
        school.setId(id);
        school.setActive(active);
        return school;
    }
}
