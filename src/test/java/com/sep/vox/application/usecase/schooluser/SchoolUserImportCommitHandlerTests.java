package com.sep.vox.application.usecase.schooluser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sep.vox.application.exception.DataConflictException;

import com.sep.vox.application.port.output.RequiresNewTransactionPort;
import com.sep.vox.application.port.input.service.ImportCommitResult;
import com.sep.vox.application.port.input.service.SchoolUserImportCommitHandler;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.OutboxRepository;
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
    private OutboxRepository outboxRepository;
    private FakeJsonSerializationPort jsonSerializationPort;
    private RequiresNewTransactionPort requiresNewTransactionPort;
    private SchoolUserImportCommitHandler handler;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        roleRepository = mock(RoleRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        outboxRepository = mock(OutboxRepository.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        requiresNewTransactionPort = mock(RequiresNewTransactionPort.class);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(requiresNewTransactionPort).runInNewTransaction(any());
        handler = new SchoolUserImportCommitHandler(
            userRepository,
            userRoleRepository,
            roleRepository,
            schoolUserRepository,
            schoolRepository,
            outboxRepository,
            jsonSerializationPort,
            requiresNewTransactionPort
        );
    }

    @Test
    void should_create_new_user_for_new_email() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Role role = role("STUDENT");
        User savedUser = user(UUID.randomUUID(), "student@example.com");
        List<ImportRow> rows = List.of(row(sessionId, 1L, studentData("student@example.com")));

        mockSchool(schoolId);
        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of());
        when(roleRepository.findByCodeIn(Set.of("STUDENT"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(any())).thenReturn(List.of());
        when(userRepository.save(any())).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.updated()).isZero();
        assertThat(result.invalid()).isZero();
        verify(userRepository).save(any(User.class));
        verify(schoolUserRepository).save(any(SchoolUser.class));
        verify(outboxRepository).save(any(Outbox.class));
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
    }

    @Test
    void should_create_school_user_for_new_teacher() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Role role = role("TEACHER");
        User savedUser = user(UUID.randomUUID(), "teacher@example.com");
        List<ImportRow> rows = List.of(row(sessionId, 1L, teacherData("teacher@example.com")));

        mockSchool(schoolId);
        when(userRepository.findByEmailIn(Set.of("teacher@example.com"))).thenReturn(List.of());
        when(roleRepository.findByCodeIn(Set.of("TEACHER"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(any())).thenReturn(List.of());
        when(userRepository.save(any())).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        verify(schoolUserRepository).save(any(SchoolUser.class));
        verify(outboxRepository).save(any(Outbox.class));
    }

    @Test
    void should_update_existing_user_profile_when_email_exists() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID existingUserId = UUID.randomUUID();
        User existingUser = user(existingUserId, "teacher@example.com");
        Role role = role("TEACHER");
        List<ImportRow> rows = List.of(row(sessionId, 1L, teacherData("teacher@example.com")));

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
        verify(outboxRepository, never()).save(any(Outbox.class));
        verify(schoolUserRepository).save(any(SchoolUser.class));
    }

    @Test
    void should_update_school_user_dates_when_student_already_in_school() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID existingUserId = UUID.randomUUID();
        User existingUser = user(existingUserId, "student@example.com");
        Role role = role("STUDENT");
        SchoolUser existingSchoolUser = SchoolUser.builder()
            .id(UUID.randomUUID())
            .schoolId(schoolId)
            .userId(existingUserId)
            .startDate(Instant.parse("2024-09-01T00:00:00Z"))
            .endDate(Instant.parse("2025-06-30T00:00:00Z"))
            .build();
        List<ImportRow> rows = List.of(row(sessionId, 1L, studentData("student@example.com")));

        mockSchool(schoolId);
        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of(existingUser));
        when(roleRepository.findByCodeIn(Set.of("STUDENT"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(Set.of(existingUserId))).thenReturn(List.of(existingSchoolUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isZero();
        assertThat(result.updated()).isEqualTo(1L);
        // An Excel cell holds only a date, so it is interpreted as midnight in Vietnam time (+07),
        // not midnight UTC: 2025-09-01 00:00+07 == 2025-08-31T17:00Z. The UTC instant is written
        // out directly here on purpose, instead of being rebuilt from DEFAULT_INPUT_ZONE, so that
        // if anyone changes the interpretation zone this test turns red.
        assertThat(existingSchoolUser.getStartDate()).isEqualTo(Instant.parse("2025-08-31T17:00:00Z"));
        assertThat(existingSchoolUser.getEndDate()).isEqualTo(Instant.parse("2026-06-29T17:00:00Z"));
        verify(schoolUserRepository).save(existingSchoolUser);
        verify(outboxRepository, never()).save(any(Outbox.class));
    }

    @Test
    void should_create_school_user_when_student_exists_but_not_in_school() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID existingUserId = UUID.randomUUID();
        User existingUser = user(existingUserId, "student@example.com");
        Role role = role("STUDENT");
        List<ImportRow> rows = List.of(row(sessionId, 1L, studentData("student@example.com")));

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
        verify(outboxRepository, never()).save(any(Outbox.class));
    }

    @Test
    void should_mark_invalid_when_email_belongs_to_user_in_another_school() {
        UUID schoolId = UUID.randomUUID();
        UUID otherSchoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID existingUserId = UUID.randomUUID();
        User existingUser = user(existingUserId, "student@example.com");
        Role role = role("STUDENT");
        SchoolUser otherSchoolUser = SchoolUser.builder()
            .id(UUID.randomUUID())
            .schoolId(otherSchoolId)
            .userId(existingUserId)
            .startDate(Instant.parse("2024-09-01T00:00:00Z"))
            .endDate(Instant.parse("2025-06-30T00:00:00Z"))
            .build();
        List<ImportRow> rows = List.of(row(sessionId, 1L, studentData("student@example.com")));

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
        assertThat(rows.get(0).getErrorsJson()).contains("another school");
        verify(userRepository, never()).save(any());
        verify(schoolUserRepository, never()).save(any(SchoolUser.class));
        assertThat(otherSchoolUser.getStartDate()).isEqualTo(Instant.parse("2024-09-01T00:00:00Z"));
        assertThat(otherSchoolUser.getEndDate()).isEqualTo(Instant.parse("2025-06-30T00:00:00Z"));
    }

    @Test
    void should_not_require_dates_for_teacher_rows() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Role role = role("TEACHER");
        User savedUser = user(UUID.randomUUID(), "teacher@example.com");
        Map<String, String> teacherWithoutDates = Map.of(
            "Email", "teacher@example.com", "Họ tên", "Nguyễn Văn B", "Vai trò", "TEACHER",
            "Điện thoại", "0901234568", "Ngày sinh", "1990-05-15",
            "Địa chỉ", "Hà Nội"
        );
        List<ImportRow> rows = List.of(row(sessionId, 1L, teacherWithoutDates));

        mockSchool(schoolId);
        when(userRepository.findByEmailIn(Set.of("teacher@example.com"))).thenReturn(List.of());
        when(roleRepository.findByCodeIn(Set.of("TEACHER"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(any())).thenReturn(List.of());
        when(userRepository.save(any())).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.invalid()).isZero();
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
    }

    @Test
    void should_mark_invalid_when_student_start_date_not_before_end_date() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Role role = role("STUDENT");
        Map<String, String> badDates = Map.of(
            "Email", "student@example.com", "Họ tên", "Nguyễn Văn A", "Vai trò", "STUDENT",
            "Điện thoại", "0901234567", "Ngày sinh", "2000-01-01",
            "Ngày bắt đầu", "2026-06-30", "Ngày kết thúc", "2026-06-30",
            "Địa chỉ", "Hà Nội"
        );
        List<ImportRow> rows = List.of(row(sessionId, 1L, badDates));

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
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Role role = role("STUDENT");
        User savedUser = user(UUID.randomUUID(), "student@example.com");
        List<ImportRow> rows = List.of(
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

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.updated()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(1).getErrorsJson()).contains("duplicated");
    }

    @Test
    void should_mark_invalid_when_role_not_allowed() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Role systemAdminRole = role("SYSTEM_ADMIN");
        Map<String, String> data = Map.of(
            "Email", "admin@example.com", "Họ tên", "Kẻ Gian", "Vai trò", "SYSTEM_ADMIN",
            "Điện thoại", "0901234599", "Ngày sinh", "1990-01-01",
            "Ngày bắt đầu", "2025-09-01", "Ngày kết thúc", "2026-06-30",
            "Địa chỉ", "Hà Nội"
        );
        List<ImportRow> rows = List.of(row(sessionId, 1L, data));

        mockSchool(schoolId);
        when(userRepository.findByEmailIn(Set.of("admin@example.com"))).thenReturn(List.of());
        when(roleRepository.findByCodeIn(Set.of("SYSTEM_ADMIN"))).thenReturn(List.of(systemAdminRole));
        when(schoolUserRepository.findByUserIdIn(any())).thenReturn(List.of());

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(0).getErrorsJson()).contains("is invalid");
        verify(userRepository, never()).save(any());
        verify(userRoleRepository, never()).save(any(UserRole.class));
        verify(schoolUserRepository, never()).save(any(SchoolUser.class));
        verify(outboxRepository, never()).save(any(Outbox.class));
    }

    @Test
    void should_handle_phone_conflict_per_row() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Role role = role("STUDENT");
        User savedUser = user(UUID.randomUUID(), "success@example.com");
        List<ImportRow> rows = List.of(
            row(sessionId, 1L, studentData("conflict@example.com")),
            row(sessionId, 2L, studentData("success@example.com"))
        );

        mockSchool(schoolId);
        when(userRepository.findByEmailIn(Set.of("conflict@example.com", "success@example.com"))).thenReturn(List.of());
        when(roleRepository.findByCodeIn(Set.of("STUDENT"))).thenReturn(List.of(role));
        when(schoolUserRepository.findByUserIdIn(any())).thenReturn(List.of());
        when(userRepository.save(any()))
            .thenThrow(new DataConflictException("phone conflict", null))
            .thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.FAILED);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(rows.get(0).getErrorsJson()).contains("already exists");
    }

    @Test
    void should_isolate_invalid_value_object_per_row() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Role role = role("STUDENT");
        User savedUser = user(UUID.randomUUID(), "good@example.com");
        Map<String, String> badPhoneData = Map.of(
            "Email", "bad@example.com", "Họ tên", "Nguyễn Văn A", "Vai trò", "STUDENT",
            "Điện thoại", "123", "Ngày sinh", "2000-01-01",
            "Ngày bắt đầu", "2025-09-01", "Ngày kết thúc", "2026-06-30",
            "Địa chỉ", "Hà Nội"
        );
        List<ImportRow> rows = List.of(
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

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.FAILED);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
    }

    private void mockSchool(UUID schoolId) {
        School school = new School();
        school.setId(schoolId);
        school.setName("Trường Test");
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));
    }

    private ImportSession session(UUID id, UUID schoolId, UUID createdBy) {
        return ImportSession.builder()
            .id(id)
            .schoolId(schoolId)
            .type(ImportType.USER)
            .fileName("users.csv")
            .originalHeadersJson("[]")
            .suggestedMappingJson("{}")
            .confirmedMappingJson(jsonSerializationPort.toJson(mapping()))
            .validRows(0L)
            .invalidRows(0L)
            .importedRows(0L)
            .skippedRows(0L)
            .totalRows(0L)
            .failureReason(null)
            .status(ImportSessionStatus.IMPORTING)
            .importedEntityId(null)
            .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
            .claimedAt(null)
            .claimedBy(null)
            .leaseExpiresAt(null)
            .attempts(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(createdBy.toString())
            .updatedBy(createdBy.toString())
            .build();
    }

    private ImportRow row(UUID sessionId, long rowNumber, Map<String, String> rawData) {
        return ImportRow.builder()
            .id(UUID.randomUUID())
            .sessionId(sessionId)
            .rowNumber(rowNumber)
            .rawDataJson(jsonSerializationPort.toJson(rawData))
            .mappedDataJson(null)
            .errorsJson(null)
            .status(ImportRowStatus.PENDING)
            .build();
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
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setCode(new RoleCode(code));
        return role;
    }

    private static User user(UUID id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(new Email(email));
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
