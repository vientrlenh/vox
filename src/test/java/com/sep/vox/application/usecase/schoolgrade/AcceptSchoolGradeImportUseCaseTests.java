package com.sep.vox.application.usecase.schoolgrade;

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

import com.sep.vox.application.port.input.command.schoolgrade.AcceptSchoolGradeImportCommand;
import com.sep.vox.application.port.input.usecase.schoolgrade.AcceptSchoolGradeImportUseCase;
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

class AcceptSchoolGradeImportUseCaseTests {

    private ImportSessionRepository importSessionRepository;
    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private UserContextPort userContextPort;
    private FakeJsonSerializationPort jsonSerializationPort;
    private SchoolUserRepository schoolUserRepository;
    private AcceptSchoolGradeImportUseCase useCase;

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
        useCase = new AcceptSchoolGradeImportUseCase(
            importSessionRepository,
            userRepository,
            schoolRepository,
            userContextPort,
            jsonSerializationPort,
            schoolUserRepository
        );
        User currentUser = activeUser(userId, schoolId);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
    }

    @Test
    void execute_should_mark_queued_with_grade_type() {
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(previewedSession()));
        when(importSessionRepository.markQueued(eq(sessionId), eq("SCHOOL_GRADE"), any(), any()))
            .thenReturn(1);

        useCase.execute(new AcceptSchoolGradeImportCommand(schoolId, sessionId, validMapping()));

        verify(importSessionRepository).markQueued(eq(sessionId), eq("SCHOOL_GRADE"), any(), any());
    }

    @Test
    void execute_should_throw_when_required_mapping_missing() {
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(previewedSession()));

        assertThatThrownBy(() -> useCase.execute(
                new AcceptSchoolGradeImportCommand(schoolId, sessionId, Map.of("Mã năm học", "code"))))
            .isInstanceOf(IllegalArgumentException.class);
        verify(importSessionRepository, never()).markQueued(any(), any(), any(), any());
    }

    @Test
    void execute_should_throw_when_session_type_mismatch() {
        ImportSession wrongType = session(ImportType.SCHOOL_CLASS, ImportSessionStatus.PREVIEWED);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(wrongType));

        assertThatThrownBy(() -> useCase.execute(
                new AcceptSchoolGradeImportCommand(schoolId, sessionId, validMapping())))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void execute_should_throw_when_session_not_previewed() {
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(session(ImportType.SCHOOL_GRADE, ImportSessionStatus.COMPLETED)));

        assertThatThrownBy(() -> useCase.execute(
                new AcceptSchoolGradeImportCommand(schoolId, sessionId, validMapping())))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private Map<String, String> validMapping() {
        return Map.of(
            "Mã khối", "schoolGradeLevelCode",
            "Mã năm học", "code",
            "Tên", "name",
            "Bắt đầu", "startDate",
            "Kết thúc", "endDate");
    }

    private ImportSession previewedSession() {
        return session(ImportType.SCHOOL_GRADE, ImportSessionStatus.PREVIEWED);
    }

    private ImportSession session(ImportType type, ImportSessionStatus status) {
        return ImportSession.builder()
            .id(sessionId)
            .schoolId(schoolId)
            .type(type)
            .fileName("grades.csv")
            .originalHeadersJson("[]")
            .suggestedMappingJson("{}")
            .confirmedMappingJson(null)
            .validRows(0L)
            .invalidRows(0L)
            .importedRows(0L)
            .skippedRows(0L)
            .totalRows(0L)
            .failureReason(null)
            .status(status)
            .importedEntityId(null)
            .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
            .claimedAt(null)
            .claimedBy(null)
            .leaseExpiresAt(null)
            .attempts(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(userId.toString())
            .updatedBy(userId.toString())
            .build();
    }

    private User activeUser(UUID id, UUID userSchoolId) {
        User user = new User();
        user.setId(id);
        user.setStatus(UserStatus.ACTIVE);
        when(schoolUserRepository.findByUserId(id)).thenReturn(
            Optional.of(SchoolUser.builder()
                .schoolId(userSchoolId)
                .userId(id)
                .startDate(Instant.now())
                .endDate(Instant.now().plus(36500, ChronoUnit.DAYS))
                .build())
        );
        return user;
    }

    private static School activeSchool(UUID id) {
        School school = new School();
        school.setId(id);
        school.setActive(true);
        return school;
    }
}
