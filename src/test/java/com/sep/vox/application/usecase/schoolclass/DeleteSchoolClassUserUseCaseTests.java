package com.sep.vox.application.usecase.schoolclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteSchoolClassUserCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.DeleteSchoolClassUserUseCase;
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

class DeleteSchoolClassUserUseCaseTests {

    private SchoolClassUserRepository schoolClassUserRepository;
    private SchoolClassRepository schoolClassRepository;
    private SchoolRepository schoolRepository;
    private UserRepository userRepository;
    private UserContextPort userContextPort;
    private DeleteSchoolClassUserUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userRepository = mock(UserRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new DeleteSchoolClassUserUseCase(
            schoolClassUserRepository,
            schoolClassRepository,
            schoolRepository,
            userRepository,
            userContextPort
        );
    }

    @Test
    void delete_should_soft_delete_active_membership() {
        var currentUserId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var membership = membership(targetUserId, classId, true, null, currentUserId);
        mockValidContext(currentUserId, schoolId, classId, targetUserId);
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(targetUserId, classId)).thenReturn(Optional.of(membership));

        var response = useCase.execute(new DeleteSchoolClassUserCommand(schoolId, classId, targetUserId));

        assertThat(response.schoolClassId()).isEqualTo(classId);
        var captor = ArgumentCaptor.forClass(SchoolClassUser.class);
        verify(schoolClassUserRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
        assertThat(captor.getValue().getLeftAt()).isNotNull();
        assertThat(captor.getValue().getUserId()).isEqualTo(targetUserId);
        assertThat(captor.getValue().getSchoolClassId()).isEqualTo(classId);
        assertThat(captor.getValue().getAssignedBy()).isEqualTo(currentUserId);
    }

    @Test
    void delete_should_be_idempotent_when_membership_is_inactive() {
        var currentUserId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var leftAt = OffsetDateTime.now().minusDays(1);
        var membership = membership(targetUserId, classId, false, leftAt, currentUserId);
        mockValidContext(currentUserId, schoolId, classId, targetUserId);
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(targetUserId, classId)).thenReturn(Optional.of(membership));

        var response = useCase.execute(new DeleteSchoolClassUserCommand(schoolId, classId, targetUserId));

        assertThat(response.schoolClassId()).isEqualTo(classId);
        assertThat(membership.getLeftAt()).isEqualTo(leftAt);
        verify(schoolClassUserRepository, never()).save(any());
    }

    @Test
    void delete_should_throw_when_membership_not_found() {
        var currentUserId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        mockValidContext(currentUserId, schoolId, classId, targetUserId);
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(targetUserId, classId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(new DeleteSchoolClassUserCommand(schoolId, classId, targetUserId)));
    }

    @Test
    void delete_should_throw_when_class_belongs_to_other_school() {
        var currentUserId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(activeSchoolClass(classId, UUID.randomUUID())));

        assertThrows(NotFoundException.class, () -> useCase.execute(new DeleteSchoolClassUserCommand(schoolId, classId, targetUserId)));

        verifyNoInteractions(schoolClassUserRepository);
    }

    @Test
    void delete_should_throw_when_target_user_is_inactive() {
        var currentUserId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(activeSchoolClass(classId, schoolId)));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user(targetUserId, schoolId, UserStatus.INACTIVE)));

        assertThrows(IllegalStateException.class, () -> useCase.execute(new DeleteSchoolClassUserCommand(schoolId, classId, targetUserId)));

        verifyNoInteractions(schoolClassUserRepository);
    }

    @Test
    void delete_should_throw_when_current_user_school_mismatches_request() {
        var currentUserId = UUID.randomUUID();
        var command = new DeleteSchoolClassUserCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

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

    private static SchoolClassUser membership(UUID userId, UUID classId, boolean isActive, OffsetDateTime leftAt, UUID assignedBy) {
        var membership = new SchoolClassUser(userId, classId, isActive, OffsetDateTime.now().minusDays(2), leftAt, assignedBy);
        membership.setId(UUID.randomUUID());
        return membership;
    }

    private static User activeUser(UUID id, UUID schoolId) {
        return user(id, schoolId, UserStatus.ACTIVE);
    }

    private static User user(UUID id, UUID schoolId, UserStatus status) {
        var user = new User();
        user.setId(id);
        user.setSchoolId(schoolId);
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
