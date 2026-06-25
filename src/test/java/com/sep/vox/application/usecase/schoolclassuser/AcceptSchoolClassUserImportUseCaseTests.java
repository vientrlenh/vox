package com.sep.vox.application.usecase.schoolclassuser;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.AcceptSchoolClassUserImportCommand;
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
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId, true, true);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));
        when(importSessionRepository.markQueued(any(), any(), any(), any(), any())).thenReturn(1);

        useCase.execute(command(schoolId, sessionId));

        verify(importSessionRepository).markQueued(eq(sessionId), eq("SCHOOL_CLASS_USER"), any(String.class), any(), any());
    }

    @Test
    void should_throw_when_session_not_found() {
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId, true, true);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command(schoolId, sessionId)))
                .hasMessageContaining("Không tìm thấy phiên import");
    }

    @Test
    void should_throw_when_session_type_is_not_school_class_user() {
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId, true, true);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(sessionWithType(sessionId, schoolId, ImportType.SCHOOL_CLASS)));

        assertThatThrownBy(() -> useCase.execute(command(schoolId, sessionId)))
                .hasMessageContaining("không phải là import người dùng vào lớp học");
    }

    @Test
    void should_throw_when_session_belongs_to_different_school() {
        var schoolId = UUID.randomUUID();
        var otherSchoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId, true, true);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(session(sessionId, otherSchoolId)));

        assertThatThrownBy(() -> useCase.execute(command(schoolId, sessionId)))
                .hasMessageContaining("không thuộc trường hiện tại");
    }

    @Test
    void should_expire_and_throw_when_session_is_expired() {
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var currentUserId = UUID.randomUUID();

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
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId, true, true);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(sessionWithStatus(sessionId, schoolId, ImportSessionStatus.QUEUED)));

        assertThatThrownBy(() -> useCase.execute(command(schoolId, sessionId)))
                .hasMessageContaining("không ở trạng thái cho accept");
    }

    @Test
    void should_throw_when_required_mapping_fields_are_missing() {
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var currentUserId = UUID.randomUUID();
        var incompleteMapping = Map.of("Email", "email");

        mockCurrentUserAndSchool(currentUserId, schoolId, true, true);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));

        assertThatThrownBy(() -> useCase.execute(
                new AcceptSchoolClassUserImportCommand(schoolId, sessionId, incompleteMapping)))
                .hasMessageContaining("thiếu trường bắt buộc");
    }

    @Test
    void should_throw_when_school_is_not_active() {
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId, true, false);

        assertThatThrownBy(() -> useCase.execute(command(schoolId, sessionId)))
                .hasMessageContaining("không hoạt động");
    }

    @Test
    void should_throw_when_current_user_inactive() {
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId, false, true);

        assertThatThrownBy(() -> useCase.execute(command(schoolId, sessionId)))
                .hasMessageContaining("Người dùng hiện tại không hoạt động");
    }

    @Test
    void should_throw_when_mark_queued_returns_zero() {
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId, true, true);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));
        when(importSessionRepository.markQueued(any(), any(), any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> useCase.execute(command(schoolId, sessionId)))
                .hasMessageContaining("không ở trạng thái cho accept hoặc đã hết hạn");
    }

    private void mockCurrentUserAndSchool(UUID currentUserId, UUID schoolId, boolean userActive, boolean schoolActive) {
        var user = new User();
        user.setId(currentUserId);
        user.setStatus(userActive ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(user));
        when(schoolUserRepository.findByUserId(currentUserId))
            .thenReturn(Optional.of(new SchoolUser(schoolId, currentUserId, OffsetDateTime.now(), null)));
        var school = new School();
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
        return buildSession(id, schoolId, ImportType.SCHOOL_CLASS_USER, ImportSessionStatus.PREVIEWED, OffsetDateTime.now().plusDays(1));
    }

    private static ImportSession sessionWithType(UUID id, UUID schoolId, ImportType type) {
        return buildSession(id, schoolId, type, ImportSessionStatus.PREVIEWED, OffsetDateTime.now().plusDays(1));
    }

    private static ImportSession sessionWithStatus(UUID id, UUID schoolId, ImportSessionStatus status) {
        return buildSession(id, schoolId, ImportType.SCHOOL_CLASS_USER, status, OffsetDateTime.now().plusDays(1));
    }

    private static ImportSession expiredSession(UUID id, UUID schoolId) {
        return buildSession(id, schoolId, ImportType.SCHOOL_CLASS_USER, ImportSessionStatus.PREVIEWED, OffsetDateTime.now().minusHours(1));
    }

    private static ImportSession buildSession(UUID id, UUID schoolId, ImportType type, ImportSessionStatus status, OffsetDateTime expiresAt) {
        return new ImportSession(
            id, schoolId, type, "class-users.csv", "[]", "{}", null,
            0L, 0L, 0L, 0L, 2L, null, status, null,
            expiresAt, null, null, null, 0,
            OffsetDateTime.now(), OffsetDateTime.now(),
            UUID.randomUUID(), UUID.randomUUID()
        );
    }
}
