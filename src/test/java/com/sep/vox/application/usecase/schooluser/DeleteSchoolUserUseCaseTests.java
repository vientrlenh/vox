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
import com.sep.vox.application.port.input.command.DeleteSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.schooluser.DeleteSchoolUserUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

public class DeleteSchoolUserUseCaseTests {

    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private SchoolUserRepository schoolUserRepository;
    private DeleteSchoolUserUseCase deleteSchoolUserUseCase;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        deleteSchoolUserUseCase = new DeleteSchoolUserUseCase(userContextPort, userRepository, schoolUserRepository);
    }

    @Test
    void delete_should_soft_delete_user() {
        var targetId = UUID.randomUUID();
        var caller = user(callerId, schoolId, UserStatus.ACTIVE);
        var target = user(targetId, schoolId, UserStatus.ACTIVE);
        var command = new DeleteSchoolUserCommand(schoolId, targetId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
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
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));

        assertThrows(IllegalArgumentException.class, () -> deleteSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void delete_should_throw_when_target_user_not_found() {
        var targetId = UUID.randomUUID();
        var caller = user(callerId, schoolId, UserStatus.ACTIVE);
        var command = new DeleteSchoolUserCommand(schoolId, targetId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
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
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThrows(NotFoundException.class, () -> deleteSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void delete_should_soft_delete_even_when_target_inactive() {
        var targetId = UUID.randomUUID();
        var caller = user(callerId, schoolId, UserStatus.ACTIVE);
        var target = user(targetId, schoolId, UserStatus.INACTIVE);
        var command = new DeleteSchoolUserCommand(schoolId, targetId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        deleteSchoolUserUseCase.execute(command);

        verify(userRepository).save(argThat(u -> u.getStatus() == UserStatus.DISABLED));
    }

    @Test
    void delete_should_reject_self_delete() {
        var caller = user(callerId, schoolId, UserStatus.ACTIVE);
        var command = new DeleteSchoolUserCommand(schoolId, callerId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));

        assertThrows(IllegalArgumentException.class, () -> deleteSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void delete_should_throw_when_caller_inactive() {
        var command = new DeleteSchoolUserCommand(schoolId, UUID.randomUUID());

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> deleteSchoolUserUseCase.execute(command));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void delete_should_end_membership() {
        var targetId = UUID.randomUUID();
        var caller = user(callerId, schoolId, UserStatus.ACTIVE);
        var target = user(targetId, schoolId, UserStatus.ACTIVE);
        var command = new DeleteSchoolUserCommand(schoolId, targetId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(caller));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolUserRepository.save(any(SchoolUser.class))).thenAnswer(inv -> inv.getArgument(0));

        deleteSchoolUserUseCase.execute(command);

        // Membership bị kết thúc: endDate không còn ở tương lai xa (100 năm) mà <= hiện tại + biên nhỏ
        verify(schoolUserRepository).save(argThat(su ->
            su.getEndDate() != null && su.getEndDate().isBefore(OffsetDateTime.now().plusMinutes(1))));
    }

    private User user(UUID id, UUID userSchoolId, UserStatus status) {
        var now = OffsetDateTime.now();
        when(schoolUserRepository.findByUserId(id)).thenReturn(Optional.of(
            new SchoolUser(userSchoolId, id, now, now.plusYears(100))
        ));
        return new User(id, new Email("user@school.edu.vn"), "hash",
            new Phone("0987654321"), new FullName("Nguyen Van A"), null,
            new DateOfBirth(LocalDate.of(2000, 1, 1)), "123 Street", null,
            status, now, now, id, id);
    }
}
