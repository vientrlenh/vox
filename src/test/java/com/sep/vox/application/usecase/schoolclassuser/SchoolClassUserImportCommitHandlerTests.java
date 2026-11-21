package com.sep.vox.application.usecase.schoolclassuser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import com.sep.vox.application.port.input.service.ImportCommitResult;
import com.sep.vox.application.port.input.service.SchoolClassUserImportCommitHandler;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.ClassCode;
import com.sep.vox.domain.valueobject.Email;

class SchoolClassUserImportCommitHandlerTests {

    private UserRepository userRepository;
    private SchoolClassRepository schoolClassRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
    private SchoolUserRepository schoolUserRepository;
    private FakeJsonSerializationPort jsonSerializationPort;
    private PlatformTransactionManager txManager;
    private SchoolClassUserImportCommitHandler handler;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        handler = new SchoolClassUserImportCommitHandler(
            userRepository,
            schoolClassRepository,
            schoolClassUserRepository,
            schoolUserRepository,
            jsonSerializationPort,
            txManager
        );
    }

    @Test
    void should_create_new_membership_for_valid_row() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        User student = activeUser(UUID.randomUUID(), schoolId, "student@example.com");
        SchoolClass schoolClass = activeSchoolClass(classId, schoolId, "ENG-01");
        List<ImportRow> rows = List.of(row(sessionId, 1L, Map.of("Email", "student@example.com", "Mã lớp", "ENG-01")));

        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of(student));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"))).thenReturn(List.of(schoolClass));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(any(), any())).thenReturn(List.of());
        when(schoolClassUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        mockUsersBelongToSchool(schoolId, student);

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.updated()).isZero();
        assertThat(result.invalid()).isZero();
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        verify(schoolClassUserRepository).save(any(SchoolClassUser.class));
    }

    @Test
    void should_reactivate_inactive_membership_when_pair_exists() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        User student = activeUser(UUID.randomUUID(), schoolId, "student@example.com");
        SchoolClass schoolClass = activeSchoolClass(classId, schoolId, "ENG-01");
        SchoolClassUser inactiveMembership = SchoolClassUser.builder()
            .id(UUID.randomUUID())
            .userId(student.getId())
            .schoolClassId(classId)
            .active(false)
            .joinedAt(Instant.now().minus(30, ChronoUnit.DAYS))
            .leftAt(Instant.now().minus(10, ChronoUnit.DAYS))
            .assignedBy(UUID.randomUUID())
            .build();
        List<ImportRow> rows = List.of(row(sessionId, 1L, Map.of("Email", "student@example.com", "Mã lớp", "ENG-01")));

        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of(student));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"))).thenReturn(List.of(schoolClass));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(any(), any())).thenReturn(List.of(inactiveMembership));
        when(schoolClassUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        mockUsersBelongToSchool(schoolId, student);

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isZero();
        assertThat(result.updated()).isEqualTo(1L);
        assertThat(result.invalid()).isZero();
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(inactiveMembership.isActive()).isTrue();
        assertThat(inactiveMembership.getLeftAt()).isNull();
        assertThat(inactiveMembership.getAssignedBy()).isEqualTo(createdBy);
        verify(schoolClassUserRepository).save(inactiveMembership);
    }

    @Test
    void should_update_active_membership_when_already_active() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID originalAssigner = UUID.randomUUID();
        User student = activeUser(UUID.randomUUID(), schoolId, "student@example.com");
        SchoolClass schoolClass = activeSchoolClass(classId, schoolId, "ENG-01");
        SchoolClassUser activeMembership = SchoolClassUser.builder()
            .id(UUID.randomUUID())
            .userId(student.getId())
            .schoolClassId(classId)
            .active(true)
            .joinedAt(Instant.now().minus(10, ChronoUnit.DAYS))
            .leftAt(null)
            .assignedBy(originalAssigner)
            .build();
        List<ImportRow> rows = List.of(row(sessionId, 1L, Map.of("Email", "student@example.com", "Mã lớp", "ENG-01")));

        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of(student));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"))).thenReturn(List.of(schoolClass));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(any(), any())).thenReturn(List.of(activeMembership));
        when(schoolClassUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        mockUsersBelongToSchool(schoolId, student);

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isZero();
        assertThat(result.updated()).isEqualTo(1L);
        assertThat(activeMembership.isActive()).isTrue();
        assertThat(activeMembership.getAssignedBy()).isEqualTo(createdBy);
    }

    @Test
    void should_mark_invalid_when_user_not_found() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        List<ImportRow> rows = List.of(row(sessionId, 1L, Map.of("Email", "notfound@example.com", "Mã lớp", "ENG-01")));

        when(userRepository.findByEmailIn(Set.of("notfound@example.com"))).thenReturn(List.of());
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"))).thenReturn(List.of(activeSchoolClass(UUID.randomUUID(), schoolId, "ENG-01")));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(any(), any())).thenReturn(List.of());

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(0).getErrorsJson()).contains("email");
        verify(schoolClassUserRepository, never()).save(any());
    }

    @Test
    void should_import_user_who_has_not_set_password_yet() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User pendingUser = user(UUID.randomUUID(), schoolId, "pending@example.com", UserStatus.INACTIVE);
        List<ImportRow> rows = List.of(row(sessionId, 1L, Map.of("Email", "pending@example.com", "Mã lớp", "ENG-01")));

        when(userRepository.findByEmailIn(Set.of("pending@example.com"))).thenReturn(List.of(pendingUser));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"))).thenReturn(List.of(activeSchoolClass(UUID.randomUUID(), schoolId, "ENG-01")));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(any(), any())).thenReturn(List.of());
        when(schoolClassUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        mockUsersBelongToSchool(schoolId, pendingUser);

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.invalid()).isZero();
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        verify(schoolClassUserRepository).save(any(SchoolClassUser.class));
    }

    @Test
    void should_mark_invalid_when_user_locked() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User lockedUser = user(UUID.randomUUID(), schoolId, "locked@example.com", UserStatus.LOCKED);
        List<ImportRow> rows = List.of(row(sessionId, 1L, Map.of("Email", "locked@example.com", "Mã lớp", "ENG-01")));

        when(userRepository.findByEmailIn(Set.of("locked@example.com"))).thenReturn(List.of(lockedUser));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"))).thenReturn(List.of(activeSchoolClass(UUID.randomUUID(), schoolId, "ENG-01")));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(any(), any())).thenReturn(List.of());

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(0).getErrorsJson()).contains("email");
        verify(schoolClassUserRepository, never()).save(any());
    }

    @Test
    void should_mark_invalid_when_user_disabled() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User disabledUser = user(UUID.randomUUID(), schoolId, "disabled@example.com", UserStatus.DISABLED);
        List<ImportRow> rows = List.of(row(sessionId, 1L, Map.of("Email", "disabled@example.com", "Mã lớp", "ENG-01")));

        when(userRepository.findByEmailIn(Set.of("disabled@example.com"))).thenReturn(List.of(disabledUser));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"))).thenReturn(List.of(activeSchoolClass(UUID.randomUUID(), schoolId, "ENG-01")));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(any(), any())).thenReturn(List.of());

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(0).getErrorsJson()).contains("email");
        verify(schoolClassUserRepository, never()).save(any());
    }

    @Test
    void should_mark_invalid_when_user_not_in_school() {
        UUID schoolId = UUID.randomUUID();
        UUID otherSchoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User otherSchoolUser = activeUser(UUID.randomUUID(), otherSchoolId, "other@example.com");
        List<ImportRow> rows = List.of(row(sessionId, 1L, Map.of("Email", "other@example.com", "Mã lớp", "ENG-01")));

        when(userRepository.findByEmailIn(Set.of("other@example.com"))).thenReturn(List.of(otherSchoolUser));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"))).thenReturn(List.of(activeSchoolClass(UUID.randomUUID(), schoolId, "ENG-01")));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(any(), any())).thenReturn(List.of());

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(0).getErrorsJson()).contains("email");
        verify(schoolClassUserRepository, never()).save(any());
    }

    @Test
    void should_mark_invalid_when_class_not_found() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User student = activeUser(UUID.randomUUID(), schoolId, "student@example.com");
        List<ImportRow> rows = List.of(row(sessionId, 1L, Map.of("Email", "student@example.com", "Mã lớp", "MISSING")));

        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of(student));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("MISSING"))).thenReturn(List.of());
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(any(), any())).thenReturn(List.of());

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(0).getErrorsJson()).contains("classCode");
        verify(schoolClassUserRepository, never()).save(any());
    }

    @Test
    void should_mark_invalid_when_class_inactive() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        User student = activeUser(UUID.randomUUID(), schoolId, "student@example.com");
        SchoolClass archivedClass = schoolClass(classId, schoolId, "INACTIVE-01", SchoolClassStatus.ARCHIVED);
        List<ImportRow> rows = List.of(row(sessionId, 1L, Map.of("Email", "student@example.com", "Mã lớp", "INACTIVE-01")));

        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of(student));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("INACTIVE-01"))).thenReturn(List.of(archivedClass));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(any(), any())).thenReturn(List.of());

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(0).getErrorsJson()).contains("classCode");
        verify(schoolClassUserRepository, never()).save(any());
    }

    @Test
    void should_mark_invalid_when_duplicate_pair_in_file() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        User student = activeUser(UUID.randomUUID(), schoolId, "student@example.com");
        SchoolClass schoolClass = activeSchoolClass(classId, schoolId, "ENG-01");
        List<ImportRow> rows = List.of(
            row(sessionId, 1L, Map.of("Email", "student@example.com", "Mã lớp", "ENG-01")),
            row(sessionId, 2L, Map.of("Email", "student@example.com", "Mã lớp", "ENG-01"))
        );

        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of(student));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"))).thenReturn(List.of(schoolClass));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(any(), any())).thenReturn(List.of());
        when(schoolClassUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        mockUsersBelongToSchool(schoolId, student);

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(1).getErrorsJson()).contains("trùng");
    }

    @Test
    void should_create_new_and_update_existing_in_same_commit() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID newClassId = UUID.randomUUID();
        UUID existingClassId = UUID.randomUUID();
        User newStudent = activeUser(UUID.randomUUID(), schoolId, "new@example.com");
        User existingStudent = activeUser(UUID.randomUUID(), schoolId, "existing@example.com");
        SchoolClass newClass = activeSchoolClass(newClassId, schoolId, "NEW-01");
        SchoolClass existingClass = activeSchoolClass(existingClassId, schoolId, "EXT-01");
        SchoolClassUser existingMembership = SchoolClassUser.builder()
            .id(UUID.randomUUID())
            .userId(existingStudent.getId())
            .schoolClassId(existingClassId)
            .active(false)
            .joinedAt(Instant.now().minus(5, ChronoUnit.DAYS))
            .leftAt(Instant.now().minus(1, ChronoUnit.DAYS))
            .assignedBy(UUID.randomUUID())
            .build();
        List<ImportRow> rows = List.of(
            row(sessionId, 1L, Map.of("Email", "new@example.com", "Mã lớp", "NEW-01")),
            row(sessionId, 2L, Map.of("Email", "existing@example.com", "Mã lớp", "EXT-01"))
        );

        when(userRepository.findByEmailIn(Set.of("new@example.com", "existing@example.com"))).thenReturn(List.of(newStudent, existingStudent));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("NEW-01", "EXT-01"))).thenReturn(List.of(newClass, existingClass));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(any(), any())).thenReturn(List.of(existingMembership));
        when(schoolClassUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        mockUsersBelongToSchool(schoolId, newStudent, existingStudent);

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.updated()).isEqualTo(1L);
        assertThat(result.invalid()).isZero();
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(existingMembership.isActive()).isTrue();
        assertThat(existingMembership.getLeftAt()).isNull();
        assertThat(existingMembership.getAssignedBy()).isEqualTo(createdBy);
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

    private ImportSession session(UUID id, UUID schoolId, UUID createdBy) {
        return ImportSession.builder()
            .id(id)
            .schoolId(schoolId)
            .type(ImportType.SCHOOL_CLASS_USER)
            .fileName("class-users.csv")
            .originalHeadersJson("[]")
            .suggestedMappingJson("{}")
            .confirmedMappingJson(jsonSerializationPort.toJson(Map.of("Email", "email", "Mã lớp", "classCode")))
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

    private User activeUser(UUID id, UUID schoolId, String email) {
        return user(id, schoolId, email, UserStatus.ACTIVE);
    }

    private User user(UUID id, UUID schoolId, String email, UserStatus status) {
        User u = new User();
        u.setId(id);
        u.setEmail(new Email(email));
        u.setStatus(status);
        return u;
    }

    private static SchoolClass activeSchoolClass(UUID id, UUID schoolId, String code) {
        return schoolClass(id, schoolId, code, SchoolClassStatus.ACTIVE);
    }

    private void mockUsersBelongToSchool(UUID schoolId, User... users) {
        List<SchoolUser> schoolUsers = Arrays.stream(users)
            .map(user -> SchoolUser.builder()
                .schoolId(schoolId)
                .userId(user.getId())
                .startDate(Instant.now())
                .endDate(null)
                .build())
            .toList();
        when(schoolUserRepository.findByUserIdIn(any())).thenReturn(schoolUsers);
    }

    private static SchoolClass schoolClass(UUID id, UUID schoolId, String code, SchoolClassStatus status) {
        SchoolClass sc = new SchoolClass();
        sc.setId(id);
        sc.setSchoolId(schoolId);
        sc.setCode(new ClassCode(code));
        sc.setStatus(status);
        return sc;
    }
}
