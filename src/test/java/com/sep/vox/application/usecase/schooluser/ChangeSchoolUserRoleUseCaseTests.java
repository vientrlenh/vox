package com.sep.vox.application.usecase.schooluser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ChangeSchoolUserRoleCommand;
import com.sep.vox.application.port.input.usecase.schooluser.ChangeSchoolUserRoleUseCase;
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

public class ChangeSchoolUserRoleUseCaseTests {

    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private UserRoleRepository userRoleRepository;
    private SchoolUserRepository schoolUserRepository;
    private ChangeSchoolUserRoleUseCase changeSchoolUserRoleUseCase;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        changeSchoolUserRoleUseCase = new ChangeSchoolUserRoleUseCase(
            userContextPort, userRepository, roleRepository, userRoleRepository, schoolUserRepository
        );
    }

    @Test
    void change_role_student_to_teacher_should_update_role_and_clear_student_id() {
        var targetId = UUID.randomUUID();
        var caller = user(callerId, schoolId);
        var target = user(targetId, schoolId);
        var studentRoleId = UUID.randomUUID();
        var teacherRoleId = UUID.randomUUID();
        var studentRole = role(studentRoleId, "STUDENT");
        var teacherRole = role(teacherRoleId, "TEACHER");
        var existingUserRole = new UserRole(1L, targetId, studentRoleId, OffsetDateTime.now());
        var command = new ChangeSchoolUserRoleCommand(schoolId, targetId, "TEACHER");
        var existingSchoolUser = new SchoolUser(targetId, schoolId, "STU-001", OffsetDateTime.now(), callerId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRoleRepository.findByUserId(targetId)).thenReturn(List.of(existingUserRole));
        when(roleRepository.findById(studentRoleId)).thenReturn(Optional.of(studentRole));
        when(roleRepository.findByCode("TEACHER")).thenReturn(Optional.of(teacherRole));
        when(schoolUserRepository.findByUserId(targetId)).thenReturn(Optional.of(existingSchoolUser));
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = changeSchoolUserRoleUseCase.execute(command);

        assertThat(result).isNull();
        verify(userRoleRepository).save(argThat(ur -> ur.getId() == 1L && ur.getRoleId().equals(teacherRoleId)));
        verify(schoolUserRepository).save(argThat(su -> su.getUserId().equals(targetId) && su.getStudentId() == null));
    }

    @Test
    void change_role_same_role_should_return_early_without_updates() {
        var targetId = UUID.randomUUID();
        var caller = user(callerId, schoolId);
        var target = user(targetId, schoolId);
        var studentRoleId = UUID.randomUUID();
        var studentRole = role(studentRoleId, "STUDENT");
        var existingUserRole = new UserRole(1L, targetId, studentRoleId, OffsetDateTime.now());
        var command = new ChangeSchoolUserRoleCommand(schoolId, targetId, "STUDENT");

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRoleRepository.findByUserId(targetId)).thenReturn(List.of(existingUserRole));
        when(roleRepository.findById(studentRoleId)).thenReturn(Optional.of(studentRole));

        var result = changeSchoolUserRoleUseCase.execute(command);

        assertThat(result).isNull();
        verify(userRoleRepository, never()).save(any(UserRole.class));
        verify(schoolUserRepository, never()).save(any(SchoolUser.class));
    }

    @Test
    void change_role_should_throw_when_caller_belongs_to_different_school() {
        var caller = user(callerId, UUID.randomUUID());
        var command = new ChangeSchoolUserRoleCommand(schoolId, UUID.randomUUID(), "TEACHER");

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));

        assertThrows(IllegalArgumentException.class, () -> changeSchoolUserRoleUseCase.execute(command));
        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    void change_role_should_throw_when_new_role_code_is_invalid() {
        var targetId = UUID.randomUUID();
        var caller = user(callerId, schoolId);
        var target = user(targetId, schoolId);
        var command = new ChangeSchoolUserRoleCommand(schoolId, targetId, "SCHOOL_ADMIN");

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThrows(IllegalArgumentException.class, () -> changeSchoolUserRoleUseCase.execute(command));
        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    void change_role_should_throw_when_target_user_not_found() {
        var targetId = UUID.randomUUID();
        var caller = user(callerId, schoolId);
        var command = new ChangeSchoolUserRoleCommand(schoolId, targetId, "TEACHER");

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> changeSchoolUserRoleUseCase.execute(command));
        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    private User user(UUID id, UUID userSchoolId) {
        var now = OffsetDateTime.now();
        return new User(id, new Email("user@school.edu.vn"), "hash",
            new Phone("0987654321"), new FullName("Nguyen Van A"), null,
            new DateOfBirth(LocalDate.of(2000, 1, 1)), "123 Street",
            UserStatus.ACTIVE, now, now, id, id, userSchoolId);
    }

    private Role role(UUID id, String code) {
        var now = OffsetDateTime.now();
        var systemId = UUID.randomUUID();
        return new Role(id, new RoleCode(code), code, now, now, systemId, systemId);
    }
}
