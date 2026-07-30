package com.sep.vox.application.usecase.schooluser;

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

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.schooluser.CreateSchoolUserUseCase;
import com.sep.vox.application.port.output.UserContextPort;
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
        var caller = callerUser(callerId, schoolId);
        var savedUser = savedUser(schoolId);
        var studentRole = role("STUDENT");
        var command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT", null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.findByEmail("student@school.edu.vn")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0987654321")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(new UserRole(savedUser.getId(), studentRole.getId(), Instant.now()));

        assertThrows(IllegalArgumentException.class, () -> createSchoolUserUseCase.execute(command));
        verify(schoolUserRepository, never()).save(any(SchoolUser.class));
    }

    @Test
    void create_student_with_dates_should_save_school_user() {
        var caller = callerUser(callerId, schoolId);
        var savedUser = savedUser(schoolId);
        var studentRole = role("STUDENT");
        var command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT",
            LocalDate.of(2023, 1, 1), LocalDate.of(2029, 1, 1)
        );
        var schoolUser = new SchoolUser(schoolId, savedUser.getId(), Instant.now(), Instant.now().plus(36500, ChronoUnit.DAYS));

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.findByEmail("student@school.edu.vn")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0987654321")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(new UserRole(savedUser.getId(), studentRole.getId(), Instant.now()));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenReturn(schoolUser);
        var result = createSchoolUserUseCase.execute(command);

        assertThat(result.id()).isEqualTo(savedUser.getId());
        verify(schoolUserRepository).save(any(SchoolUser.class));
        verify(outboxRepository).save(any(Outbox.class));
    }

    @Test
    void create_student_with_start_date_not_before_end_date_should_throw() {
        var caller = callerUser(callerId, schoolId);
        var savedUser = savedUser(schoolId);
        var studentRole = role("STUDENT");
        var command = new CreateSchoolUserCommand(
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
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(new UserRole(savedUser.getId(), studentRole.getId(), Instant.now()));

        assertThrows(IllegalArgumentException.class, () -> createSchoolUserUseCase.execute(command));
        verify(schoolUserRepository, never()).save(any(SchoolUser.class));
    }

    @Test
    void create_should_throw_when_school_inactive() {
        var caller = callerUser(callerId, schoolId);
        var studentRole = role("STUDENT");
        var inactiveSchool = school(schoolId, "Trường A");
        inactiveSchool.setActive(false);
        var command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT",
            LocalDate.of(2023, 1, 1), LocalDate.of(2029, 1, 1)
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(inactiveSchool));
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));

        assertThrows(IllegalStateException.class, () -> createSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void create_teacher_should_save_school_user() {
        var caller = callerUser(callerId, schoolId);
        var savedUser = savedUser(schoolId);
        var teacherRole = role("TEACHER");
        var command = new CreateSchoolUserCommand(
            schoolId, "teacher@school.edu.vn", "0987654322",
            "Tran Thi B", LocalDate.of(1985, 3, 20), "456 Street", "TEACHER", null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("TEACHER")).thenReturn(Optional.of(teacherRole));
        when(userRepository.findByEmail("teacher@school.edu.vn")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0987654322")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(new UserRole(savedUser.getId(), teacherRole.getId(), Instant.now()));
        var result = createSchoolUserUseCase.execute(command);

        assertThat(result.id()).isEqualTo(savedUser.getId());
        // Giáo viên giờ cũng được gắn vào trường qua school_users (không có thời hạn)
        verify(schoolUserRepository).save(any(SchoolUser.class));
        verify(outboxRepository).save(any(Outbox.class));
    }

    @Test
    void create_should_throw_when_caller_belongs_to_different_school() {
        var otherSchoolId = UUID.randomUUID();
        var caller = callerUser(callerId, otherSchoolId);
        var command = new CreateSchoolUserCommand(
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
        var caller = callerUser(callerId, schoolId);
        var command = new CreateSchoolUserCommand(
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
        var caller = callerUser(callerId, schoolId);
        var command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT", null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> createSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void create_should_throw_when_caller_is_inactive() {
        var command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT", null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> createSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void create_should_throw_when_email_already_exists() {
        var caller = callerUser(callerId, schoolId);
        var studentRole = role("STUDENT");
        var command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT", null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.findByEmail("student@school.edu.vn")).thenReturn(Optional.of(savedUser(schoolId)));

        assertThrows(DuplicatedException.class, () -> createSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void create_should_throw_when_phone_already_exists() {
        var caller = callerUser(callerId, schoolId);
        var studentRole = role("STUDENT");
        var command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT", null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.findByEmail("student@school.edu.vn")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0987654321")).thenReturn(Optional.of(savedUser(schoolId)));

        assertThrows(DuplicatedException.class, () -> createSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    private User callerUser(UUID id, UUID userSchoolId) {
        return callerUser(id, userSchoolId, UserStatus.ACTIVE);
    }

    private User callerUser(UUID id, UUID userSchoolId, UserStatus status) {
        var now = Instant.now();
        var user = new User(id, new Email("admin@school.edu.vn"), "hash",
            new Phone("0900000000"), new FullName("Admin User"), null,
            new DateOfBirth(LocalDate.of(1980, 1, 1)), "Admin Street", null,
            status, now, now, id, id);
        when(schoolUserRepository.findByUserId(id)).thenReturn(Optional.of(
            new SchoolUser(userSchoolId, id, now, now.plus(36500, ChronoUnit.DAYS))
        ));
        return user;
    }

    private User savedUser(UUID userSchoolId) {
        var now = Instant.now();
        var id = UUID.randomUUID();
        return new User(id, new Email("student@school.edu.vn"), "__PASSWORD_NOT_SET__",
            new Phone("0987654321"), new FullName("Nguyen Van A"), null,
            new DateOfBirth(LocalDate.of(2005, 1, 15)), "123 Street", null,
            UserStatus.INACTIVE, now, now, callerId, callerId);
    }

    private Role role(String code) {
        var now = Instant.now();
        var systemId = UUID.randomUUID();
        return new Role(UUID.randomUUID(), new RoleCode(code), code, now, now, systemId, systemId);
    }

    private School school(UUID id, String name) {
        var school = new School();
        school.setId(id);
        school.setName(name);
        school.setActive(true);
        return school;
    }

}