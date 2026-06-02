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

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewSchoolClassDetailsQuery;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassDetailsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.schoolclass.SchoolClass;
import com.sep.vox.domain.model.schoolclass.SchoolClassStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.ClassCode;

class ViewSchoolClassDetailsUseCaseTests {

    private SchoolClassRepository schoolClassRepository;
    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private UserContextPort userContextPort;
    private ViewSchoolClassDetailsUseCase viewSchoolClassDetailsUseCase;

    @BeforeEach
    void setUp() {
        schoolClassRepository = mock(SchoolClassRepository.class);
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userContextPort = mock(UserContextPort.class);
        viewSchoolClassDetailsUseCase = new ViewSchoolClassDetailsUseCase(
            schoolClassRepository,
            userRepository,
            schoolRepository,
            userContextPort
        );
    }

    @Test
    void view_school_class_details_should_return_class_in_current_users_school() {
        var ids = TestIds.create();
        mockCurrentActiveSchool(ids);
        var schoolClass = schoolClass(ids.classId(), ids.schoolId(), ids.languageId(), ids.gradeId(), ids.levelVersionId());
        when(schoolClassRepository.findById(ids.classId())).thenReturn(Optional.of(schoolClass));

        var result = viewSchoolClassDetailsUseCase.execute(new ViewSchoolClassDetailsQuery(ids.classId()));

        assertThat(result.id()).isEqualTo(ids.classId());
        assertThat(result.schoolId()).isEqualTo(ids.schoolId());
        assertThat(result.languageId()).isEqualTo(ids.languageId());
        assertThat(result.schoolGradeId()).isEqualTo(ids.gradeId());
        assertThat(result.targetSchoolLevelVersionId()).isEqualTo(ids.levelVersionId());
        assertThat(result.code()).isEqualTo("ENG_10_A");
        assertThat(result.status()).isEqualTo("ACTIVE");
        verify(schoolClassRepository).findById(ids.classId());
    }

    @Test
    void view_school_class_details_should_throw_when_current_user_is_missing() {
        var ids = TestIds.create();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ids.currentUserId());
        when(userRepository.findById(ids.currentUserId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
            () -> viewSchoolClassDetailsUseCase.execute(new ViewSchoolClassDetailsQuery(ids.classId())));

        verify(schoolRepository, never()).findById(any());
        verify(schoolClassRepository, never()).findById(any());
    }

    @Test
    void view_school_class_details_should_throw_when_current_user_is_inactive() {
        var ids = TestIds.create();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ids.currentUserId());
        when(userRepository.findById(ids.currentUserId()))
            .thenReturn(Optional.of(user(ids.currentUserId(), ids.schoolId(), UserStatus.INACTIVE)));

        assertThrows(IllegalStateException.class,
            () -> viewSchoolClassDetailsUseCase.execute(new ViewSchoolClassDetailsQuery(ids.classId())));

        verify(schoolRepository, never()).findById(any());
        verify(schoolClassRepository, never()).findById(any());
    }

    @Test
    void view_school_class_details_should_throw_when_current_user_has_no_school() {
        var ids = TestIds.create();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ids.currentUserId());
        when(userRepository.findById(ids.currentUserId())).thenReturn(Optional.of(user(ids.currentUserId(), null)));

        assertThrows(IllegalStateException.class,
            () -> viewSchoolClassDetailsUseCase.execute(new ViewSchoolClassDetailsQuery(ids.classId())));

        verify(schoolRepository, never()).findById(any());
        verify(schoolClassRepository, never()).findById(any());
    }

    @Test
    void view_school_class_details_should_throw_when_school_is_missing() {
        var ids = TestIds.create();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ids.currentUserId());
        when(userRepository.findById(ids.currentUserId())).thenReturn(Optional.of(user(ids.currentUserId(), ids.schoolId())));
        when(schoolRepository.findById(ids.schoolId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
            () -> viewSchoolClassDetailsUseCase.execute(new ViewSchoolClassDetailsQuery(ids.classId())));

        verify(schoolClassRepository, never()).findById(any());
    }

    @Test
    void view_school_class_details_should_throw_when_school_is_inactive() {
        var ids = TestIds.create();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ids.currentUserId());
        when(userRepository.findById(ids.currentUserId())).thenReturn(Optional.of(user(ids.currentUserId(), ids.schoolId())));
        when(schoolRepository.findById(ids.schoolId())).thenReturn(Optional.of(school(ids.schoolId(), false)));

        assertThrows(IllegalStateException.class,
            () -> viewSchoolClassDetailsUseCase.execute(new ViewSchoolClassDetailsQuery(ids.classId())));

        verify(schoolClassRepository, never()).findById(any());
    }

    @Test
    void view_school_class_details_should_throw_when_class_is_missing() {
        var ids = TestIds.create();
        mockCurrentActiveSchool(ids);
        when(schoolClassRepository.findById(ids.classId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
            () -> viewSchoolClassDetailsUseCase.execute(new ViewSchoolClassDetailsQuery(ids.classId())));
    }

    @Test
    void view_school_class_details_should_throw_when_class_belongs_to_other_school() {
        var ids = TestIds.create();
        mockCurrentActiveSchool(ids);
        var schoolClass = schoolClass(ids.classId(), UUID.randomUUID(), ids.languageId(), ids.gradeId(), ids.levelVersionId());
        when(schoolClassRepository.findById(ids.classId())).thenReturn(Optional.of(schoolClass));

        assertThrows(NotFoundException.class,
            () -> viewSchoolClassDetailsUseCase.execute(new ViewSchoolClassDetailsQuery(ids.classId())));
    }

    private void mockCurrentActiveSchool(TestIds ids) {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ids.currentUserId());
        when(userRepository.findById(ids.currentUserId())).thenReturn(Optional.of(user(ids.currentUserId(), ids.schoolId())));
        when(schoolRepository.findById(ids.schoolId())).thenReturn(Optional.of(school(ids.schoolId(), true)));
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

    private static SchoolClass schoolClass(UUID id, UUID schoolId, UUID languageId, UUID gradeId, UUID levelVersionId) {
        var now = OffsetDateTime.now();
        return new SchoolClass(
            id,
            schoolId,
            languageId,
            gradeId,
            new ClassCode("ENG_10_A"),
            "English 10A",
            "Test class",
            levelVersionId,
            SchoolClassStatus.ACTIVE,
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }

    private record TestIds(
            UUID currentUserId,
            UUID schoolId,
            UUID classId,
            UUID languageId,
            UUID gradeId,
            UUID levelVersionId) {

        private static TestIds create() {
            return new TestIds(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
            );
        }
    }
}
