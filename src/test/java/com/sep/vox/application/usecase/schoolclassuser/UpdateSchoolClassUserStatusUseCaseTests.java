package com.sep.vox.application.usecase.schoolclassuser;

import com.sep.vox.application.usecase.TestSchoolUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateSchoolClassUserStatusCommand;
import com.sep.vox.application.port.input.usecase.schoolclassuser.UpdateSchoolClassUserStatusUseCase;
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

class UpdateSchoolClassUserStatusUseCaseTests {

    private SchoolClassUserRepository schoolClassUserRepository;
    private SchoolClassRepository schoolClassRepository;
    private SchoolRepository schoolRepository;
    private UserRepository userRepository;
    private UserContextPort userContextPort;
    private UpdateSchoolClassUserStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userRepository = mock(UserRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new UpdateSchoolClassUserStatusUseCase(
            schoolClassUserRepository,
            schoolClassRepository,
            schoolRepository,
            userRepository,
            userContextPort,
            TestSchoolUserRepository.create()
        );
    }

    @Test
    void update_should_activate_inactive_membership_and_clear_left_at() {
        var currentUserId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var membership = membership(targetUserId, classId, false, OffsetDateTime.now().minusDays(1), currentUserId);
        mockValidContext(currentUserId, schoolId, classId, targetUserId);
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(targetUserId, classId)).thenReturn(Optional.of(membership));

        var response = useCase.execute(new UpdateSchoolClassUserStatusCommand(schoolId, classId, targetUserId, true));

        assertThat(response.schoolClassId()).isEqualTo(classId);
        var captor = ArgumentCaptor.forClass(SchoolClassUser.class);
        verify(schoolClassUserRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(captor.getValue().getLeftAt()).isNull();
    }

    @Test
    void update_should_deactivate_active_membership() {
        var currentUserId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var membership = membership(targetUserId, classId, true, null, currentUserId);
        mockValidContext(currentUserId, schoolId, classId, targetUserId);
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(targetUserId, classId)).thenReturn(Optional.of(membership));

        var response = useCase.execute(new UpdateSchoolClassUserStatusCommand(schoolId, classId, targetUserId, false));

        assertThat(response.schoolClassId()).isEqualTo(classId);
        var captor = ArgumentCaptor.forClass(SchoolClassUser.class);
        verify(schoolClassUserRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
        assertThat(captor.getValue().getLeftAt()).isNotNull();
    }

    @Test
    void update_should_be_idempotent_when_status_is_already_inactive() {
        var currentUserId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var leftAt = OffsetDateTime.now().minusDays(1);
        var membership = membership(targetUserId, classId, false, leftAt, currentUserId);
        mockValidContext(currentUserId, schoolId, classId, targetUserId);
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(targetUserId, classId)).thenReturn(Optional.of(membership));

        var response = useCase.execute(new UpdateSchoolClassUserStatusCommand(schoolId, classId, targetUserId, false));

        assertThat(response.schoolClassId()).isEqualTo(classId);
        assertThat(membership.getLeftAt()).isEqualTo(leftAt);
        verify(schoolClassUserRepository, never()).save(any());
    }

    @Test
    void update_should_throw_when_membership_not_found() {
        var currentUserId = UUID.randomUUID();
        var targetUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        mockValidContext(currentUserId, schoolId, classId, targetUserId);
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(targetUserId, classId)).thenReturn(Optional.empty());

        assertThrows(
            NotFoundException.class,
            () -> useCase.execute(new UpdateSchoolClassUserStatusCommand(schoolId, classId, targetUserId, true))
        );
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
        var user = new User();
        user.setId(id);
        TestSchoolUserRepository.remember(id, schoolId);
        user.setStatus(UserStatus.ACTIVE);
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
