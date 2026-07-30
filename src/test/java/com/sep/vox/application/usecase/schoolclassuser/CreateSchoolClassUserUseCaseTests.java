package com.sep.vox.application.usecase.schoolclassuser;

import com.sep.vox.application.usecase.TestSchoolUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateSchoolClassUserCommand;
import com.sep.vox.application.port.input.usecase.schoolclassuser.CreateSchoolClassUserUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.ClassCode;

class CreateSchoolClassUserUseCaseTests {

    private SchoolClassUserRepository schoolClassUserRepository;
    private SchoolClassRepository schoolClassRepository;
    private SchoolRepository schoolRepository;
    private UserRepository userRepository;
    private UserContextPort userContextPort;
    private CreateSchoolClassUserUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userRepository = mock(UserRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new CreateSchoolClassUserUseCase(
            schoolClassUserRepository,
            schoolClassRepository,
            schoolRepository,
            userRepository,
            userContextPort,
            TestSchoolUserRepository.create()
        );
    }

    @Test
    void create_should_save_active_membership_for_current_users_school() {
        var currentUserId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var savedId = UUID.randomUUID();
        var command = new CreateSchoolClassUserCommand(schoolId, classId, targetUserId);

        mockValidContext(currentUserId, schoolId, classId, targetUserId);
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(targetUserId, classId)).thenReturn(Optional.empty());
        when(schoolClassUserRepository.save(any(SchoolClassUser.class))).thenAnswer(invocation -> {
            var schoolClassUser = invocation.getArgument(0, SchoolClassUser.class);
            schoolClassUser.setId(savedId);
            return schoolClassUser;
        });

        var response = useCase.execute(command);

        assertThat(response.schoolClassUserId()).isEqualTo(savedId);
        var captor = ArgumentCaptor.forClass(SchoolClassUser.class);
        verify(schoolClassUserRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(targetUserId);
        assertThat(captor.getValue().getSchoolClassId()).isEqualTo(classId);
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(captor.getValue().getJoinedAt()).isNotNull();
        assertThat(captor.getValue().getLeftAt()).isNull();
        assertThat(captor.getValue().getAssignedBy()).isEqualTo(currentUserId);
    }

    @Test
    void create_should_throw_when_membership_already_exists() {
        var currentUserId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var command = new CreateSchoolClassUserCommand(schoolId, classId, targetUserId);

        mockValidContext(currentUserId, schoolId, classId, targetUserId);
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(targetUserId, classId))
            .thenReturn(Optional.of(new SchoolClassUser(targetUserId, classId, true, Instant.now(), null, currentUserId)));

        assertThrows(DuplicatedException.class, () -> useCase.execute(command));

        verify(schoolClassUserRepository, never()).save(any());
    }

    @Test
    void create_should_reactivate_when_membership_inactive() {
        var currentUserId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var membershipId = UUID.randomUUID();
        var command = new CreateSchoolClassUserCommand(schoolId, classId, targetUserId);

        mockValidContext(currentUserId, schoolId, classId, targetUserId);
        var leftMembership = new SchoolClassUser(
            membershipId, targetUserId, classId, false, Instant.now().minus(5, ChronoUnit.DAYS),
            Instant.now().minus(1, ChronoUnit.DAYS), UUID.randomUUID());
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(targetUserId, classId))
            .thenReturn(Optional.of(leftMembership));
        when(schoolClassUserRepository.save(any(SchoolClassUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = useCase.execute(command);

        assertThat(response.schoolClassUserId()).isEqualTo(membershipId);
        var captor = ArgumentCaptor.forClass(SchoolClassUser.class);
        verify(schoolClassUserRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(membershipId);
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(captor.getValue().getLeftAt()).isNull();
        assertThat(captor.getValue().getAssignedBy()).isEqualTo(currentUserId);
    }

    @Test
    void create_should_throw_duplicated_when_save_hits_unique_violation() {
        var currentUserId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var command = new CreateSchoolClassUserCommand(schoolId, classId, targetUserId);

        mockValidContext(currentUserId, schoolId, classId, targetUserId);
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(targetUserId, classId)).thenReturn(Optional.empty());
        when(schoolClassUserRepository.save(any(SchoolClassUser.class)))
            .thenThrow(new org.springframework.dao.DataIntegrityViolationException("unique violation"));

        assertThrows(DuplicatedException.class, () -> useCase.execute(command));
    }

    @Test
    void create_should_throw_when_class_not_found() {
        var currentUserId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var command = new CreateSchoolClassUserCommand(schoolId, classId, targetUserId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(command));

        verifyNoInteractions(schoolClassUserRepository);
    }

    @Test
    void create_should_throw_when_class_belongs_to_other_school() {
        var currentUserId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var command = new CreateSchoolClassUserCommand(schoolId, classId, targetUserId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(activeSchoolClass(classId, UUID.randomUUID())));

        assertThrows(NotFoundException.class, () -> useCase.execute(command));

        verifyNoInteractions(schoolClassUserRepository);
    }

    @Test
    void create_should_throw_when_target_user_not_found() {
        var currentUserId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var command = new CreateSchoolClassUserCommand(schoolId, classId, targetUserId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(activeSchoolClass(classId, schoolId)));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(command));

        verifyNoInteractions(schoolClassUserRepository);
    }

    @Test
    void create_should_throw_when_target_user_is_inactive() {
        var currentUserId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var command = new CreateSchoolClassUserCommand(schoolId, classId, targetUserId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(activeSchoolClass(classId, schoolId)));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user(targetUserId, schoolId, UserStatus.INACTIVE)));

        assertThrows(IllegalStateException.class, () -> useCase.execute(command));

        verifyNoInteractions(schoolClassUserRepository);
    }

    @Test
    void create_should_throw_when_target_user_belongs_to_other_school() {
        var currentUserId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var command = new CreateSchoolClassUserCommand(schoolId, classId, targetUserId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(activeSchoolClass(classId, schoolId)));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(activeUser(targetUserId, UUID.randomUUID())));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));

        verifyNoInteractions(schoolClassUserRepository);
    }

    @Test
    void create_should_throw_when_current_user_is_inactive() {
        var currentUserId = UUID.randomUUID();
        var command = new CreateSchoolClassUserCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(user(currentUserId, UUID.randomUUID(), UserStatus.INACTIVE)));

        assertThrows(IllegalStateException.class, () -> useCase.execute(command));

        verifyNoInteractions(schoolRepository, schoolClassRepository, schoolClassUserRepository);
    }

    @Test
    void create_should_throw_when_current_user_has_no_school() {
        var currentUserId = UUID.randomUUID();
        var command = new CreateSchoolClassUserCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, null)));

        assertThrows(IllegalStateException.class, () -> useCase.execute(command));

        verifyNoInteractions(schoolRepository, schoolClassRepository, schoolClassUserRepository);
    }

    @Test
    void create_should_throw_when_requested_school_differs_from_current_user_school() {
        var currentUserId = UUID.randomUUID();
        var command = new CreateSchoolClassUserCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, UUID.randomUUID())));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));

        verifyNoInteractions(schoolRepository, schoolClassRepository, schoolClassUserRepository);
    }

    private void mockValidContext(UUID currentUserId, UUID schoolId, UUID classId, UUID targetUserId) {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(activeSchoolClass(classId, schoolId)));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(activeUser(targetUserId, schoolId)));
    }

    private static User activeUser(UUID id, UUID schoolId) {
        return user(id, schoolId, UserStatus.ACTIVE);
    }

    private static User user(UUID id, UUID schoolId, UserStatus status) {
        var user = new User();
        user.setId(id);
        TestSchoolUserRepository.remember(id, schoolId);
        user.setStatus(status);
        return user;
    }

    private static School activeSchool(UUID id) {
        var school = new School();
        school.setId(id);
        school.setActive(true);
        return school;
    }

    private static SchoolClass activeSchoolClass(UUID id, UUID schoolId) {
        var schoolClass = new SchoolClass();
        schoolClass.setId(id);
        schoolClass.setSchoolId(schoolId);
        schoolClass.setCode(new ClassCode("ENG-01"));
        schoolClass.setName("English 01");
        schoolClass.setStatus(SchoolClassStatus.ACTIVE);
        return schoolClass;
    }
}
