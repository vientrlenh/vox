package com.sep.vox.application.usecase.schoolclassuser;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.schoolclassuser.AcceptSchoolClassUserImportCommand;
import com.sep.vox.application.port.input.usecase.schoolclassuser.AcceptSchoolClassUserImportUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
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

class AcceptSchoolClassUserImportUseCaseTests {

    private ImportSessionRepository importSessionRepository;
    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private UserContextPort userContextPort;
    private JsonSerializationPort jsonSerializationPort;
    private SchoolUserRepository schoolUserRepository;
    private AcceptSchoolClassUserImportUseCase useCase;

    @BeforeEach
    void setUp() {
        importSessionRepository = mock(ImportSessionRepository.class);
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userContextPort = mock(UserContextPort.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        schoolUserRepository = mock(SchoolUserRepository.class);
        useCase = new AcceptSchoolClassUserImportUseCase(
            importSessionRepository,
            userRepository,
            schoolRepository,
            userContextPort,
            jsonSerializationPort,
            schoolUserRepository
        );
    }

    @Test
    void should_mark_session_as_queued_successfully() {
        UUID schoolId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId, true, true);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));
        when(importSessionRepository.markQueued(any(), any(), any(), any(), any())).thenReturn(1);

        useCase.execute(command(schoolId, sessionId));

        verify(importSessionRepository).markQueued(eq(sessionId), eq("SCHOOL_CLASS_USER"), any(String.class), any(), any());
    }

    @Test
    void should_throw_when_session_not_found() {
        UUID schoolId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId, true, true);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command(schoolId, sessionId)))
                .hasMessageContaining("Không tìm thấy phiên import");
    }

    @Test
    void should_throw_when_session_type_is_not_school_class_user() {
        UUID schoolId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId, true, true);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(sessionWithType(sessionId, schoolId, ImportType.SCHOOL_CLASS)));

        assertThatThrownBy(() -> useCase.execute(command(schoolId, sessionId)))
                .hasMessageContaining("không phải là import người dùng vào lớp học");
    }

    @Test
    void should_throw_when_session_belongs_to_different_school() {
        UUID schoolId = UUID.randomUUID();
        UUID otherSchoolId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId, true, true);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(session(sessionId, otherSchoolId)));

        assertThatThrownBy(() -> useCase.execute(command(schoolId, sessionId)))
                .hasMessageContaining("không thuộc trường hiện tại");
    }

    @Test
    void should_expire_and_throw_when_session_is_expired() {
        UUID schoolId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId, true, true);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(expiredSession(sessionId, schoolId)));
        when(importSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> useCase.execute(command(schoolId, sessionId)))
                .hasMessageContaining("đã hết hạn");
        verify(importSessionRepository).save(any(ImportSession.class));
    }

    @Test
    void should_throw_when_session_status_is_not_previewed() {
        UUID schoolId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId, true, true);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(sessionWithStatus(sessionId, schoolId, ImportSessionStatus.QUEUED)));

        assertThatThrownBy(() -> useCase.execute(command(schoolId, sessionId)))
                .hasMessageContaining("không ở trạng thái cho accept");
    }

    @Test
    void should_throw_when_required_mapping_fields_are_missing() {
        UUID schoolId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        Map<String, String> incompleteMapping = Map.of("Email", "email");

        mockCurrentUserAndSchool(currentUserId, schoolId, true, true);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));

        assertThatThrownBy(() -> useCase.execute(
                new AcceptSchoolClassUserImportCommand(schoolId, sessionId, incompleteMapping)))
                .hasMessageContaining("thiếu trường bắt buộc");
    }

    @Test
    void should_throw_when_school_is_not_active() {
        UUID schoolId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId, true, false);

        assertThatThrownBy(() -> useCase.execute(command(schoolId, sessionId)))
                .hasMessageContaining("không hoạt động");
    }

    @Test
    void should_throw_when_current_user_inactive() {
        UUID schoolId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId, false, true);

        assertThatThrownBy(() -> useCase.execute(command(schoolId, sessionId)))
                .hasMessageContaining("Người dùng hiện tại không hoạt động");
    }

    @Test
    void should_throw_when_mark_queued_returns_zero() {
        UUID schoolId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId, true, true);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));
        when(importSessionRepository.markQueued(any(), any(), any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> useCase.execute(command(schoolId, sessionId)))
                .hasMessageContaining("không ở trạng thái cho accept hoặc đã hết hạn");
    }

    private void mockCurrentUserAndSchool(UUID currentUserId, UUID schoolId, boolean userActive, boolean schoolActive) {
        User user = new User();
        user.setId(currentUserId);
        user.setStatus(userActive ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(user));
        when(schoolUserRepository.findByUserId(currentUserId))
            .thenReturn(Optional.of(SchoolUser.builder()
                .schoolId(schoolId)
                .userId(currentUserId)
                .startDate(Instant.now())
                .endDate(null)
                .build()));
        School school = new School();
        school.setId(schoolId);
        school.setActive(schoolActive);
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));
    }

    private static AcceptSchoolClassUserImportCommand command(UUID schoolId, UUID sessionId) {
        return new AcceptSchoolClassUserImportCommand(schoolId, sessionId, mapping());
    }

    private static Map<String, String> mapping() {
        return Map.of("Email", "email", "Mã lớp", "classCode");
    }

    private static ImportSession session(UUID id, UUID schoolId) {
        return buildSession(id, schoolId, ImportType.SCHOOL_CLASS_USER, ImportSessionStatus.PREVIEWED, Instant.now().plus(1, ChronoUnit.DAYS));
    }

    private static ImportSession sessionWithType(UUID id, UUID schoolId, ImportType type) {
        return buildSession(id, schoolId, type, ImportSessionStatus.PREVIEWED, Instant.now().plus(1, ChronoUnit.DAYS));
    }

    private static ImportSession sessionWithStatus(UUID id, UUID schoolId, ImportSessionStatus status) {
        return buildSession(id, schoolId, ImportType.SCHOOL_CLASS_USER, status, Instant.now().plus(1, ChronoUnit.DAYS));
    }

    private static ImportSession expiredSession(UUID id, UUID schoolId) {
        return buildSession(id, schoolId, ImportType.SCHOOL_CLASS_USER, ImportSessionStatus.PREVIEWED, Instant.now().minus(1, ChronoUnit.HOURS));
    }

    private static ImportSession buildSession(UUID id, UUID schoolId, ImportType type, ImportSessionStatus status, Instant expiresAt) {
        return ImportSession.builder()
            .id(id)
            .schoolId(schoolId)
            .type(type)
            .fileName("class-users.csv")
            .originalHeadersJson("[]")
            .suggestedMappingJson("{}")
            .confirmedMappingJson(null)
            .validRows(0L)
            .invalidRows(0L)
            .importedRows(0L)
            .skippedRows(0L)
            .totalRows(2L)
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
}
