package com.sep.vox.application.usecase.schooluser;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.schooluser.DeleteSchoolUserUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

public class DeleteSchoolUserUseCaseTests {

    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private DeleteSchoolUserUseCase deleteSchoolUserUseCase;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        deleteSchoolUserUseCase = new DeleteSchoolUserUseCase(userContextPort, userRepository);
    }

    @Test
    void delete_should_soft_delete_user() {
        var targetId = UUID.randomUUID();
        var caller = user(callerId, schoolId, UserStatus.ACTIVE);
        var target = user(targetId, schoolId, UserStatus.ACTIVE);
        var command = new DeleteSchoolUserCommand(schoolId, targetId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        deleteSchoolUserUseCase.execute(command);

        verify(userRepository).save(argThat(u -> u.getStatus() == UserStatus.DISABLED));
    }

    @Test
    void delete_should_throw_when_caller_belongs_to_different_school() {
        var caller = user(callerId, UUID.randomUUID(), UserStatus.ACTIVE);
        var command = new DeleteSchoolUserCommand(schoolId, UUID.randomUUID());

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));

        assertThrows(IllegalArgumentException.class, () -> deleteSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void delete_should_throw_when_target_user_not_found() {
        var targetId = UUID.randomUUID();
        var caller = user(callerId, schoolId, UserStatus.ACTIVE);
        var command = new DeleteSchoolUserCommand(schoolId, targetId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> deleteSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void delete_should_throw_when_target_belongs_to_different_school() {
        var targetId = UUID.randomUUID();
        var caller = user(callerId, schoolId, UserStatus.ACTIVE);
        var target = user(targetId, UUID.randomUUID(), UserStatus.ACTIVE);
        var command = new DeleteSchoolUserCommand(schoolId, targetId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThrows(NotFoundException.class, () -> deleteSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void delete_should_throw_when_target_user_is_inactive() {
        var targetId = UUID.randomUUID();
        var caller = user(callerId, schoolId, UserStatus.ACTIVE);
        var target = user(targetId, schoolId, UserStatus.INACTIVE);
        var command = new DeleteSchoolUserCommand(schoolId, targetId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThrows(UnauthorizedException.class, () -> deleteSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    private User user(UUID id, UUID userSchoolId, UserStatus status) {
        var now = OffsetDateTime.now();
        return new User(id, new Email("user@school.edu.vn"), "hash",
            new Phone("0987654321"), new FullName("Nguyen Van A"), null,
            new DateOfBirth(LocalDate.of(2000, 1, 1)), "123 Street", null,
            status, now, now, id, id, userSchoolId);
    }
}
