package com.sep.vox.application.usecase.schoolclass;

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
import com.sep.vox.application.port.input.command.DeleteSchoolClassCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.DeleteSchoolClassUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.schoolclass.SchoolClass;
import com.sep.vox.domain.model.schoolclass.SchoolClassStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassDependencyRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.ClassCode;

class DeleteSchoolClassUseCaseTests {

    private SchoolClassRepository schoolClassRepository;
    private SchoolClassDependencyRepository schoolClassDependencyRepository;
    private SchoolRepository schoolRepository;
    private UserRepository userRepository;
    private UserContextPort userContextPort;
    private DeleteSchoolClassUseCase deleteSchoolClassUseCase;

    @BeforeEach
    void setUp() {
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolClassDependencyRepository = mock(SchoolClassDependencyRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userRepository = mock(UserRepository.class);
        userContextPort = mock(UserContextPort.class);
        deleteSchoolClassUseCase = new DeleteSchoolClassUseCase(
            schoolClassRepository,
            schoolClassDependencyRepository,
            schoolRepository,
            userRepository,
            userContextPort
        );
    }

    @Test
    void delete_school_class_should_hard_delete_when_no_dependency_exists() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(schoolClassDependencyRepository.existsDependencyBySchoolClassId(ids.classId())).thenReturn(false);

        var result = deleteSchoolClassUseCase.execute(new DeleteSchoolClassCommand(ids.classId()));

        assertThat(result.id()).isEqualTo(ids.classId());
        assertThat(result.deleteType()).isEqualTo("HARD");
        assertThat(result.status()).isNull();
        assertThat(result.updatedAt()).isNull();
        verify(schoolClassRepository).deleteById(ids.classId());
        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void delete_school_class_should_soft_delete_when_dependency_exists() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(schoolClassDependencyRepository.existsDependencyBySchoolClassId(ids.classId())).thenReturn(true);
        when(schoolClassRepository.save(any(SchoolClass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = deleteSchoolClassUseCase.execute(new DeleteSchoolClassCommand(ids.classId()));

        assertThat(result.id()).isEqualTo(ids.classId());
        assertThat(result.deleteType()).isEqualTo("SOFT");
        assertThat(result.status()).isEqualTo("ARCHIVED");
        assertThat(result.updatedAt()).isNotNull();

        var captor = ArgumentCaptor.forClass(SchoolClass.class);
        verify(schoolClassRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(SchoolClassStatus.ARCHIVED);
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUpdatedBy()).isEqualTo(ids.currentUserId());
        verify(schoolClassRepository, never()).deleteById(ids.classId());
    }

    @Test
    void delete_school_class_should_throw_when_current_user_is_missing() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(userRepository.findById(ids.currentUserId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> deleteSchoolClassUseCase.execute(new DeleteSchoolClassCommand(ids.classId())));

        verifyNoDeleteOrSave();
    }

    @Test
    void delete_school_class_should_throw_when_current_user_is_inactive() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(userRepository.findById(ids.currentUserId()))
            .thenReturn(Optional.of(user(ids.currentUserId(), ids.schoolId(), UserStatus.INACTIVE)));

        assertThrows(IllegalStateException.class, () -> deleteSchoolClassUseCase.execute(new DeleteSchoolClassCommand(ids.classId())));

        verify(schoolRepository, never()).findById(any());
        verify(schoolClassRepository, never()).findById(any());
        verify(schoolClassDependencyRepository, never()).existsDependencyBySchoolClassId(any());
        verifyNoDeleteOrSave();
    }

    @Test
    void delete_school_class_should_throw_when_current_user_has_no_school() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(userRepository.findById(ids.currentUserId())).thenReturn(Optional.of(user(ids.currentUserId(), null)));

        assertThrows(IllegalStateException.class, () -> deleteSchoolClassUseCase.execute(new DeleteSchoolClassCommand(ids.classId())));

        verifyNoDeleteOrSave();
    }

    @Test
    void delete_school_class_should_throw_when_school_is_missing_or_inactive() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(schoolRepository.findById(ids.schoolId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> deleteSchoolClassUseCase.execute(new DeleteSchoolClassCommand(ids.classId())));

        mockValidDependencies(ids);
        when(schoolRepository.findById(ids.schoolId())).thenReturn(Optional.of(school(ids.schoolId(), false)));

        assertThrows(IllegalStateException.class, () -> deleteSchoolClassUseCase.execute(new DeleteSchoolClassCommand(ids.classId())));
        verifyNoDeleteOrSave();
    }

    @Test
    void delete_school_class_should_throw_when_class_is_missing_or_belongs_to_other_school() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(schoolClassRepository.findById(ids.classId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> deleteSchoolClassUseCase.execute(new DeleteSchoolClassCommand(ids.classId())));

        mockValidDependencies(ids);
        when(schoolClassRepository.findById(ids.classId()))
            .thenReturn(Optional.of(schoolClass(ids.classId(), UUID.randomUUID())));

        assertThrows(NotFoundException.class, () -> deleteSchoolClassUseCase.execute(new DeleteSchoolClassCommand(ids.classId())));
        verifyNoDeleteOrSave();
    }

    private void verifyNoDeleteOrSave() {
        verify(schoolClassRepository, never()).deleteById(any(UUID.class));
        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    private void mockValidDependencies(TestIds ids) {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ids.currentUserId());
        when(userRepository.findById(ids.currentUserId())).thenReturn(Optional.of(user(ids.currentUserId(), ids.schoolId())));
        when(schoolRepository.findById(ids.schoolId())).thenReturn(Optional.of(school(ids.schoolId(), true)));
        when(schoolClassRepository.findById(ids.classId())).thenReturn(Optional.of(schoolClass(ids.classId(), ids.schoolId())));
    }

    private static User user(UUID userId, UUID schoolId) {
        return user(userId, schoolId, UserStatus.ACTIVE);
    }

    private static User user(UUID userId, UUID schoolId, UserStatus status) {
        var user = new User();
        user.setId(userId);
        user.setSchoolId(schoolId);
        user.setStatus(status);
        return user;
    }

    private static School school(UUID schoolId, boolean active) {
        var school = new School();
        school.setId(schoolId);
        school.setActive(active);
        return school;
    }

    private static SchoolClass schoolClass(UUID classId, UUID schoolId) {
        var now = OffsetDateTime.now();
        return new SchoolClass(
            classId,
            schoolId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            new ClassCode("ENG_10_A"),
            "English 10A",
            "Description",
            UUID.randomUUID(),
            SchoolClassStatus.ACTIVE,
            now.minusDays(1),
            now.minusDays(1),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }

    private record TestIds(UUID currentUserId, UUID schoolId, UUID classId) {

        private static TestIds create() {
            return new TestIds(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        }
    }
}
