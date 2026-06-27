package com.sep.vox.application.usecase.schooluser;

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

import com.sep.vox.application.port.input.command.AcceptSchoolUserImportCommand;
import com.sep.vox.application.port.input.usecase.schooluser.AcceptSchoolUserImportUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class AcceptSchoolUserImportUseCaseTests {

    private ImportSessionRepository importSessionRepository;
    private UserContextPort userContextPort;
    private JsonSerializationPort jsonSerializationPort;
    private UserRepository userRepository;
    private SchoolUserRepository schoolUserRepository;
    private SchoolRepository schoolRepository;
    private AcceptSchoolUserImportUseCase useCase;

    @BeforeEach
    void setUp() {
        importSessionRepository = mock(ImportSessionRepository.class);
        userContextPort = mock(UserContextPort.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        userRepository = mock(UserRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        useCase = new AcceptSchoolUserImportUseCase(
            importSessionRepository,
            userContextPort,
            jsonSerializationPort,
            userRepository,
            schoolUserRepository,
            schoolRepository
        );
    }

    @Test
    void should_mark_session_as_queued_successfully() {
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));
        when(importSessionRepository.markQueued(any(), any(), any(), any(), any())).thenReturn(1);

        useCase.execute(command(schoolId, sessionId));

        verify(importSessionRepository).markQueued(eq(sessionId), eq("USER"), any(String.class), any(), any());
    }

    @Test
    void should_throw_when_session_not_found() {
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command(schoolId, sessionId)))
                .hasMessageContaining("Không tìm thấy phiên import");
    }

    @Test
    void should_throw_when_session_type_is_not_user() {
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(sessionWithType(sessionId, schoolId, ImportType.SCHOOL_DIRECTORY)));

        assertThatThrownBy(() -> useCase.execute(command(schoolId, sessionId)))
                .hasMessageContaining("không phải là import người dùng");
    }

    @Test
    void should_throw_when_session_belongs_to_different_school() {
        var schoolId = UUID.randomUUID();
        var otherSchoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId);
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

        mockCurrentUserAndSchool(currentUserId, schoolId);
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

        mockCurrentUserAndSchool(currentUserId, schoolId);
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

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));

        assertThatThrownBy(() -> useCase.execute(
                new AcceptSchoolUserImportCommand(schoolId, sessionId, incompleteMapping)))
                .hasMessageContaining("thiếu trường bắt buộc");
    }

    @Test
    void should_throw_when_school_is_not_active() {
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var currentUserId = UUID.randomUUID();

        var admin = new User();
        admin.setId(currentUserId);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(admin));
        when(schoolUserRepository.findByUserId(currentUserId))
            .thenReturn(Optional.of(new SchoolUser(schoolId, currentUserId, OffsetDateTime.now(), null)));
        var inactiveSchool = new School();
        inactiveSchool.setId(schoolId);
        inactiveSchool.setActive(false);
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(inactiveSchool));

        assertThatThrownBy(() -> useCase.execute(command(schoolId, sessionId)))
                .hasMessageContaining("không hoạt động");
    }

    @Test
    void should_throw_when_mark_queued_returns_zero() {
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var currentUserId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));
        when(importSessionRepository.markQueued(any(), any(), any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> useCase.execute(command(schoolId, sessionId)))
                .hasMessageContaining("không ở trạng thái cho accept hoặc đã hết hạn");
    }

    private void mockCurrentUserAndSchool(UUID currentUserId, UUID schoolId) {
        var admin = new User();
        admin.setId(currentUserId);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(admin));
        when(schoolUserRepository.findByUserId(currentUserId))
            .thenReturn(Optional.of(new SchoolUser(schoolId, currentUserId, OffsetDateTime.now(), null)));
        var school = new School();
        school.setId(schoolId);
        school.setActive(true);
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));
    }

    private static AcceptSchoolUserImportCommand command(UUID schoolId, UUID sessionId) {
        return new AcceptSchoolUserImportCommand(schoolId, sessionId, mapping());
    }

    private static Map<String, String> mapping() {
        return Map.of(
            "Email", "email", "Họ tên", "fullName", "Vai trò", "roleCode",
            "Điện thoại", "phone", "Ngày sinh", "dateOfBirth", "Ngày bắt đầu", "startDate",
            "Ngày kết thúc", "endDate", "Địa chỉ", "address"
        );
    }

    private static ImportSession session(UUID id, UUID schoolId) {
        return new ImportSession(
            id, schoolId, ImportType.USER, "users.csv", "[]", "{}", null,
            0L, 0L, 0L, 0L, 2L, null, ImportSessionStatus.PREVIEWED, null,
            OffsetDateTime.now().plusDays(1), null, null, null, 0,
            OffsetDateTime.now(), OffsetDateTime.now(),
            UUID.randomUUID(), UUID.randomUUID()
        );
    }

    private static ImportSession sessionWithType(UUID id, UUID schoolId, ImportType type) {
        return new ImportSession(
            id, schoolId, type, "users.csv", "[]", "{}", null,
            0L, 0L, 0L, 0L, 2L, null, ImportSessionStatus.PREVIEWED, null,
            OffsetDateTime.now().plusDays(1), null, null, null, 0,
            OffsetDateTime.now(), OffsetDateTime.now(),
            UUID.randomUUID(), UUID.randomUUID()
        );
    }

    private static ImportSession sessionWithStatus(UUID id, UUID schoolId, ImportSessionStatus status) {
        return new ImportSession(
            id, schoolId, ImportType.USER, "users.csv", "[]", "{}", null,
            0L, 0L, 0L, 0L, 2L, null, status, null,
            OffsetDateTime.now().plusDays(1), null, null, null, 0,
            OffsetDateTime.now(), OffsetDateTime.now(),
            UUID.randomUUID(), UUID.randomUUID()
        );
    }

    private static ImportSession expiredSession(UUID id, UUID schoolId) {
        return new ImportSession(
            id, schoolId, ImportType.USER, "users.csv", "[]", "{}", null,
            0L, 0L, 0L, 0L, 2L, null, ImportSessionStatus.PREVIEWED, null,
            OffsetDateTime.now().minusHours(1), null, null, null, 0,
            OffsetDateTime.now(), OffsetDateTime.now(),
            UUID.randomUUID(), UUID.randomUUID()
        );
    }
}
