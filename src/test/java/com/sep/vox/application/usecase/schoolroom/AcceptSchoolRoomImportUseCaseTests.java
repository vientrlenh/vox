package com.sep.vox.application.usecase.schoolroom;

import com.sep.vox.application.usecase.TestSchoolUserRepository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.AcceptSchoolRoomImportCommand;
import com.sep.vox.application.port.input.usecase.schoolroom.AcceptSchoolRoomImportUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class AcceptSchoolRoomImportUseCaseTests {

    private ImportSessionRepository importSessionRepository;
    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private UserContextPort userContextPort;
    private FakeJsonSerializationPort jsonSerializationPort;
    private SchoolUserRepository schoolUserRepository;
    private AcceptSchoolRoomImportUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        importSessionRepository = mock(ImportSessionRepository.class);
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userContextPort = mock(UserContextPort.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        schoolUserRepository = mock(SchoolUserRepository.class);
        useCase = new AcceptSchoolRoomImportUseCase(
            importSessionRepository,
            userRepository,
            schoolRepository,
            userContextPort,
            jsonSerializationPort,
            schoolUserRepository
        );
        var currentUser = activeUser(userId, schoolId);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
    }

    @Test
    void execute_should_mark_queued_with_room_type() {
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(previewedSession()));
        when(importSessionRepository.markQueued(eq(sessionId), eq("SCHOOL_ROOM"), any(), any(), eq(userId)))
            .thenReturn(1);

        useCase.execute(new AcceptSchoolRoomImportCommand(schoolId, sessionId, validMapping()));

        verify(importSessionRepository).markQueued(eq(sessionId), eq("SCHOOL_ROOM"), any(), any(), eq(userId));
    }

    @Test
    void execute_should_throw_when_required_mapping_missing() {
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(previewedSession()));

        assertThatThrownBy(() -> useCase.execute(
                new AcceptSchoolRoomImportCommand(schoolId, sessionId, Map.of("Mã phòng", "code"))))
            .isInstanceOf(IllegalArgumentException.class);
        verify(importSessionRepository, never()).markQueued(any(), any(), any(), any(), any());
    }

    @Test
    void execute_should_throw_when_session_type_mismatch() {
        var wrongType = session(ImportType.SCHOOL_CLASS, ImportSessionStatus.PREVIEWED);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(wrongType));

        assertThatThrownBy(() -> useCase.execute(
                new AcceptSchoolRoomImportCommand(schoolId, sessionId, validMapping())))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void execute_should_throw_when_session_not_previewed() {
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(session(ImportType.SCHOOL_ROOM, ImportSessionStatus.COMPLETED)));

        assertThatThrownBy(() -> useCase.execute(
                new AcceptSchoolRoomImportCommand(schoolId, sessionId, validMapping())))
            .isInstanceOf(IllegalStateException.class);
    }

    private Map<String, String> validMapping() {
        return Map.of("Mã phòng", "code", "Tên phòng", "name");
    }

    private ImportSession previewedSession() {
        return session(ImportType.SCHOOL_ROOM, ImportSessionStatus.PREVIEWED);
    }

    private ImportSession session(ImportType type, ImportSessionStatus status) {
        return new ImportSession(
            sessionId, schoolId, type, "rooms.csv", "[]", "{}", null,
            0L, 0L, 0L, 0L, 0L, null, status, null,
            Instant.now().plus(1, ChronoUnit.DAYS), null, null, null, 0,
            Instant.now(), Instant.now(), userId, userId
        );
    }

    private User activeUser(UUID id, UUID userSchoolId) {
        var user = new User();
        user.setId(id);
        TestSchoolUserRepository.remember(id, userSchoolId);
        user.setStatus(UserStatus.ACTIVE);
        when(schoolUserRepository.findByUserId(id)).thenReturn(
            Optional.of(new SchoolUser(userSchoolId, id, Instant.now(), Instant.now().plus(36500, ChronoUnit.DAYS)))
        );
        return user;
    }

    private static School activeSchool(UUID id) {
        var school = new School();
        school.setId(id);
        school.setActive(true);
        return school;
    }
}
