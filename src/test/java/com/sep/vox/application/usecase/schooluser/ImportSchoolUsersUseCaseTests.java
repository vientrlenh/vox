package com.sep.vox.application.usecase.schooluser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.common.importer.ImportParserFactory;
import com.sep.vox.application.port.input.command.ImportFieldMapping;
import com.sep.vox.application.port.input.command.ImportSchoolUsersCommand;
import com.sep.vox.application.port.input.usecase.schooluser.ImportSchoolUsersUseCase;
import com.sep.vox.application.port.output.ImportFileResource;
import com.sep.vox.application.port.output.SchoolUserImportFileStoragePort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.role.Role;
import com.sep.vox.domain.model.schooluser.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.model.userrole.UserRole;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.RoleCode;

public class ImportSchoolUsersUseCaseTests {

    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private UserRoleRepository userRoleRepository;
    private SchoolUserRepository schoolUserRepository;
    private SchoolUserImportFileStoragePort fileStoragePort;
    private ImportParserFactory importParserFactory;
    private ImportSchoolUsersUseCase importSchoolUsersUseCase;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        fileStoragePort = mock(SchoolUserImportFileStoragePort.class);
        importParserFactory = mock(com.sep.vox.application.common.importer.ImportParserFactory.class);
        when(importParserFactory.forFormat(com.sep.vox.application.common.importer.ImportFileFormat.CSV))
            .thenReturn(new com.sep.vox.infrastructure.importer.CsvImportParser());

        importSchoolUsersUseCase = new ImportSchoolUsersUseCase(
            userContextPort,
            userRepository,
            roleRepository,
            userRoleRepository,
            schoolUserRepository,
            fileStoragePort,
            new NoopTransactionManager(),
            importParserFactory
        );
    }

    @Test
    void import_should_return_errors_when_required_fields_missing() {
        var csv = "Email,Phone,Full Name,DOB\n,0987654321,Nguyen Van A,2005-01-15\n";
        var command = new ImportSchoolUsersCommand(
            schoolId,
            "file-1",
            true,
            "STUDENT",
            Map.of(
                "email", new ImportFieldMapping("Email", null, null, null),
                "phone", new ImportFieldMapping("Phone", null, null, null),
                "fullName", new ImportFieldMapping("Full Name", null, null, null),
                "dateOfBirth", new ImportFieldMapping("DOB", null, null, null)
            )
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(callerUser(callerId, schoolId)));
        when(fileStoragePort.load("file-1")).thenReturn(resource("CSV", csv));

        var result = importSchoolUsersUseCase.execute(command);

        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.errors()).isNotEmpty();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void import_should_create_student_when_valid_and_not_dry_run() {
        var csv = "Email,Phone,Full Name,DOB,Role,Student ID\nstudent@school.edu.vn,0987654321,Nguyen Van A,2005-01-15,STUDENT,STU-001\n";
        var command = new ImportSchoolUsersCommand(
            schoolId,
            "file-1",
            false,
            null,
            Map.of(
                "email", new ImportFieldMapping("Email", null, null, null),
                "phone", new ImportFieldMapping("Phone", null, null, null),
                "fullName", new ImportFieldMapping("Full Name", null, null, null),
                "dateOfBirth", new ImportFieldMapping("DOB", null, null, null),
                "roleCode", new ImportFieldMapping("Role", null, null, null),
                "studentId", new ImportFieldMapping("Student ID", null, null, null)
            )
        );

        var role = role("STUDENT");
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(callerUser(callerId, schoolId)));
        when(fileStoragePort.load("file-1")).thenReturn(resource("CSV", csv));
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(role));
        when(userRepository.findByEmail("student@school.edu.vn")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0987654321")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            var user = invocation.getArgument(0, User.class);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = importSchoolUsersUseCase.execute(command);

        assertThat(result.createdCount()).isEqualTo(1);
        verify(userRepository).save(any(User.class));
        verify(userRoleRepository).save(any(UserRole.class));
        verify(schoolUserRepository).save(any(SchoolUser.class));
    }

    @Test
    void import_should_throw_when_caller_has_different_school() {
        var command = new ImportSchoolUsersCommand(schoolId, "file-1", true, "STUDENT", Map.of());

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(callerUser(callerId, UUID.randomUUID())));

        assertThrows(IllegalArgumentException.class, () -> importSchoolUsersUseCase.execute(command));
        verify(fileStoragePort, never()).load(any(String.class));
    }

    @Test
    void import_should_throw_when_caller_not_found() {
        var command = new ImportSchoolUsersCommand(schoolId, "file-1", true, "STUDENT", Map.of());

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> importSchoolUsersUseCase.execute(command));
        verify(fileStoragePort, never()).load(any(String.class));
    }

    private ImportFileResource resource(String format, String content) {
        return new ImportFileResource(
            "file-1",
            "students.csv",
            format,
            content.getBytes(StandardCharsets.UTF_8).length,
            OffsetDateTime.now().plusHours(1),
            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        );
    }

    private User callerUser(UUID id, UUID userSchoolId) {
        var now = OffsetDateTime.now();
        return new User(id, new Email("admin@school.edu.vn"), "hash",
            new Phone("0900000000"), new FullName("Admin User"), null,
            new DateOfBirth(LocalDate.of(1980, 1, 1)), "Admin Street",
            UserStatus.ACTIVE, now, now, id, id, userSchoolId);
    }

    private Role role(String code) {
        var now = OffsetDateTime.now();
        var role = new Role();
        role.setId(UUID.randomUUID());
        role.setCode(new RoleCode(code));
        role.setName(code);
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        return role;
    }

    private static class NoopTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
