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

import com.sep.vox.application.port.input.service.ImportCommitResult;
import com.sep.vox.application.port.input.service.SchoolUserImportCommitHandler;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.PasswordSetUpTokenPort;
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
import com.sep.vox.domain.repository.PasswordSetUpTokenRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.RoleCode;

class SchoolUserImportCommitHandlerTests {

    private UserRepository userRepository;
    private UserRoleRepository userRoleRepository;
    private RoleRepository roleRepository;
    private SchoolUserRepository schoolUserRepository;
    private SchoolRepository schoolRepository;
    private PasswordSetUpTokenPort passwordSetUpTokenPort;
    private PasswordSetUpTokenRepository passwordSetUpTokenRepository;
    private EventPublisherPort eventPublisherPort;
    private FakeJsonSerializationPort jsonSerializationPort;
    private PlatformTransactionManager txManager;
    private SchoolUserImportCommitHandler handler;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        roleRepository = mock(RoleRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        passwordSetUpTokenPort = mock(PasswordSetUpTokenPort.class);
        passwordSetUpTokenRepository = mock(PasswordSetUpTokenRepository.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        handler = new SchoolUserImportCommitHandler(
            userRepository,
            userRoleRepository,
            roleRepository,
            schoolUserRepository,
            schoolRepository,
            passwordSetUpTokenPort,
            passwordSetUpTokenRepository,
            eventPublisherPort,
            jsonSerializationPort,
            txManager
        );
    }

    @Test
    void should_create_new_user_for_new_email() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var role = role("STUDENT");
        var savedUser = user(UUID.randomUUID(), "student@example.com");
        var rows = List.of(row(sessionId, 1L, studentData("student@example.com")));

        mockSchool(schoolId);
        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of());
        when(roleRepository.findByCodeIn(Set.of("STUDENT"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(any())).thenReturn(List.of());
        when(userRepository.save(any())).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordSetUpTokenPort.generateToken()).thenReturn(new GeneratedPasswordSetUpToken("rawToken", "hashedToken"));
        when(passwordSetUpTokenRepository.save(any(PasswordSetUpToken.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.updated()).isZero();
        assertThat(result.invalid()).isZero();
        verify(userRepository).save(any(User.class));
        verify(schoolUserRepository).save(any(SchoolUser.class));
        verify(eventPublisherPort).publish(any());
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
    }

    @Test
    void should_create_school_user_for_new_teacher() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var role = role("TEACHER");
        var savedUser = user(UUID.randomUUID(), "teacher@example.com");
        var rows = List.of(row(sessionId, 1L, teacherData("teacher@example.com")));

        mockSchool(schoolId);
        when(userRepository.findByEmailIn(Set.of("teacher@example.com"))).thenReturn(List.of());
        when(roleRepository.findByCodeIn(Set.of("TEACHER"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(any())).thenReturn(List.of());
        when(userRepository.save(any())).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordSetUpTokenPort.generateToken()).thenReturn(new GeneratedPasswordSetUpToken("rawToken", "hashedToken"));
        when(passwordSetUpTokenRepository.save(any(PasswordSetUpToken.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        verify(schoolUserRepository).save(any(SchoolUser.class));
        verify(eventPublisherPort).publish(any());
    }

    @Test
    void should_update_existing_user_profile_when_email_exists() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var existingUserId = UUID.randomUUID();
        var existingUser = user(existingUserId, "teacher@example.com");
        var role = role("TEACHER");
        var rows = List.of(row(sessionId, 1L, teacherData("teacher@example.com")));

        mockSchool(schoolId);
        when(userRepository.findByEmailIn(Set.of("teacher@example.com"))).thenReturn(List.of(existingUser));
        when(roleRepository.findByCodeIn(Set.of("TEACHER"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(Set.of(existingUserId))).thenReturn(List.of());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isZero();
        assertThat(result.updated()).isEqualTo(1L);
        assertThat(result.invalid()).isZero();
        verify(userRepository).save(existingUser);
        verify(eventPublisherPort, never()).publish(any());
        verify(schoolUserRepository).save(any(SchoolUser.class));
    }

    @Test
    void should_update_school_user_dates_when_student_already_in_school() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var existingUserId = UUID.randomUUID();
        var existingUser = user(existingUserId, "student@example.com");
        var role = role("STUDENT");
        var existingSchoolUser = new SchoolUser(
            UUID.randomUUID(), schoolId, existingUserId,
            OffsetDateTime.parse("2024-09-01T00:00:00Z"),
            OffsetDateTime.parse("2025-06-30T00:00:00Z")
        );
        var rows = List.of(row(sessionId, 1L, studentData("student@example.com")));

        mockSchool(schoolId);
        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of(existingUser));
        when(roleRepository.findByCodeIn(Set.of("STUDENT"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(Set.of(existingUserId))).thenReturn(List.of(existingSchoolUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isZero();
        assertThat(result.updated()).isEqualTo(1L);
        assertThat(existingSchoolUser.getStartDate()).isEqualTo(OffsetDateTime.parse("2025-09-01T00:00:00Z"));
        assertThat(existingSchoolUser.getEndDate()).isEqualTo(OffsetDateTime.parse("2026-06-30T00:00:00Z"));
        verify(schoolUserRepository).save(existingSchoolUser);
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void should_create_school_user_when_student_exists_but_not_in_school() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var existingUserId = UUID.randomUUID();
        var existingUser = user(existingUserId, "student@example.com");
        var role = role("STUDENT");
        var rows = List.of(row(sessionId, 1L, studentData("student@example.com")));

        mockSchool(schoolId);
        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of(existingUser));
        when(roleRepository.findByCodeIn(Set.of("STUDENT"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(Set.of(existingUserId))).thenReturn(List.of());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isZero();
        assertThat(result.updated()).isEqualTo(1L);
        verify(schoolUserRepository).save(any(SchoolUser.class));
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void should_mark_invalid_when_email_belongs_to_user_in_another_school() {
        var schoolId = UUID.randomUUID();
        var otherSchoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var existingUserId = UUID.randomUUID();
        var existingUser = user(existingUserId, "student@example.com");
        var role = role("STUDENT");
        var otherSchoolUser = new SchoolUser(
            UUID.randomUUID(), otherSchoolId, existingUserId,
            OffsetDateTime.parse("2024-09-01T00:00:00Z"),
            OffsetDateTime.parse("2025-06-30T00:00:00Z")
        );
        var rows = List.of(row(sessionId, 1L, studentData("student@example.com")));

        mockSchool(schoolId);
        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of(existingUser));
        when(roleRepository.findByCodeIn(Set.of("STUDENT"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(Set.of(existingUserId))).thenReturn(List.of(otherSchoolUser));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        // A user already belonging to another school must NOT be overwritten or attached
        // into the importer's school (1 user = 1 school).
        assertThat(result.created()).isZero();
        assertThat(result.updated()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(0).getErrorsJson()).contains("trường khác");
        verify(userRepository, never()).save(any());
        verify(schoolUserRepository, never()).save(any(SchoolUser.class));
        assertThat(otherSchoolUser.getStartDate()).isEqualTo(OffsetDateTime.parse("2024-09-01T00:00:00Z"));
        assertThat(otherSchoolUser.getEndDate()).isEqualTo(OffsetDateTime.parse("2025-06-30T00:00:00Z"));
    }

    @Test
    void should_not_require_dates_for_teacher_rows() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var role = role("TEACHER");
        var savedUser = user(UUID.randomUUID(), "teacher@example.com");
        var teacherWithoutDates = Map.of(
            "Email", "teacher@example.com", "Họ tên", "Nguyễn Văn B", "Vai trò", "TEACHER",
            "Điện thoại", "0901234568", "Ngày sinh", "1990-05-15",
            "Địa chỉ", "Hà Nội"
        );
        var rows = List.of(row(sessionId, 1L, teacherWithoutDates));

        mockSchool(schoolId);
        when(userRepository.findByEmailIn(Set.of("teacher@example.com"))).thenReturn(List.of());
        when(roleRepository.findByCodeIn(Set.of("TEACHER"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(any())).thenReturn(List.of());
        when(userRepository.save(any())).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordSetUpTokenPort.generateToken()).thenReturn(new GeneratedPasswordSetUpToken("raw", "hash"));
        when(passwordSetUpTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.invalid()).isZero();
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
    }

    @Test
    void should_mark_invalid_when_student_start_date_not_before_end_date() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var role = role("STUDENT");
        var badDates = Map.of(
            "Email", "student@example.com", "Họ tên", "Nguyễn Văn A", "Vai trò", "STUDENT",
            "Điện thoại", "0901234567", "Ngày sinh", "2000-01-01",
            "Ngày bắt đầu", "2026-06-30", "Ngày kết thúc", "2026-06-30",
            "Địa chỉ", "Hà Nội"
        );
        var rows = List.of(row(sessionId, 1L, badDates));

        mockSchool(schoolId);
        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of());
        when(roleRepository.findByCodeIn(Set.of("STUDENT"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(any())).thenReturn(List.of());

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        verify(userRepository, never()).save(any());
    }

    @Test
    void should_mark_duplicate_email_within_file_as_invalid() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var role = role("STUDENT");
        var savedUser = user(UUID.randomUUID(), "student@example.com");
        var rows = List.of(
            row(sessionId, 1L, studentData("student@example.com")),
            row(sessionId, 2L, studentData("student@example.com"))
        );

        mockSchool(schoolId);
        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of());
        when(roleRepository.findByCodeIn(Set.of("STUDENT"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(any())).thenReturn(List.of());
        when(userRepository.save(any())).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordSetUpTokenPort.generateToken()).thenReturn(new GeneratedPasswordSetUpToken("raw", "hash"));
        when(passwordSetUpTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.updated()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(1).getErrorsJson()).contains("trùng");
    }

    @Test
    void should_mark_invalid_when_role_not_allowed() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var systemAdminRole = role("SYSTEM_ADMIN");
        var data = Map.of(
            "Email", "admin@example.com", "Họ tên", "Kẻ Gian", "Vai trò", "SYSTEM_ADMIN",
            "Điện thoại", "0901234599", "Ngày sinh", "1990-01-01",
            "Ngày bắt đầu", "2025-09-01", "Ngày kết thúc", "2026-06-30",
            "Địa chỉ", "Hà Nội"
        );
        var rows = List.of(row(sessionId, 1L, data));

        mockSchool(schoolId);
        when(userRepository.findByEmailIn(Set.of("admin@example.com"))).thenReturn(List.of());
        when(roleRepository.findByCodeIn(Set.of("SYSTEM_ADMIN"))).thenReturn(List.of(systemAdminRole));
        when(schoolUserRepository.findByUserIdIn(any())).thenReturn(List.of());

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(0).getErrorsJson()).contains("không hợp lệ");
        verify(userRepository, never()).save(any());
        verify(userRoleRepository, never()).save(any(UserRole.class));
        verify(schoolUserRepository, never()).save(any(SchoolUser.class));
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void should_handle_phone_conflict_per_row() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var role = role("STUDENT");
        var savedUser = user(UUID.randomUUID(), "success@example.com");
        var rows = List.of(
            row(sessionId, 1L, studentData("conflict@example.com")),
            row(sessionId, 2L, studentData("success@example.com"))
        );

        mockSchool(schoolId);
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

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.FAILED);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(rows.get(0).getErrorsJson()).contains("tồn tại");
    }

    @Test
    void should_isolate_invalid_value_object_per_row() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var role = role("STUDENT");
        var savedUser = user(UUID.randomUUID(), "good@example.com");
        var badPhoneData = Map.of(
            "Email", "bad@example.com", "Họ tên", "Nguyễn Văn A", "Vai trò", "STUDENT",
            "Điện thoại", "123", "Ngày sinh", "2000-01-01",
            "Ngày bắt đầu", "2025-09-01", "Ngày kết thúc", "2026-06-30",
            "Địa chỉ", "Hà Nội"
        );
        var rows = List.of(
            row(sessionId, 1L, badPhoneData),
            row(sessionId, 2L, studentData("good@example.com"))
        );

        mockSchool(schoolId);
        when(userRepository.findByEmailIn(Set.of("bad@example.com", "good@example.com"))).thenReturn(List.of());
        when(roleRepository.findByCodeIn(Set.of("STUDENT"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(any())).thenReturn(List.of());
        when(userRepository.save(any())).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordSetUpTokenPort.generateToken()).thenReturn(new GeneratedPasswordSetUpToken("raw", "hash"));
        when(passwordSetUpTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.FAILED);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
    }

    private void mockSchool(UUID schoolId) {
        var school = new School();
        school.setId(schoolId);
        school.setName("Trường Test");
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));
    }

    private ImportSession session(UUID id, UUID schoolId, UUID createdBy) {
        return new ImportSession(
            id, schoolId, ImportType.USER, "users.csv", "[]", "{}",
            jsonSerializationPort.toJson(mapping()),
            0L, 0L, 0L, 0L, 0L, null, ImportSessionStatus.IMPORTING, null,
            OffsetDateTime.now().plusDays(1), null, null, null, 0,
            OffsetDateTime.now(), OffsetDateTime.now(),
            createdBy, createdBy
        );
    }

    private ImportRow row(UUID sessionId, long rowNumber, Map<String, String> rawData) {
        return new ImportRow(UUID.randomUUID(), sessionId, rowNumber, jsonSerializationPort.toJson(rawData), null, null, ImportRowStatus.PENDING);
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
