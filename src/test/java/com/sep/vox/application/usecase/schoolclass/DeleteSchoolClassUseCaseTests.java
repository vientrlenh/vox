package com.sep.vox.application.usecase.schoolclass;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteSchoolClassCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.DeleteSchoolClassUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schoolclass.DeleteSchoolClassResponse;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassDependencyRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.ClassCode;

class DeleteSchoolClassUseCaseTests {

    private SchoolClassRepository schoolClassRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
    private SchoolClassDependencyRepository schoolClassDependencyRepository;
    private SchoolRepository schoolRepository;
    private UserRepository userRepository;
    private UserContextPort userContextPort;
    private SchoolUserRepository schoolUserRepository;
    private DeleteSchoolClassUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        schoolClassDependencyRepository = mock(SchoolClassDependencyRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userRepository = mock(UserRepository.class);
        userContextPort = mock(UserContextPort.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        useCase = new DeleteSchoolClassUseCase(
            schoolClassRepository,
            schoolClassUserRepository,
            schoolClassDependencyRepository,
            schoolRepository,
            userRepository,
            userContextPort,
            schoolUserRepository
        );
    }

    @Test
    void delete_should_hard_delete_when_class_has_no_dependency() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        stubActiveContext(userId, schoolId);
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(schoolClass(classId, schoolId)));
        when(schoolClassDependencyRepository.existsDependencyBySchoolClassId(classId)).thenReturn(false);

        DeleteSchoolClassResponse response = useCase.execute(new DeleteSchoolClassCommand(schoolId, classId));

        assertThat(response.id()).isEqualTo(classId);
        assertThat(response.deleteType()).isEqualTo("HARD");
        assertThat(response.status()).isNull();
        assertThat(response.updatedAt()).isNull();
        verify(schoolClassRepository).deleteById(classId);
    }

    @Test
    void delete_should_soft_delete_when_class_has_dependency() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        SchoolClass schoolClass = schoolClass(classId, schoolId);
        stubActiveContext(userId, schoolId);
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        when(schoolClassDependencyRepository.existsDependencyBySchoolClassId(classId)).thenReturn(true);
        when(schoolClassRepository.save(any(SchoolClass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeleteSchoolClassResponse response = useCase.execute(new DeleteSchoolClassCommand(schoolId, classId));

        ArgumentCaptor<SchoolClass> captor = ArgumentCaptor.forClass(SchoolClass.class);
        verify(schoolClassRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SchoolClassStatus.ARCHIVED);
        // Soft-deleting a class must deactivate all members of the class.
        verify(schoolClassUserRepository).deactivateBySchoolClassId(org.mockito.ArgumentMatchers.eq(classId), any());
        verify(schoolClassRepository, org.mockito.Mockito.never()).deleteById(any());
        assertThat(response.id()).isEqualTo(classId);
        assertThat(response.deleteType()).isEqualTo("SOFT");
        assertThat(response.status()).isEqualTo("ARCHIVED");
        assertThat(response.updatedAt()).isEqualTo(captor.getValue().getUpdatedAt().toString());
    }

    @Test
    void delete_should_throw_when_class_already_archived() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        SchoolClass archived = schoolClass(classId, schoolId);
        archived.setStatus(SchoolClassStatus.ARCHIVED);
        stubActiveContext(userId, schoolId);
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(archived));

        assertThrows(NotFoundException.class, () -> useCase.execute(new DeleteSchoolClassCommand(schoolId, classId)));

        verifyNoInteractions(schoolClassDependencyRepository);
        verify(schoolClassRepository, org.mockito.Mockito.never()).deleteById(any());
        verify(schoolClassRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void delete_should_throw_when_class_does_not_exist() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        stubActiveContext(userId, schoolId);
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(new DeleteSchoolClassCommand(schoolId, classId)));

        verifyNoInteractions(schoolClassDependencyRepository);
    }

    @Test
    void delete_should_throw_when_class_belongs_to_other_school() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        stubActiveContext(userId, schoolId);
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(schoolClass(classId, UUID.randomUUID())));

        assertThrows(NotFoundException.class, () -> useCase.execute(new DeleteSchoolClassCommand(schoolId, classId)));

        verifyNoInteractions(schoolClassDependencyRepository);
    }

    @Test
    void delete_should_throw_when_requested_school_differs_from_current_user_school() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        stubActiveContext(userId, schoolId);

        assertThrows(IllegalArgumentException.class,
            () -> useCase.execute(new DeleteSchoolClassCommand(UUID.randomUUID(), UUID.randomUUID())));

        verifyNoInteractions(schoolClassRepository, schoolClassDependencyRepository);
    }

    @Test
    void delete_should_throw_when_current_user_is_inactive() {
        UUID userId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        User _u1 = user(userId, UUID.randomUUID(), UserStatus.INACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_u1));

        assertThrows(IllegalStateException.class, () -> useCase.execute(new DeleteSchoolClassCommand(UUID.randomUUID(), UUID.randomUUID())));

        verifyNoInteractions(schoolRepository, schoolClassRepository, schoolClassDependencyRepository);
    }

    @Test
    void delete_should_throw_when_current_user_has_no_school() {
        UUID userId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        User _user = user(userId, null, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user));

        assertThrows(IllegalStateException.class, () -> useCase.execute(new DeleteSchoolClassCommand(UUID.randomUUID(), UUID.randomUUID())));

        verifyNoInteractions(schoolRepository, schoolClassRepository, schoolClassDependencyRepository);
    }

    @Test
    void delete_should_throw_when_school_is_inactive() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        School school = activeSchool(schoolId);
        school.setActive(false);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        User _user1 = user(userId, schoolId, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user1));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));

        assertThrows(IllegalStateException.class, () -> useCase.execute(new DeleteSchoolClassCommand(schoolId, UUID.randomUUID())));

        verifyNoInteractions(schoolClassRepository, schoolClassDependencyRepository);
    }

    private void stubActiveContext(UUID userId, UUID schoolId) {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        User _user2 = user(userId, schoolId, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user2));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
    }

    private User user(UUID id, UUID schoolId, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        when(schoolUserRepository.findByUserId(id)).thenReturn(
            schoolId != null ? Optional.of(SchoolUser.builder()
                .schoolId(schoolId)
                .userId(id)
                .startDate(java.time.Instant.now())
                .endDate(java.time.Instant.now().plus(36500, ChronoUnit.DAYS))
                .build()) : Optional.empty()
        );
        return user;
    }

    private static School activeSchool(UUID id) {
        School school = new School();
        school.setId(id);
        school.setActive(true);
        return school;
    }

    private static SchoolClass schoolClass(UUID id, UUID schoolId) {
        Instant now = Instant.now();
        UUID userId = UUID.randomUUID();
        return SchoolClass.builder()
            .id(id)
            .schoolId(schoolId)
            .languageId(UUID.randomUUID())
            .schoolGradeId(UUID.randomUUID())
            .code(new ClassCode("ENG-01"))
            .name("English 01")
            .description("Starter class")
            .status(SchoolClassStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .createdBy(userId.toString())
            .updatedBy(userId.toString())
            .build();
    }
}
