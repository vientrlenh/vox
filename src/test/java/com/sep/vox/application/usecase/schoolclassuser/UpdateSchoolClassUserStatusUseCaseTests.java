package com.sep.vox.application.usecase.schoolclassuser;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.schoolclassuser.UpdateSchoolClassUserStatusCommand;
import com.sep.vox.application.port.input.usecase.schoolclassuser.UpdateSchoolClassUserStatusUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schoolclassuser.UpdateSchoolClassUserStatusResponse;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.ClassCode;

class UpdateSchoolClassUserStatusUseCaseTests {

    private SchoolClassUserRepository schoolClassUserRepository;
    private SchoolClassRepository schoolClassRepository;
    private SchoolRepository schoolRepository;
    private UserRepository userRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserContextPort userContextPort;
    private UpdateSchoolClassUserStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userRepository = mock(UserRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new UpdateSchoolClassUserStatusUseCase(
            schoolClassUserRepository,
            schoolClassRepository,
            schoolRepository,
            userRepository,
            userContextPort,
            schoolUserRepository
        );
    }

    @Test
    void update_should_activate_inactive_membership_and_clear_left_at() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        SchoolClassUser membership = membership(targetUserId, classId, false, Instant.now().minus(1, ChronoUnit.DAYS), currentUserId);
        mockValidContext(currentUserId, schoolId, classId, targetUserId);
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(targetUserId, classId)).thenReturn(Optional.of(membership));

        UpdateSchoolClassUserStatusResponse response = useCase.execute(new UpdateSchoolClassUserStatusCommand(schoolId, classId, targetUserId, true));

        assertThat(response.schoolClassId()).isEqualTo(classId);
        ArgumentCaptor<SchoolClassUser> captor = ArgumentCaptor.forClass(SchoolClassUser.class);
        verify(schoolClassUserRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(captor.getValue().getLeftAt()).isNull();
    }

    @Test
    void update_should_deactivate_active_membership() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        SchoolClassUser membership = membership(targetUserId, classId, true, null, currentUserId);
        mockValidContext(currentUserId, schoolId, classId, targetUserId);
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(targetUserId, classId)).thenReturn(Optional.of(membership));

        UpdateSchoolClassUserStatusResponse response = useCase.execute(new UpdateSchoolClassUserStatusCommand(schoolId, classId, targetUserId, false));

        assertThat(response.schoolClassId()).isEqualTo(classId);
        ArgumentCaptor<SchoolClassUser> captor = ArgumentCaptor.forClass(SchoolClassUser.class);
        verify(schoolClassUserRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
        assertThat(captor.getValue().getLeftAt()).isNotNull();
    }

    @Test
    void update_should_be_idempotent_when_status_is_already_inactive() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        Instant leftAt = Instant.now().minus(1, ChronoUnit.DAYS);
        SchoolClassUser membership = membership(targetUserId, classId, false, leftAt, currentUserId);
        mockValidContext(currentUserId, schoolId, classId, targetUserId);
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(targetUserId, classId)).thenReturn(Optional.of(membership));

        UpdateSchoolClassUserStatusResponse response = useCase.execute(new UpdateSchoolClassUserStatusCommand(schoolId, classId, targetUserId, false));

        assertThat(response.schoolClassId()).isEqualTo(classId);
        assertThat(membership.getLeftAt()).isEqualTo(leftAt);
        verify(schoolClassUserRepository, never()).save(any());
    }

    @Test
    void update_should_throw_when_membership_not_found() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        mockValidContext(currentUserId, schoolId, classId, targetUserId);
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(targetUserId, classId)).thenReturn(Optional.empty());

        assertThrows(
            ResourceNotFoundException.class,
            () -> useCase.execute(new UpdateSchoolClassUserStatusCommand(schoolId, classId, targetUserId, true))
        );
    }

    private void mockValidContext(UUID currentUserId, UUID schoolId, UUID classId, UUID targetUserId) {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(activeSchoolClass(classId, schoolId)));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(activeUser(targetUserId, schoolId)));
        mockUserSchool(currentUserId, schoolId);
        mockUserSchool(targetUserId, schoolId);
    }

    private void mockUserSchool(UUID userId, UUID schoolId) {
        when(schoolUserRepository.findByUserId(userId))
            .thenReturn(Optional.of(SchoolUser.builder()
                .schoolId(schoolId)
                .userId(userId)
                .startDate(Instant.now())
                .endDate(null)
                .build()));
    }

    private static SchoolClassUser membership(UUID userId, UUID classId, boolean isActive, Instant leftAt, UUID assignedBy) {
        SchoolClassUser membership = SchoolClassUser.builder()
            .userId(userId)
            .schoolClassId(classId)
            .active(isActive)
            .joinedAt(Instant.now().minus(2, ChronoUnit.DAYS))
            .leftAt(leftAt)
            .assignedBy(assignedBy)
            .build();
        membership.setId(UUID.randomUUID());
        return membership;
    }

    private static User activeUser(UUID id, UUID schoolId) {
        User user = new User();
        user.setId(id);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private static School activeSchool(UUID id) {
        School school = new School();
        school.setId(id);
        school.setActive(true);
        return school;
    }

    private static SchoolClass activeSchoolClass(UUID id, UUID schoolId) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(id);
        schoolClass.setSchoolId(schoolId);
        schoolClass.setCode(new ClassCode("ENG-01"));
        schoolClass.setName("English 01");
        schoolClass.setStatus(SchoolClassStatus.ACTIVE);
        return schoolClass;
    }
}
