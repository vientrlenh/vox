package com.sep.vox.application.usecase.schoolclassuser;

import com.sep.vox.application.usecase.TestSchoolUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.command.AcceptSchoolClassUserImportCommand;
import com.sep.vox.application.port.input.usecase.schoolclassuser.AcceptSchoolClassUserImportUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.ClassCode;
import com.sep.vox.domain.valueobject.Email;

class AcceptSchoolClassUserImportUseCaseTests {

    private ImportSessionRepository importSessionRepository;
    private ImportRowRepository importRowRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
    private SchoolClassRepository schoolClassRepository;
    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private UserContextPort userContextPort;
    private FakeJsonSerializationPort jsonSerializationPort;
    private AcceptSchoolClassUserImportUseCase useCase;

    @BeforeEach
    void setUp() {
        importSessionRepository = mock(ImportSessionRepository.class);
        importRowRepository = mock(ImportRowRepository.class);
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userContextPort = mock(UserContextPort.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        useCase = new AcceptSchoolClassUserImportUseCase(
            importSessionRepository,
            importRowRepository,
            schoolClassUserRepository,
            schoolClassRepository,
            userRepository,
            schoolRepository,
            userContextPort,
            jsonSerializationPort,
            TestSchoolUserRepository.create()
        );
    }

    @Test
    void execute_should_import_valid_rows_and_mark_invalid_rows() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var otherSchoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var inactiveClassId = UUID.randomUUID();
        var duplicateDbClassId = UUID.randomUUID();
        var validUser = activeUser(UUID.randomUUID(), schoolId, "student@example.com");
        var duplicateFileUser = activeUser(UUID.randomUUID(), schoolId, "duplicate-file@example.com");
        var duplicateDbUser = activeUser(UUID.randomUUID(), schoolId, "duplicate-db@example.com");
        var inactiveUser = user(UUID.randomUUID(), schoolId, "inactive@example.com", UserStatus.INACTIVE);
        var otherSchoolUser = activeUser(UUID.randomUUID(), otherSchoolId, "other-school@example.com");
        var activeClass = activeSchoolClass(classId, schoolId, "ENG-01");
        var inactiveClass = schoolClass(inactiveClassId, schoolId, "INACTIVE-01", SchoolClassStatus.ARCHIVED);
        var duplicateDbClass = activeSchoolClass(duplicateDbClassId, schoolId, "DUP-DB");
        var session = previewedSession(sessionId, schoolId, ImportSessionStatus.PREVIEWED, ImportType.SCHOOL_CLASS_USER);
        var rows = List.of(
            row(sessionId, 1L, Map.of("Email", " Student@Example.com ", "Mã lớp", " eng-01 "), jsonSerializationPort),
            row(sessionId, 2L, Map.of("Email", "missing@example.com", "Mã lớp", "ENG-01"), jsonSerializationPort),
            row(sessionId, 3L, Map.of("Email", "inactive@example.com", "Mã lớp", "ENG-01"), jsonSerializationPort),
            row(sessionId, 4L, Map.of("Email", "other-school@example.com", "Mã lớp", "ENG-01"), jsonSerializationPort),
            row(sessionId, 5L, Map.of("Email", "student@example.com", "Mã lớp", "MISSING"), jsonSerializationPort),
            row(sessionId, 6L, Map.of("Email", "student@example.com", "Mã lớp", "INACTIVE-01"), jsonSerializationPort),
            row(sessionId, 7L, Map.of("Email", "duplicate-file@example.com", "Mã lớp", "ENG-01"), jsonSerializationPort),
            row(sessionId, 8L, Map.of("Email", "duplicate-file@example.com", "Mã lớp", "ENG-01"), jsonSerializationPort),
            row(sessionId, 9L, Map.of("Email", "duplicate-db@example.com", "Mã lớp", "DUP-DB"), jsonSerializationPort)
        );
        var mapping = Map.of("Email", "email", "Mã lớp", "classCode");

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, schoolId, "admin@example.com")));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(importSessionRepository.save(any(ImportSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importRowRepository.findBySessionIdOrderByRowNumber(sessionId)).thenReturn(rows);
        var expectedEmails = Set.of(
            "student@example.com",
            "missing@example.com",
            "inactive@example.com",
            "other-school@example.com",
            "duplicate-file@example.com",
            "duplicate-db@example.com"
        );
        var expectedClassCodes = Set.of("ENG-01", "MISSING", "INACTIVE-01", "DUP-DB");
        when(userRepository.findByEmailIn(expectedEmails)).thenReturn(List.of(
            validUser,
            inactiveUser,
            otherSchoolUser,
            duplicateFileUser,
            duplicateDbUser
        ));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, expectedClassCodes)).thenReturn(List.of(
            activeClass,
            inactiveClass,
            duplicateDbClass
        ));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(any(), any()))
            .thenReturn(List.of(new SchoolClassUser(duplicateDbUser.getId(), duplicateDbClassId, true, OffsetDateTime.now(), null, currentUserId)));
        when(schoolClassUserRepository.saveAll(any())).thenAnswer(invocation -> new ArrayList<>(invocation.getArgument(0)));

        var response = useCase.execute(new AcceptSchoolClassUserImportCommand(schoolId, sessionId, mapping));

        assertThat(response.totalRows()).isEqualTo(9L);
        assertThat(response.importedRows()).isEqualTo(3L);
        assertThat(response.updatedRows()).isEqualTo(1L);
        assertThat(response.invalidRows()).isEqualTo(6L);
        assertThat(response.skippedRows()).isZero();
        assertThat(response.status()).isEqualTo("COMPLETED");
        verify(userRepository).findByEmailIn(expectedEmails);
        verify(schoolClassRepository).findBySchoolIdAndCodeIn(schoolId, expectedClassCodes);
        verify(schoolClassUserRepository).findByUserIdInAndSchoolClassIdIn(any(), any());
        verify(userRepository, never()).findByEmail(any());
        verify(schoolClassRepository, never()).findBySchoolIdAndCode(any(), any());
        verify(schoolClassUserRepository, never()).save(any());
        verify(schoolClassUserRepository, never()).findByUserIdAndSchoolClassId(any(), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ImportRow>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(importRowRepository).saveAll(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue())
            .extracting(ImportRow::getStatus)
            .containsExactly(
                ImportRowStatus.IMPORTED,
                ImportRowStatus.INVALID,
                ImportRowStatus.INVALID,
                ImportRowStatus.INVALID,
                ImportRowStatus.INVALID,
                ImportRowStatus.INVALID,
                ImportRowStatus.IMPORTED,
                ImportRowStatus.INVALID,
                ImportRowStatus.IMPORTED
            );
        assertThat(rowsCaptor.getValue().get(0).getMappedDataJson()).contains("student@example.com", "ENG-01");
        assertThat(rowsCaptor.getValue().get(7).getErrorsJson()).contains("trùng");
        assertThat(rowsCaptor.getValue().get(8).getErrorsJson()).isNull();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<SchoolClassUser>> membershipCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(schoolClassUserRepository).saveAll(membershipCaptor.capture());
        var savedMemberships = membershipCaptor.getValue();
        assertThat(savedMemberships)
            .hasSize(3)
            .extracting(SchoolClassUser::getSchoolClassId)
            .containsExactlyInAnyOrder(classId, classId, duplicateDbClassId);

        var updatedMembership = savedMemberships.stream()
            .filter(m -> m.getSchoolClassId().equals(duplicateDbClassId))
            .findFirst()
            .orElseThrow();
        assertThat(updatedMembership.isActive()).isTrue();
        assertThat(updatedMembership.getLeftAt()).isNull();
        assertThat(updatedMembership.getAssignedBy()).isEqualTo(currentUserId);
    }

    @Test
    void execute_should_throw_when_required_mapping_is_missing() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var mapping = Map.of("Email", "email");

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(previewedSession(sessionId, schoolId, ImportSessionStatus.PREVIEWED, ImportType.SCHOOL_CLASS_USER)));

        assertThrows(IllegalArgumentException.class,
            () -> useCase.execute(new AcceptSchoolClassUserImportCommand(schoolId, sessionId, mapping)));

        verify(importRowRepository, never()).findBySessionIdOrderByRowNumber(any());
        verify(schoolClassUserRepository, never()).save(any());
        verify(schoolClassUserRepository, never()).saveAll(any());
    }

    @Test
    void execute_should_throw_when_session_has_wrong_type() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(previewedSession(sessionId, schoolId, ImportSessionStatus.PREVIEWED, ImportType.SCHOOL_CLASS)));

        assertThrows(IllegalArgumentException.class,
            () -> useCase.execute(new AcceptSchoolClassUserImportCommand(schoolId, sessionId, mapping())));

        verify(importRowRepository, never()).findBySessionIdOrderByRowNumber(any());
    }

    @Test
    void execute_should_throw_when_session_belongs_to_other_school() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(previewedSession(sessionId, UUID.randomUUID(), ImportSessionStatus.PREVIEWED, ImportType.SCHOOL_CLASS_USER)));

        assertThrows(IllegalArgumentException.class,
            () -> useCase.execute(new AcceptSchoolClassUserImportCommand(schoolId, sessionId, mapping())));

        verify(importRowRepository, never()).findBySessionIdOrderByRowNumber(any());
    }

    @Test
    void execute_should_throw_when_session_is_not_previewed() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(previewedSession(sessionId, schoolId, ImportSessionStatus.COMPLETED, ImportType.SCHOOL_CLASS_USER)));

        assertThrows(IllegalStateException.class,
            () -> useCase.execute(new AcceptSchoolClassUserImportCommand(schoolId, sessionId, mapping())));

        verify(importRowRepository, never()).findBySessionIdOrderByRowNumber(any());
    }

    @Test
    void execute_should_throw_and_expire_session_when_session_is_expired() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var session = previewedSession(sessionId, schoolId, ImportSessionStatus.PREVIEWED, ImportType.SCHOOL_CLASS_USER);
        session.setExpiresAt(OffsetDateTime.now().minusMinutes(1));

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(importSessionRepository.save(any(ImportSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(IllegalStateException.class,
            () -> useCase.execute(new AcceptSchoolClassUserImportCommand(schoolId, sessionId, mapping())));

        assertThat(session.getStatus()).isEqualTo(ImportSessionStatus.EXPIRED);
        verify(importRowRepository, never()).findBySessionIdOrderByRowNumber(any());
    }

    @Test
    void execute_should_reactivate_inactive_membership_when_pair_exists() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var student = activeUser(UUID.randomUUID(), schoolId, "student@example.com");
        var schoolClass = activeSchoolClass(classId, schoolId, "ENG-01");
        var inactiveMembership = new SchoolClassUser(
            UUID.randomUUID(),
            student.getId(), classId,
            false,
            OffsetDateTime.now().minusDays(30),
            OffsetDateTime.now().minusDays(10),
            UUID.randomUUID()
        );

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(previewedSession(sessionId, schoolId, ImportSessionStatus.PREVIEWED, ImportType.SCHOOL_CLASS_USER)));
        when(importSessionRepository.save(any(ImportSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(importRowRepository.findBySessionIdOrderByRowNumber(sessionId))
            .thenReturn(List.of(row(sessionId, 1L, Map.of("Email", "student@example.com", "Mã lớp", "ENG-01"), jsonSerializationPort)));
        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of(student));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"))).thenReturn(List.of(schoolClass));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(any(), any())).thenReturn(List.of(inactiveMembership));
        when(schoolClassUserRepository.saveAll(any())).thenAnswer(inv -> new ArrayList<>((Collection<?>) inv.getArgument(0)));

        var response = useCase.execute(new AcceptSchoolClassUserImportCommand(schoolId, sessionId, mapping()));

        assertThat(response.importedRows()).isEqualTo(1L);
        assertThat(response.updatedRows()).isEqualTo(1L);
        assertThat(response.invalidRows()).isZero();
        assertThat(inactiveMembership.isActive()).isTrue();
        assertThat(inactiveMembership.getLeftAt()).isNull();
        assertThat(inactiveMembership.getAssignedBy()).isEqualTo(currentUserId);
        assertThat(inactiveMembership.getJoinedAt()).isNotNull();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<SchoolClassUser>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(schoolClassUserRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1).first().isSameAs(inactiveMembership);
    }

    @Test
    void execute_should_update_active_membership_when_pair_already_active() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var originalAssigner = UUID.randomUUID();
        var student = activeUser(UUID.randomUUID(), schoolId, "student@example.com");
        var schoolClass = activeSchoolClass(classId, schoolId, "ENG-01");
        var activeMembership = new SchoolClassUser(
            UUID.randomUUID(),
            student.getId(), classId,
            true,
            OffsetDateTime.now().minusDays(10),
            null,
            originalAssigner
        );

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(previewedSession(sessionId, schoolId, ImportSessionStatus.PREVIEWED, ImportType.SCHOOL_CLASS_USER)));
        when(importSessionRepository.save(any(ImportSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(importRowRepository.findBySessionIdOrderByRowNumber(sessionId))
            .thenReturn(List.of(row(sessionId, 1L, Map.of("Email", "student@example.com", "Mã lớp", "ENG-01"), jsonSerializationPort)));
        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of(student));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"))).thenReturn(List.of(schoolClass));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(any(), any())).thenReturn(List.of(activeMembership));
        when(schoolClassUserRepository.saveAll(any())).thenAnswer(inv -> new ArrayList<>((Collection<?>) inv.getArgument(0)));

        var response = useCase.execute(new AcceptSchoolClassUserImportCommand(schoolId, sessionId, mapping()));

        assertThat(response.importedRows()).isEqualTo(1L);
        assertThat(response.updatedRows()).isEqualTo(1L);
        assertThat(activeMembership.isActive()).isTrue();
        assertThat(activeMembership.getAssignedBy()).isEqualTo(currentUserId);
    }

    @Test
    void execute_should_create_new_and_update_existing_in_same_session() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var newClassId = UUID.randomUUID();
        var existingClassId = UUID.randomUUID();
        var newStudent = activeUser(UUID.randomUUID(), schoolId, "new@example.com");
        var existingStudent = activeUser(UUID.randomUUID(), schoolId, "existing@example.com");
        var newClass = activeSchoolClass(newClassId, schoolId, "NEW-01");
        var existingClass = activeSchoolClass(existingClassId, schoolId, "EXT-01");
        var existingMembership = new SchoolClassUser(
            UUID.randomUUID(),
            existingStudent.getId(), existingClassId,
            false,
            OffsetDateTime.now().minusDays(5),
            OffsetDateTime.now().minusDays(1),
            UUID.randomUUID()
        );
        var rows = List.of(
            row(sessionId, 1L, Map.of("Email", "new@example.com", "Mã lớp", "NEW-01"), jsonSerializationPort),
            row(sessionId, 2L, Map.of("Email", "existing@example.com", "Mã lớp", "EXT-01"), jsonSerializationPort)
        );

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(previewedSession(sessionId, schoolId, ImportSessionStatus.PREVIEWED, ImportType.SCHOOL_CLASS_USER)));
        when(importSessionRepository.save(any(ImportSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(importRowRepository.findBySessionIdOrderByRowNumber(sessionId)).thenReturn(rows);
        when(userRepository.findByEmailIn(Set.of("new@example.com", "existing@example.com")))
            .thenReturn(List.of(newStudent, existingStudent));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("NEW-01", "EXT-01")))
            .thenReturn(List.of(newClass, existingClass));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(any(), any()))
            .thenReturn(List.of(existingMembership));
        when(schoolClassUserRepository.saveAll(any())).thenAnswer(inv -> new ArrayList<>((Collection<?>) inv.getArgument(0)));

        var response = useCase.execute(new AcceptSchoolClassUserImportCommand(schoolId, sessionId, mapping()));

        assertThat(response.importedRows()).isEqualTo(2L);
        assertThat(response.updatedRows()).isEqualTo(1L);
        assertThat(response.invalidRows()).isZero();
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<SchoolClassUser>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(schoolClassUserRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        var newMembership = captor.getValue().stream().filter(m -> m.getSchoolClassId().equals(newClassId)).findFirst().orElseThrow();
        assertThat(newMembership.getId()).isNull();
        assertThat(newMembership.isActive()).isTrue();
        assertThat(existingMembership.isActive()).isTrue();
        assertThat(existingMembership.getLeftAt()).isNull();
        assertThat(existingMembership.getAssignedBy()).isEqualTo(currentUserId);
    }

    @Test
    void execute_should_mark_duplicate_pair_within_file_as_invalid() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var student = activeUser(UUID.randomUUID(), schoolId, "student@example.com");
        var schoolClass = activeSchoolClass(classId, schoolId, "ENG-01");
        var rows = List.of(
            row(sessionId, 1L, Map.of("Email", "student@example.com", "Mã lớp", "ENG-01"), jsonSerializationPort),
            row(sessionId, 2L, Map.of("Email", "student@example.com", "Mã lớp", "ENG-01"), jsonSerializationPort)
        );

        mockCurrentUserAndSchool(currentUserId, schoolId);
        when(importSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(previewedSession(sessionId, schoolId, ImportSessionStatus.PREVIEWED, ImportType.SCHOOL_CLASS_USER)));
        when(importSessionRepository.save(any(ImportSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(importRowRepository.findBySessionIdOrderByRowNumber(sessionId)).thenReturn(rows);
        when(userRepository.findByEmailIn(Set.of("student@example.com"))).thenReturn(List.of(student));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"))).thenReturn(List.of(schoolClass));
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(any(), any())).thenReturn(List.of());
        when(schoolClassUserRepository.saveAll(any())).thenAnswer(inv -> new ArrayList<>((Collection<?>) inv.getArgument(0)));

        var response = useCase.execute(new AcceptSchoolClassUserImportCommand(schoolId, sessionId, mapping()));

        assertThat(response.importedRows()).isEqualTo(1L);
        assertThat(response.updatedRows()).isZero();
        assertThat(response.invalidRows()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(1).getErrorsJson()).contains("trùng");
    }

    private void mockCurrentUserAndSchool(UUID currentUserId, UUID schoolId) {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, schoolId, "admin@example.com")));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
    }

    private static Map<String, String> mapping() {
        return Map.of("Email", "email", "Mã lớp", "classCode");
    }

    private static ImportSession previewedSession(UUID id, UUID schoolId, ImportSessionStatus status, ImportType type) {
        return new ImportSession(
            id,
            schoolId,
            type,
            "class-users.csv",
            "[]",
            "{}",
            null,
            0L,
            0L,
            0L,
            0L,
            2L,
            null,
            status,
            null,
            OffsetDateTime.now().plusDays(1),
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }

    private static ImportRow row(UUID sessionId, long rowNumber, Map<String, String> rawData, FakeJsonSerializationPort jsonSerializationPort) {
        return new ImportRow(UUID.randomUUID(), sessionId, rowNumber, jsonSerializationPort.toJson(rawData), null, null, ImportRowStatus.PENDING);
    }

    private static User activeUser(UUID id, UUID schoolId, String email) {
        return user(id, schoolId, email, UserStatus.ACTIVE);
    }

    private static User user(UUID id, UUID schoolId, String email, UserStatus status) {
        var user = new User();
        user.setId(id);
        TestSchoolUserRepository.remember(id, schoolId);
        user.setEmail(new Email(email));
        user.setStatus(status);
        return user;
    }

    private static School activeSchool(UUID id) {
        var school = new School();
        school.setId(id);
        school.setActive(true);
        return school;
    }

    private static SchoolClass activeSchoolClass(UUID id, UUID schoolId, String code) {
        return schoolClass(id, schoolId, code, SchoolClassStatus.ACTIVE);
    }

    private static SchoolClass schoolClass(UUID id, UUID schoolId, String code, SchoolClassStatus status) {
        var schoolClass = new SchoolClass();
        schoolClass.setId(id);
        schoolClass.setSchoolId(schoolId);
        schoolClass.setCode(new ClassCode(code));
        schoolClass.setName(code);
        schoolClass.setStatus(status);
        return schoolClass;
    }
}
