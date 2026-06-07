package com.sep.vox.application.usecase.schooluser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.event.SchoolUserPasswordSetUpEmailRequestedEvent;
import com.sep.vox.application.port.input.command.CreateSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.schooluser.CreateSchoolUserUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.PasswordSetUpTokenPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.output.GeneratedPasswordSetUpToken;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.PasswordSetUpTokenRepository;
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
    private UserRoleQueryRepository userRoleQueryRepository;
    private SchoolRepository schoolRepository;
    private PasswordSetUpTokenPort passwordSetUpTokenPort;
    private PasswordSetUpTokenRepository passwordSetUpTokenRepository;
    private EventPublisherPort eventPublisherPort;
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
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        passwordSetUpTokenPort = mock(PasswordSetUpTokenPort.class);
        passwordSetUpTokenRepository = mock(PasswordSetUpTokenRepository.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        createSchoolUserUseCase = new CreateSchoolUserUseCase(
            userContextPort, userRepository, roleRepository,
            userRoleRepository, schoolUserRepository, userRoleQueryRepository,
            schoolRepository, passwordSetUpTokenPort, passwordSetUpTokenRepository, eventPublisherPort
        );
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school(schoolId, "Trường A")));
        when(passwordSetUpTokenPort.generateToken()).thenReturn(new GeneratedPasswordSetUpToken("raw-token", "hashed-token"));
        when(passwordSetUpTokenRepository.save(any(PasswordSetUpToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void create_student_without_student_id_should_succeed() {
        var caller = callerUser(callerId, schoolId);
        var savedUser = savedUser(schoolId);
        var studentRole = role("STUDENT");
        var command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT", null, null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.findByEmail("student@school.edu.vn")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0987654321")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(new UserRole(savedUser.getId(), studentRole.getId(), OffsetDateTime.now()));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(savedUser.getId())).thenReturn(List.of(roleInfo("STUDENT")));

        var result = createSchoolUserUseCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.roleCode()).isEqualTo("STUDENT");
        assertThat(result.studentId()).isNull();
        verify(schoolUserRepository, never()).save(any(SchoolUser.class));
        verify(eventPublisherPort).publish(any(SchoolUserPasswordSetUpEmailRequestedEvent.class));
    }

    @Test
    void create_student_with_student_id_should_save_school_user() {
        var caller = callerUser(callerId, schoolId);
        var savedUser = savedUser(schoolId);
        var studentRole = role("STUDENT");
        var command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT", "STU-001", null, null
        );
        var schoolUser = new SchoolUser("STU-001", schoolId, savedUser.getId(), OffsetDateTime.now(), OffsetDateTime.now().plusYears(100));

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.findByEmail("student@school.edu.vn")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0987654321")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(new UserRole(savedUser.getId(), studentRole.getId(), OffsetDateTime.now()));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenReturn(schoolUser);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(savedUser.getId())).thenReturn(List.of(roleInfo("STUDENT")));

        var result = createSchoolUserUseCase.execute(command);

        assertThat(result.studentId()).isEqualTo("STU-001");
        verify(schoolUserRepository).save(any(SchoolUser.class));
        verify(eventPublisherPort).publish(any(SchoolUserPasswordSetUpEmailRequestedEvent.class));
    }

    @Test
    void create_teacher_should_not_save_school_user() {
        var caller = callerUser(callerId, schoolId);
        var savedUser = savedUser(schoolId);
        var teacherRole = role("TEACHER");
        var command = new CreateSchoolUserCommand(
            schoolId, "teacher@school.edu.vn", "0987654322",
            "Tran Thi B", LocalDate.of(1985, 3, 20), "456 Street", "TEACHER", null, null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("TEACHER")).thenReturn(Optional.of(teacherRole));
        when(userRepository.findByEmail("teacher@school.edu.vn")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0987654322")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(new UserRole(savedUser.getId(), teacherRole.getId(), OffsetDateTime.now()));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(savedUser.getId())).thenReturn(List.of(roleInfo("TEACHER")));

        var result = createSchoolUserUseCase.execute(command);

        assertThat(result.roleCode()).isEqualTo("TEACHER");
        verify(schoolUserRepository, never()).save(any(SchoolUser.class));
        verify(eventPublisherPort).publish(any(SchoolUserPasswordSetUpEmailRequestedEvent.class));
    }

    @Test
    void create_should_throw_when_caller_belongs_to_different_school() {
        var otherSchoolId = UUID.randomUUID();
        var caller = callerUser(callerId, otherSchoolId);
        var command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT", null, null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("SCHOOL_ADMIN")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> createSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void create_should_throw_when_role_code_is_invalid() {
        var caller = callerUser(callerId, schoolId);
        var command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "SCHOOL_ADMIN", null, null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));

        assertThrows(IllegalArgumentException.class, () -> createSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void create_should_throw_when_role_not_found_in_repository() {
        var caller = callerUser(callerId, schoolId);
        var command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT", null, null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> createSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void create_should_throw_when_caller_is_inactive() {
        var caller = callerUser(callerId, schoolId, UserStatus.INACTIVE);
        var command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT", null, null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));

        assertThrows(UnauthorizedException.class, () -> createSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void create_should_throw_when_email_already_exists() {
        var caller = callerUser(callerId, schoolId);
        var studentRole = role("STUDENT");
        var command = new CreateSchoolUserCommand(
            schoolId, "student@school.edu.vn", "0987654321",
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT", null, null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
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
            "Nguyen Van A", LocalDate.of(2005, 1, 15), "123 Street", "STUDENT", null, null, null
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
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
        var now = OffsetDateTime.now();
        return new User(id, new Email("admin@school.edu.vn"), "hash",
            new Phone("0900000000"), new FullName("Admin User"), null,
            new DateOfBirth(LocalDate.of(1980, 1, 1)), "Admin Street", null,
            status, now, now, id, id, userSchoolId);
    }

    private User savedUser(UUID userSchoolId) {
        var now = OffsetDateTime.now();
        var id = UUID.randomUUID();
        return new User(id, new Email("student@school.edu.vn"), "__PASSWORD_NOT_SET__",
            new Phone("0987654321"), new FullName("Nguyen Van A"), null,
            new DateOfBirth(LocalDate.of(2005, 1, 15)), "123 Street", null,
            UserStatus.INACTIVE, now, now, callerId, callerId, userSchoolId);
    }

    private Role role(String code) {
        var now = OffsetDateTime.now();
        var systemId = UUID.randomUUID();
        return new Role(UUID.randomUUID(), new RoleCode(code), code, now, now, systemId, systemId);
    }

    private School school(UUID id, String name) {
        var school = new School();
        school.setId(id);
        school.setName(name);
        return school;
    }

    private UserRoleInfo roleInfo(String code) {
        return new UserRoleInfo(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now(), code, code);
    }
}