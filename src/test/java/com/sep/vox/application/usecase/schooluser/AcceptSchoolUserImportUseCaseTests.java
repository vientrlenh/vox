package com.sep.vox.application.usecase.schooluser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import com.sep.vox.application.port.input.command.AcceptSchoolUserImportCommand;
import com.sep.vox.application.port.input.usecase.schooluser.AcceptSchoolUserImportUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.PasswordSetUpTokenPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.output.GeneratedPasswordSetUpToken;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.PasswordSetUpTokenRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.RoleCode;

class AcceptSchoolUserImportUseCaseTests {

    private ImportSessionRepository importSessionRepository;
    private ImportRowRepository importRowRepository;
    private UserRepository userRepository;
    private UserRoleRepository userRoleRepository;
    private RoleRepository roleRepository;
    private SchoolUserRepository schoolUserRepository;
    private SchoolRepository schoolRepository;
    private PasswordSetUpTokenPort passwordSetUpTokenPort;
    private PasswordSetUpTokenRepository passwordSetUpTokenRepository;
    private EventPublisherPort eventPublisherPort;
    private UserContextPort userContextPort;
    private FakeJsonSerializationPort jsonSerializationPort;
    private PlatformTransactionManager txManager;
    private AcceptSchoolUserImportUseCase useCase;

    @BeforeEach
    void setUp() {
        importSessionRepository = mock(ImportSessionRepository.class);
        importRowRepository = mock(ImportRowRepository.class);
        userRepository = mock(UserRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        roleRepository = mock(RoleRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        passwordSetUpTokenPort = mock(PasswordSetUpTokenPort.class);
        passwordSetUpTokenRepository = mock(PasswordSetUpTokenRepository.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        userContextPort = mock(UserContextPort.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        useCase = new AcceptSchoolUserImportUseCase(
            importSessionRepository,
            importRowRepository,
            userRepository,
            userRoleRepository,
            roleRepository,
            schoolUserRepository,
            schoolRepository,
            passwordSetUpTokenPort,
            passwordSetUpTokenRepository,
            eventPublisherPort,
            userContextPort,
            jsonSerializationPort,
            txManager
        );
    }

    @Test
    void execute_should_create_new_user_for_new_email() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var role = role("STUDENT");
        var savedUser = user(UUID.randomUUID(), "student@example.com");

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));
        when(importRowRepository.findBySessionIdOrderByRowNumber(sessionId))
            .thenReturn(List.of(row(sessionId, 1L, studentData("student@example.com"))));
        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of());
        when(roleRepository.findByCodeIn(Set.of("STUDENT"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(any())).thenReturn(List.of());
        when(userRepository.save(any())).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordSetUpTokenPort.generateToken()).thenReturn(new GeneratedPasswordSetUpToken("rawToken", "hashedToken"));
        when(passwordSetUpTokenRepository.save(any(PasswordSetUpToken.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = useCase.execute(command(schoolId, sessionId));

        assertThat(response.importedRows()).isEqualTo(1L);
        assertThat(response.updatedRows()).isZero();
        assertThat(response.invalidRows()).isZero();
        assertThat(response.status()).isEqualTo("COMPLETED");
        verify(userRepository).findByEmailIn(Set.of("student@example.com"));
        verify(roleRepository).findByCodeIn(Set.of("STUDENT"));
        verify(userRepository, never()).existsByEmail(any());
        verify(roleRepository, never()).findByCode(any());
        verify(userRepository).save(any(User.class));
        verify(schoolUserRepository).save(any(SchoolUser.class));
        verify(eventPublisherPort).publish(any());
    }

    @Test
    void execute_should_create_school_user_for_new_teacher() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var role = role("TEACHER");
        var savedUser = user(UUID.randomUUID(), "teacher@example.com");

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));
        when(importRowRepository.findBySessionIdOrderByRowNumber(sessionId))
            .thenReturn(List.of(row(sessionId, 1L, teacherData("teacher@example.com"))));
        when(userRepository.findByEmailIn(Set.of("teacher@example.com"))).thenReturn(List.of());
        when(roleRepository.findByCodeIn(Set.of("TEACHER"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(any())).thenReturn(List.of());
        when(userRepository.save(any())).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordSetUpTokenPort.generateToken()).thenReturn(new GeneratedPasswordSetUpToken("rawToken", "hashedToken"));
        when(passwordSetUpTokenRepository.save(any(PasswordSetUpToken.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = useCase.execute(command(schoolId, sessionId));

        assertThat(response.importedRows()).isEqualTo(1L);
        // Giáo viên mới cũng được gắn vào trường qua school_users (không thời hạn)
        verify(schoolUserRepository).save(any(SchoolUser.class));
        verify(eventPublisherPort).publish(any());
    }

    @Test
    void execute_should_update_existing_user_profile_when_email_exists() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var existingUserId = UUID.randomUUID();
        var existingUser = user(existingUserId, "teacher@example.com");
        var role = role("TEACHER");

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));
        when(importRowRepository.findBySessionIdOrderByRowNumber(sessionId))
            .thenReturn(List.of(row(sessionId, 1L, teacherData("teacher@example.com"))));
        when(userRepository.findByEmailIn(Set.of("teacher@example.com"))).thenReturn(List.of(existingUser));
        when(roleRepository.findByCodeIn(Set.of("TEACHER"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(Set.of(existingUserId))).thenReturn(List.of());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = useCase.execute(command(schoolId, sessionId));

        assertThat(response.importedRows()).isEqualTo(1L);
        assertThat(response.updatedRows()).isEqualTo(1L);
        assertThat(response.invalidRows()).isZero();
        verify(userRepository).save(existingUser);
        verify(eventPublisherPort, never()).publish(any());
        // Giáo viên chưa có trong trường giờ được gắn vào school_users (không thời hạn)
        verify(schoolUserRepository).save(any(SchoolUser.class));
    }

    @Test
    void execute_should_update_school_user_dates_when_student_already_in_school() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var existingUserId = UUID.randomUUID();
        var existingUser = user(existingUserId, "student@example.com");
        var role = role("STUDENT");
        var existingSchoolUser = new SchoolUser(
            UUID.randomUUID(), schoolId, existingUserId,
            OffsetDateTime.parse("2024-09-01T00:00:00Z"),
            OffsetDateTime.parse("2025-06-30T00:00:00Z")
        );

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));
        when(importRowRepository.findBySessionIdOrderByRowNumber(sessionId))
            .thenReturn(List.of(row(sessionId, 1L, studentData("student@example.com"))));
        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of(existingUser));
        when(roleRepository.findByCodeIn(Set.of("STUDENT"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(Set.of(existingUserId))).thenReturn(List.of(existingSchoolUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = useCase.execute(command(schoolId, sessionId));

        assertThat(response.importedRows()).isEqualTo(1L);
        assertThat(response.updatedRows()).isEqualTo(1L);
        assertThat(existingSchoolUser.getStartDate()).isEqualTo(
            OffsetDateTime.parse("2025-09-01T00:00:00Z"));
        assertThat(existingSchoolUser.getEndDate()).isEqualTo(
            OffsetDateTime.parse("2026-06-30T00:00:00Z"));
        verify(schoolUserRepository).save(existingSchoolUser);
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void execute_should_create_school_user_when_student_exists_but_not_in_school() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var existingUserId = UUID.randomUUID();
        var existingUser = user(existingUserId, "student@example.com");
        var role = role("STUDENT");

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));
        when(importRowRepository.findBySessionIdOrderByRowNumber(sessionId))
            .thenReturn(List.of(row(sessionId, 1L, studentData("student@example.com"))));
        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of(existingUser));
        when(roleRepository.findByCodeIn(Set.of("STUDENT"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(Set.of(existingUserId))).thenReturn(List.of());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = useCase.execute(command(schoolId, sessionId));

        assertThat(response.importedRows()).isEqualTo(1L);
        assertThat(response.updatedRows()).isEqualTo(1L);
        verify(schoolUserRepository).save(any(SchoolUser.class));
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void execute_should_mark_duplicate_email_within_file_as_invalid() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var role = role("STUDENT");
        var savedUser = user(UUID.randomUUID(), "student@example.com");
        var rows = List.of(
            row(sessionId, 1L, studentData("student@example.com")),
            row(sessionId, 2L, studentData("student@example.com"))
        );

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));
        when(importRowRepository.findBySessionIdOrderByRowNumber(sessionId)).thenReturn(rows);
        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of());
        when(roleRepository.findByCodeIn(Set.of("STUDENT"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(any())).thenReturn(List.of());
        when(userRepository.save(any())).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordSetUpTokenPort.generateToken()).thenReturn(new GeneratedPasswordSetUpToken("raw", "hash"));
        when(passwordSetUpTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = useCase.execute(command(schoolId, sessionId));

        assertThat(response.importedRows()).isEqualTo(1L);
        assertThat(response.updatedRows()).isZero();
        assertThat(response.invalidRows()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(1).getErrorsJson()).contains("trùng");
    }

    @Test
    void execute_should_mark_invalid_when_role_not_allowed() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var systemAdminRole = role("SYSTEM_ADMIN");
        // Role tồn tại trong DB nhưng KHÔNG thuộc {STUDENT, TEACHER} -> leo thang đặc quyền phải bị chặn
        var data = Map.of(
            "Email", "admin@example.com", "Họ tên", "Kẻ Gian", "Vai trò", "SYSTEM_ADMIN",
            "Điện thoại", "0901234599", "Ngày sinh", "1990-01-01",
            "Ngày bắt đầu", "2025-09-01", "Ngày kết thúc", "2026-06-30",
            "Địa chỉ", "Hà Nội"
        );
        var rows = List.of(row(sessionId, 1L, data));

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));
        when(importRowRepository.findBySessionIdOrderByRowNumber(sessionId)).thenReturn(rows);
        when(userRepository.findByEmailIn(Set.of("admin@example.com"))).thenReturn(List.of());
        when(roleRepository.findByCodeIn(Set.of("SYSTEM_ADMIN"))).thenReturn(List.of(systemAdminRole));
        when(schoolUserRepository.findByUserIdIn(any())).thenReturn(List.of());

        var response = useCase.execute(command(schoolId, sessionId));

        assertThat(response.importedRows()).isZero();
        assertThat(response.invalidRows()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(0).getErrorsJson()).contains("không hợp lệ");
        verify(userRepository, never()).save(any());
        verify(userRoleRepository, never()).save(any(UserRole.class));
        verify(schoolUserRepository, never()).save(any(SchoolUser.class));
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void execute_should_mark_invalid_when_role_not_found() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));
        when(importRowRepository.findBySessionIdOrderByRowNumber(sessionId))
            .thenReturn(List.of(row(sessionId, 1L, studentData("student@example.com"))));
        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of());
        when(roleRepository.findByCodeIn(Set.of("STUDENT"))).thenReturn(List.of());
        when(schoolUserRepository.findByUserIdIn(any())).thenReturn(List.of());

        var response = useCase.execute(command(schoolId, sessionId));

        assertThat(response.importedRows()).isZero();
        assertThat(response.invalidRows()).isEqualTo(1L);
        verify(userRepository, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void execute_should_handle_phone_conflict_per_row() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var role = role("STUDENT");
        var savedUser = user(UUID.randomUUID(), "success@example.com");
        var rows = List.of(
            row(sessionId, 1L, studentData("conflict@example.com")),
            row(sessionId, 2L, studentData("success@example.com"))
        );

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session(sessionId, schoolId)));
        when(importRowRepository.findBySessionIdOrderByRowNumber(sessionId)).thenReturn(rows);
        when(userRepository.findByEmailIn(Set.of("conflict@example.com", "success@example.com"))).thenReturn(List.of());
        when(roleRepository.findByCodeIn(Set.of("STUDENT"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(any())).thenReturn(List.of());
        when(userRepository.save(any()))
            .thenThrow(new DataIntegrityViolationException("phone conflict"))
            .thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordSetUpTokenPort.generateToken()).thenReturn(new GeneratedPasswordSetUpToken("raw", "hash"));
        when(passwordSetUpTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = useCase.execute(command(schoolId, sessionId));

        assertThat(response.importedRows()).isEqualTo(1L);
        assertThat(response.invalidRows()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(rows.get(0).getErrorsJson()).contains("tồn tại");
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
        school.setName("Trường Test");
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));
        when(importSessionRepository.save(any(ImportSession.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ImportRow row(UUID sessionId, long rowNumber, Map<String, String> rawData) {
        return new ImportRow(UUID.randomUUID(), sessionId, rowNumber, jsonSerializationPort.toJson(rawData), null, null, ImportRowStatus.PENDING);
    }

    private static ImportSession session(UUID id, UUID schoolId) {
        return new ImportSession(
            id, schoolId, ImportType.USER, "users.csv", "[]", "{}", null,
            0L, 0L, 0L, 0L, 2L, null, ImportSessionStatus.PREVIEWED, null,
            OffsetDateTime.now().plusDays(1), OffsetDateTime.now(), OffsetDateTime.now(),
            UUID.randomUUID(), UUID.randomUUID()
        );
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

    private static Map<String, String> studentData(String email) {
        return Map.of(
            "Email", email, "Họ tên", "Nguyễn Văn A", "Vai trò", "STUDENT",
            "Điện thoại", "0901234567", "Ngày sinh", "2000-01-01",
            "Ngày bắt đầu", "2025-09-01", "Ngày kết thúc", "2026-06-30",
            "Địa chỉ", "Hà Nội"
        );
    }

    private static Map<String, String> teacherData(String email) {
        return Map.of(
            "Email", email, "Họ tên", "Nguyễn Văn B", "Vai trò", "TEACHER",
            "Điện thoại", "0901234568", "Ngày sinh", "1990-05-15",
            "Ngày bắt đầu", "2025-09-01", "Ngày kết thúc", "2026-06-30",
            "Địa chỉ", "Hà Nội"
        );
    }

    private static Role role(String code) {
        var role = new Role();
        role.setId(UUID.randomUUID());
        role.setCode(new RoleCode(code));
        return role;
    }

    private static User user(UUID id, String email) {
        var user = new User();
        user.setId(id);
        user.setEmail(new Email(email));
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
