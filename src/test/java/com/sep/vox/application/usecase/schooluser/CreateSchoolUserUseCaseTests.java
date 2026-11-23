package com.sep.vox.application.usecase.schooluser;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.CreateSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.schooluser.CreateSchoolUserUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schooluser.CreateSchoolUserResponse;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.RoleCode;

public class CreateSchoolUserUseCaseTests {

    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private UserRoleRepository userRoleRepository;
    private SchoolUserRepository schoolUserRepository;
    private SchoolRepository schoolRepository;
    private OutboxRepository outboxRepository;
    private FakeJsonSerializationPort jsonSerializationPort;
    private CreateSchoolUserUseCase createSchoolUserUseCase;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        outboxRepository = mock(OutboxRepository.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        createSchoolUserUseCase = new CreateSchoolUserUseCase(
            userContextPort, userRepository, roleRepository,
            userRoleRepository, schoolUserRepository,
            schoolRepository, outboxRepository, jsonSerializationPort
        );
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school(schoolId, "Trường A")));
    }

    @Test
    void create_student_without_dates_should_throw() {
        User caller = callerUser(callerId, schoolId);
        User savedUser = savedUser(schoolId);
        Role studentRole = role("STUDENT");
        CreateSchoolUserCommand command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT", null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.findByEmail("student@school.edu.vn")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0987654321")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(UserRole.builder()
            .userId(savedUser.getId())
            .roleId(studentRole.getId())
            .createdAt(Instant.now())
            .build());

        assertThrows(IllegalArgumentException.class, () -> createSchoolUserUseCase.execute(command));
        verify(schoolUserRepository, never()).save(any(SchoolUser.class));
    }

    @Test
    void create_student_with_dates_should_save_school_user() {
        User caller = callerUser(callerId, schoolId);
        User savedUser = savedUser(schoolId);
        Role studentRole = role("STUDENT");
        CreateSchoolUserCommand command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT",
            LocalDate.of(2023, 1, 1), LocalDate.of(2029, 1, 1)
        );
        SchoolUser schoolUser = SchoolUser.builder()
            .schoolId(schoolId)
            .userId(savedUser.getId())
            .startDate(Instant.now())
            .endDate(Instant.now().plus(36500, ChronoUnit.DAYS))
            .build();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.findByEmail("student@school.edu.vn")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0987654321")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(UserRole.builder()
            .userId(savedUser.getId())
            .roleId(studentRole.getId())
            .createdAt(Instant.now())
            .build());
        when(schoolUserRepository.save(any(SchoolUser.class))).thenReturn(schoolUser);
        CreateSchoolUserResponse result = createSchoolUserUseCase.execute(command);

        assertThat(result.id()).isEqualTo(savedUser.getId());
        verify(schoolUserRepository).save(any(SchoolUser.class));
        verify(outboxRepository).save(any(Outbox.class));
    }

    @Test
    void create_student_with_start_date_not_before_end_date_should_throw() {
        User caller = callerUser(callerId, schoolId);
        User savedUser = savedUser(schoolId);
        Role studentRole = role("STUDENT");
        CreateSchoolUserCommand command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT",
            LocalDate.of(2029, 1, 1), LocalDate.of(2029, 1, 1)
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.findByEmail("student@school.edu.vn")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0987654321")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(UserRole.builder()
            .userId(savedUser.getId())
            .roleId(studentRole.getId())
            .createdAt(Instant.now())
            .build());

        assertThrows(IllegalArgumentException.class, () -> createSchoolUserUseCase.execute(command));
        verify(schoolUserRepository, never()).save(any(SchoolUser.class));
    }

    @Test
    void create_should_throw_when_school_inactive() {
        User caller = callerUser(callerId, schoolId);
        Role studentRole = role("STUDENT");
        School inactiveSchool = school(schoolId, "Trường A");
        inactiveSchool.setActive(false);
        CreateSchoolUserCommand command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT",
            LocalDate.of(2023, 1, 1), LocalDate.of(2029, 1, 1)
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(inactiveSchool));
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));

        assertThrows(IllegalArgumentException.class, () -> createSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void create_teacher_should_save_school_user() {
        User caller = callerUser(callerId, schoolId);
        User savedUser = savedUser(schoolId);
        Role teacherRole = role("TEACHER");
        CreateSchoolUserCommand command = new CreateSchoolUserCommand(
            schoolId, "teacher@school.edu.vn", "0987654322",
            "Tran Thi B", LocalDate.of(1985, 3, 20), "456 Street", "TEACHER", null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("TEACHER")).thenReturn(Optional.of(teacherRole));
        when(userRepository.findByEmail("teacher@school.edu.vn")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0987654322")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(UserRole.builder()
            .userId(savedUser.getId())
            .roleId(teacherRole.getId())
            .createdAt(Instant.now())
            .build());
        CreateSchoolUserResponse result = createSchoolUserUseCase.execute(command);

        assertThat(result.id()).isEqualTo(savedUser.getId());
        // A teacher is now also attached to the school through school_users (with no expiry).
        verify(schoolUserRepository).save(any(SchoolUser.class));
        verify(outboxRepository).save(any(Outbox.class));
    }

    @Test
    void create_should_throw_when_caller_belongs_to_different_school() {
        UUID otherSchoolId = UUID.randomUUID();
        User caller = callerUser(callerId, otherSchoolId);
        CreateSchoolUserCommand command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT", null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("SCHOOL_ADMIN")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> createSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void create_should_throw_when_role_code_is_invalid() {
        User caller = callerUser(callerId, schoolId);
        CreateSchoolUserCommand command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "SCHOOL_ADMIN", null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));

        assertThrows(IllegalArgumentException.class, () -> createSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void create_should_throw_when_role_not_found_in_repository() {
        User caller = callerUser(callerId, schoolId);
        CreateSchoolUserCommand command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT", null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> createSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void create_should_throw_when_caller_is_inactive() {
        CreateSchoolUserCommand command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT", null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> createSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void create_should_throw_when_email_already_exists() {
        User caller = callerUser(callerId, schoolId);
        Role studentRole = role("STUDENT");
        CreateSchoolUserCommand command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT", null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.findByEmail("student@school.edu.vn")).thenReturn(Optional.of(savedUser(schoolId)));

        assertThrows(ResourceDuplicatedException.class, () -> createSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void create_should_throw_when_phone_already_exists() {
        User caller = callerUser(callerId, schoolId);
        Role studentRole = role("STUDENT");
        CreateSchoolUserCommand command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT", null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.findByEmail("student@school.edu.vn")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0987654321")).thenReturn(Optional.of(savedUser(schoolId)));

        assertThrows(ResourceDuplicatedException.class, () -> createSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    private User callerUser(UUID id, UUID userSchoolId) {
        return callerUser(id, userSchoolId, UserStatus.ACTIVE);
    }

    private User callerUser(UUID id, UUID userSchoolId, UserStatus status) {
        Instant now = Instant.now();
        User user = User.builder()
            .id(id)
            .email(new Email("admin@school.edu.vn"))
            .passwordHash("hash")
            .phone(new Phone("0900000000"))
            .fullName(new FullName("Admin User"))
            .dateOfBirth(new DateOfBirth(LocalDate.of(1980, 1, 1)))
            .address("Admin Street")
            .status(status)
            .createdAt(now)
            .updatedAt(now)
            .createdBy(id.toString())
            .updatedBy(id.toString())
            .build();
        when(schoolUserRepository.findByUserId(id)).thenReturn(Optional.of(
            SchoolUser.builder()
                .schoolId(userSchoolId)
                .userId(id)
                .startDate(now)
                .endDate(now.plus(36500, ChronoUnit.DAYS))
                .build()
        ));
        return user;
    }

    private User savedUser(UUID userSchoolId) {
        Instant now = Instant.now();
        UUID id = UUID.randomUUID();
        return User.builder()
            .id(id)
            .email(new Email("student@school.edu.vn"))
            .passwordHash("__PASSWORD_NOT_SET__")
            .phone(new Phone("0987654321"))
            .fullName(new FullName("Nguyen Van A"))
            .dateOfBirth(new DateOfBirth(LocalDate.of(2005, 1, 15)))
            .address("123 Street")
            .status(UserStatus.INACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .createdBy(callerId.toString())
            .updatedBy(callerId.toString())
            .build();
    }

    private Role role(String code) {
        Instant now = Instant.now();
        UUID systemId = UUID.randomUUID();
        return Role.builder()
            .id(UUID.randomUUID())
            .code(new RoleCode(code))
            .name(code)
            .createdAt(now)
            .updatedAt(now)
            .createdBy(systemId.toString())
            .updatedBy(systemId.toString())
            .build();
    }

    private School school(UUID id, String name) {
        School school = new School();
        school.setId(id);
        school.setName(name);
        school.setActive(true);
        return school;
    }

    @Test
    void create_should_throw_duplicated_when_repository_reports_a_data_conflict_on_the_user() {
        CreateSchoolUserCommand command = stubStudentCreationUpToTheUserSave();
        when(userRepository.save(any(User.class))).thenThrow(new DataConflictException("unique violation", null));

        assertThatThrownBy(() -> createSchoolUserUseCase.execute(command))
            .isInstanceOf(ResourceDuplicatedException.class)
            .hasMessage("Email hoặc số điện thoại đã tồn tại");
    }

    @Test
    void create_should_throw_illegal_argument_when_repository_reports_a_data_conflict_on_the_school_link() {
        CreateSchoolUserCommand command = stubStudentCreationUpToTheUserSave();
        User savedUser = savedUser(schoolId);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(UserRole.builder().userId(savedUser.getId()).build());
        when(schoolUserRepository.save(any(SchoolUser.class))).thenThrow(new DataConflictException("unique violation", null));

        assertThatThrownBy(() -> createSchoolUserUseCase.execute(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Không thể gán người dùng vào trường: dữ liệu không hợp lệ");
    }

    private CreateSchoolUserCommand stubStudentCreationUpToTheUserSave() {
        CreateSchoolUserCommand command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT",
            LocalDate.of(2023, 1, 1), LocalDate.of(2029, 1, 1)
        );
        User caller = callerUser(callerId, schoolId);
        Role studentRole = role("STUDENT");
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.findByEmail("student@school.edu.vn")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0987654321")).thenReturn(Optional.empty());
        return command;
    }
}
