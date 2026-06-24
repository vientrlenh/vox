package com.sep.vox.application.usecase.importfile;

import com.sep.vox.application.usecase.TestSchoolUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.RejectImportSessionCommand;
import com.sep.vox.application.port.input.usecase.importfile.RejectImportSessionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
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
        useCase = new RejectImportSessionUseCase(
            importSessionRepository,
            userRepository,
            schoolRepository,
            userContextPort,
            TestSchoolUserRepository.create()
        );
        schoolUserRepository = mock(SchoolUserRepository.class);
        useCase = new RejectImportSessionUseCase(importSessionRepository, userRepository, schoolRepository, userContextPort, schoolUserRepository);
    }

    @Test
    void execute_should_cancel_previewed_session_for_current_school() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var session = session(sessionId, schoolId, ImportSessionStatus.PREVIEWED, OffsetDateTime.now().plusDays(1));

        mockActiveContext(userId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(importSessionRepository.save(any(ImportSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.execute(new RejectImportSessionCommand(sessionId, "  User   cancelled  "));

        assertThat(response.importSessionId()).isEqualTo(sessionId);
        assertThat(response.status()).isEqualTo("CANCELLED");
        assertThat(response.reason()).isEqualTo("User cancelled");
        assertThat(session.getStatus()).isEqualTo(ImportSessionStatus.CANCELLED);
        assertThat(session.getFailureReason()).isEqualTo("User cancelled");
        assertThat(session.getUpdatedBy()).isEqualTo(userId);
        assertThat(session.getUpdatedAt()).isNotNull();
    }

    @Test
    void execute_should_store_null_reason_when_reason_blank() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var session = session(sessionId, schoolId, ImportSessionStatus.PREVIEWED, OffsetDateTime.now().plusDays(1));

        mockActiveContext(userId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(importSessionRepository.save(any(ImportSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.execute(new RejectImportSessionCommand(sessionId, " "));

        assertThat(response.reason()).isNull();
        assertThat(session.getFailureReason()).isNull();
    }

    @Test
    void execute_should_throw_when_session_belongs_to_other_school() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();

        mockActiveContext(userId, schoolId);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(session(sessionId, UUID.randomUUID(), ImportSessionStatus.PREVIEWED, OffsetDateTime.now().plusDays(1))));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new RejectImportSessionCommand(sessionId, null)));
    }

    @Test
    void execute_should_throw_when_session_not_found() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();

        mockActiveContext(userId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(new RejectImportSessionCommand(sessionId, null)));
    }

    @Test
    void execute_should_throw_when_session_not_previewed() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();

        mockActiveContext(userId, schoolId);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(session(sessionId, schoolId, ImportSessionStatus.COMPLETED, OffsetDateTime.now().plusDays(1))));

        assertThrows(IllegalStateException.class, () -> useCase.execute(new RejectImportSessionCommand(sessionId, null)));
    }

    @Test
    void execute_should_mark_expired_session_and_throw() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var session = session(sessionId, schoolId, ImportSessionStatus.PREVIEWED, OffsetDateTime.now().minusMinutes(1));

        mockActiveContext(userId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(importSessionRepository.save(any(ImportSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(IllegalStateException.class, () -> useCase.execute(new RejectImportSessionCommand(sessionId, null)));

        var captor = ArgumentCaptor.forClass(ImportSession.class);
        verify(importSessionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ImportSessionStatus.EXPIRED);
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(userId);
    }

    @Test
    void execute_should_throw_when_current_user_inactive() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        var _user = user(userId, schoolId, UserStatus.INACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user));

        assertThrows(IllegalStateException.class, () -> useCase.execute(new RejectImportSessionCommand(UUID.randomUUID(), null)));
    }

    @Test
    void execute_should_throw_when_current_user_has_no_school() {
        var userId = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        var _user1 = user(userId, null, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user1));

        assertThrows(IllegalStateException.class, () -> useCase.execute(new RejectImportSessionCommand(UUID.randomUUID(), null)));
    }

    @Test
    void execute_should_throw_when_school_inactive() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        var _user2 = user(userId, schoolId, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user2));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school(schoolId, false)));

        assertThrows(IllegalStateException.class, () -> useCase.execute(new RejectImportSessionCommand(UUID.randomUUID(), null)));
    }

    @Test
    void execute_should_throw_when_command_missing_session_id() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new RejectImportSessionCommand(null, null)));
    }

    private void mockActiveContext(UUID userId, UUID schoolId) {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        var _user3 = user(userId, schoolId, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user3));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school(schoolId, true)));
    }

    private static ImportSession session(UUID id, UUID schoolId, ImportSessionStatus status, OffsetDateTime expiresAt) {
        return new ImportSession(
            id,
            schoolId,
            ImportType.SCHOOL_CLASS,
            "classes.csv",
            "[]",
            "{}",
            null,
            0L,
            0L,
            0L,
            0L,
            1L,
            null,
            status,
            null,
            expiresAt, 
            null, 
            null, 
            null, 
            0, 
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }

    private User user(UUID id, UUID schoolId, UserStatus status) {
        var user = new User();
        user.setId(id);
        TestSchoolUserRepository.remember(id, schoolId);
        user.setStatus(status);
        when(schoolUserRepository.findByUserId(id)).thenReturn(
            schoolId != null ? Optional.of(new SchoolUser(schoolId, id, java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now().plusYears(100))) : Optional.empty()
        );
        return user;
    }

    private static School school(UUID id, boolean active) {
        var school = new School();
        school.setId(id);
        school.setActive(active);
        return school;
    }
}
