package com.sep.vox.application.usecase.schooluser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.ViewSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUserUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

public class ViewSchoolUserUseCaseTests {

    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private SchoolUserRepository schoolUserRepository;
    private ViewSchoolUserUseCase viewSchoolUserUseCase;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        viewSchoolUserUseCase = new ViewSchoolUserUseCase(
            userContextPort, userRepository, userRoleQueryRepository, schoolUserRepository
        );
    }

    @Test
    void view_should_return_user_with_role_and_student_id() {
        var targetId = UUID.randomUUID();
        var caller = user(callerId, schoolId);
        var target = user(targetId, schoolId);
        var schoolUserId = UUID.randomUUID();
        var schoolUser = new SchoolUser(schoolUserId, schoolId, targetId, OffsetDateTime.now(), OffsetDateTime.now().plusYears(100));
        var command = new ViewSchoolUserCommand(schoolId, targetId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(targetId)).thenReturn(List.of(roleInfo("STUDENT")));
        when(schoolUserRepository.findByUserId(targetId)).thenReturn(Optional.of(schoolUser));

        var result = viewSchoolUserUseCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(schoolUserId);
        assertThat(result.userId()).isEqualTo(targetId);
    }

    @Test
    void view_should_return_teacher_role() {
        var targetId = UUID.randomUUID();
        var caller = user(callerId, schoolId);
        var target = user(targetId, schoolId);
        var command = new ViewSchoolUserCommand(schoolId, targetId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(targetId)).thenReturn(List.of(roleInfo("TEACHER")));

        var result = viewSchoolUserUseCase.execute(command);

        assertThat(result).isNotNull();
    }

    @Test
    void view_should_throw_when_caller_belongs_to_different_school() {
        var caller = user(callerId, UUID.randomUUID());
        var command = new ViewSchoolUserCommand(schoolId, UUID.randomUUID());

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));

        assertThrows(IllegalArgumentException.class, () -> viewSchoolUserUseCase.execute(command));
    }

    @Test
    void view_should_throw_when_target_user_not_found() {
        var targetId = UUID.randomUUID();
        var caller = user(callerId, schoolId);
        var command = new ViewSchoolUserCommand(schoolId, targetId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> viewSchoolUserUseCase.execute(command));
    }

    @Test
    void view_should_throw_when_target_user_belongs_to_different_school() {
        var targetId = UUID.randomUUID();
        var caller = user(callerId, schoolId);
        var target = user(targetId, UUID.randomUUID());
        var command = new ViewSchoolUserCommand(schoolId, targetId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThrows(NotFoundException.class, () -> viewSchoolUserUseCase.execute(command));
    }

    @Test
    void view_should_throw_when_target_user_is_inactive() {
        var targetId = UUID.randomUUID();
        var caller = user(callerId, schoolId);
        var target = user(targetId, schoolId, UserStatus.INACTIVE);
        var command = new ViewSchoolUserCommand(schoolId, targetId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThrows(UnauthorizedException.class, () -> viewSchoolUserUseCase.execute(command));
    }

    private User user(UUID id, UUID userSchoolId) {
        return user(id, userSchoolId, UserStatus.ACTIVE);
    }

    private User user(UUID id, UUID userSchoolId, UserStatus status) {
        var now = OffsetDateTime.now();
        when(schoolUserRepository.findByUserId(id)).thenReturn(Optional.of(
            new SchoolUser(UUID.randomUUID(), userSchoolId, id, now, now.plusYears(100))
        ));
        return new User(id, new Email("user@school.edu.vn"), "hash",
            new Phone("0987654321"), new FullName("Nguyen Van A"), null,
            new DateOfBirth(LocalDate.of(2000, 1, 1)), "123 Street", null,
            status, now, now, id, id);
    }

    private UserRoleInfo roleInfo(String code) {
        return new UserRoleInfo(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now(), code, code);
    }
}
