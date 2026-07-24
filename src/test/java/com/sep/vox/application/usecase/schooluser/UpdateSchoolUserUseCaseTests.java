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
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.schooluser.UpdateSchoolUserUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

public class UpdateSchoolUserUseCaseTests {

    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private SchoolUserRepository schoolUserRepository;
    private UpdateSchoolUserUseCase updateSchoolUserUseCase;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        updateSchoolUserUseCase = new UpdateSchoolUserUseCase(userContextPort, userRepository, schoolUserRepository);
    }

    @Test
    void should_throw_illegal_argument_when_no_field_provided() {
        var command = command(null, false, null, false, null, false, null, false);

        assertThrows(IllegalArgumentException.class, () -> updateSchoolUserUseCase.execute(command));
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void should_update_full_name_only() {
        wireCallerAndTarget(schoolId);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var command = command("  Nguyen   Van  Updated ", true, null, false, null, false, null, false);

        var result = updateSchoolUserUseCase.execute(command);

        assertThat(result.id()).isEqualTo(targetId);
        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getFullName().value()).isEqualTo("Nguyen Van Updated");
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(callerId);
    }

    @Test
    void should_update_phone_when_phone_not_taken() {
        wireCallerAndTarget(schoolId);
        when(userRepository.findByPhone("0912345678")).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var command = command(null, false, "0912345678", true, null, false, null, false);

        var result = updateSchoolUserUseCase.execute(command);

        assertThat(result.id()).isEqualTo(targetId);
        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPhone().value()).isEqualTo("0912345678");
    }

    @Test
    void should_update_address_to_null() {
        wireCallerAndTarget(schoolId);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var command = command(null, false, null, false, null, true, null, false);

        updateSchoolUserUseCase.execute(command);

        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getAddress()).isNull();
    }

    @Test
    void should_update_date_of_birth() {
        wireCallerAndTarget(schoolId);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var newDob = LocalDate.of(2006, 6, 15);
        var command = command(null, false, null, false, null, false, newDob, true);

        updateSchoolUserUseCase.execute(command);

        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getDateOfBirth().value()).isEqualTo(newDob);
    }

    @Test
    void should_throw_duplicated_when_phone_belongs_to_another_user() {
        wireCallerAndTarget(schoolId);
        when(userRepository.findByPhone("0912345678"))
            .thenReturn(Optional.of(user(UUID.randomUUID(), "0912345678")));
        var command = command(null, false, "0912345678", true, null, false, null, false);

        assertThrows(DuplicatedException.class, () -> updateSchoolUserUseCase.execute(command));
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void should_not_treat_phone_as_duplicated_when_it_belongs_to_target() {
        wireCallerAndTarget(schoolId);
        when(userRepository.findByPhone("0987654321"))
            .thenReturn(Optional.of(user(targetId, "0987654321")));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var command = command(null, false, "0987654321", true, null, false, null, false);

        var result = updateSchoolUserUseCase.execute(command);

        assertThat(result.id()).isEqualTo(targetId);
        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void should_throw_duplicated_when_save_violates_unique_constraint() {
        wireCallerAndTarget(schoolId);
        when(userRepository.findByPhone("0912345678")).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(User.class)))
            .thenThrow(new DataIntegrityViolationException("unique constraint idx_user_phone violated"));
        var command = command(null, false, "0912345678", true, null, false, null, false);

        assertThrows(DuplicatedException.class, () -> updateSchoolUserUseCase.execute(command));
    }

    @Test
    void should_throw_illegal_argument_when_caller_in_different_school() {
        var otherSchoolId = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(user(callerId, "0900000000")));
        when(schoolUserRepository.findByUserId(callerId)).thenReturn(Optional.of(schoolUser(otherSchoolId, callerId)));
        var command = command("Nguyen Van Updated", true, null, false, null, false, null, false);

        assertThrows(IllegalArgumentException.class, () -> updateSchoolUserUseCase.execute(command));
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void should_throw_not_found_when_target_in_different_school() {
        var otherSchoolId = UUID.randomUUID();
        wireCallerAndTarget(otherSchoolId);
        var command = command("Nguyen Van Updated", true, null, false, null, false, null, false);

        assertThrows(NotFoundException.class, () -> updateSchoolUserUseCase.execute(command));
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void should_throw_not_found_when_caller_inactive() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.empty());
        var command = command("Nguyen Van Updated", true, null, false, null, false, null, false);

        assertThrows(NotFoundException.class, () -> updateSchoolUserUseCase.execute(command));
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void should_throw_not_found_when_target_not_found() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(user(callerId, "0900000000")));
        when(schoolUserRepository.findByUserId(callerId)).thenReturn(Optional.of(schoolUser(schoolId, callerId)));
        when(userRepository.findByIdForUpdate(targetId)).thenReturn(Optional.empty());
        var command = command("Nguyen Van Updated", true, null, false, null, false, null, false);

        assertThrows(NotFoundException.class, () -> updateSchoolUserUseCase.execute(command));
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    private void wireCallerAndTarget(UUID targetSchoolId) {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(callerId);
        when(userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)).thenReturn(Optional.of(user(callerId, "0900000000")));
        when(schoolUserRepository.findByUserId(callerId)).thenReturn(Optional.of(schoolUser(schoolId, callerId)));
        when(userRepository.findByIdForUpdate(targetId)).thenReturn(Optional.of(user(targetId, "0987654321")));
        when(schoolUserRepository.findByUserId(targetId)).thenReturn(Optional.of(schoolUser(targetSchoolId, targetId)));
    }

    private UpdateSchoolUserCommand command(
            String fullName, boolean fullNameProvided,
            String phone, boolean phoneProvided,
            String address, boolean addressProvided,
            LocalDate dateOfBirth, boolean dateOfBirthProvided) {
        return new UpdateSchoolUserCommand(
            schoolId, targetId,
            fullName, fullNameProvided,
            phone, phoneProvided,
            address, addressProvided,
            dateOfBirth, dateOfBirthProvided
        );
    }

    private User user(UUID id, String phone) {
        var now = OffsetDateTime.now();
        return new User(id, new Email("user@school.edu.vn"), "hash",
            new Phone(phone), new FullName("Nguyen Van A"), null,
            new DateOfBirth(LocalDate.of(2005, 1, 15)), "123 Street", null,
            UserStatus.ACTIVE, now, now, id, id);
    }

    private SchoolUser schoolUser(UUID userSchoolId, UUID userId) {
        var now = OffsetDateTime.now();
        return new SchoolUser(userSchoolId, userId, now, now.plusYears(100));
    }
}
