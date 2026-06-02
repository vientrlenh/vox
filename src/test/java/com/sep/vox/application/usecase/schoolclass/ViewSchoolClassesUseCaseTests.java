package com.sep.vox.application.usecase.schoolclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewSchoolClassesQuery;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassesUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.schoolclass.SchoolClass;
import com.sep.vox.domain.model.schoolclass.SchoolClassStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.ClassCode;

class ViewSchoolClassesUseCaseTests {

    private SchoolClassRepository schoolClassRepository;
    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private UserContextPort userContextPort;
    private ViewSchoolClassesUseCase viewSchoolClassesUseCase;

    @BeforeEach
    void setUp() {
        schoolClassRepository = mock(SchoolClassRepository.class);
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userContextPort = mock(UserContextPort.class);
        viewSchoolClassesUseCase = new ViewSchoolClassesUseCase(
            schoolClassRepository,
            userRepository,
            schoolRepository,
            userContextPort
        );
    }

    @Test
    void view_school_classes_should_return_current_users_school_classes() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
        var levelVersionId = UUID.randomUUID();
        var schoolClass = schoolClass(schoolId, languageId, gradeId, levelVersionId, "ENG_10_A");
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(user(currentUserId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school(schoolId, true)));
        when(schoolClassRepository.findBySchoolId(any(), any()))
            .thenReturn(new PageResult<>(List.of(schoolClass), 1, 20, 1, 1));

        var result = viewSchoolClassesUseCase.execute(new ViewSchoolClassesQuery(1, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().schoolId()).isEqualTo(schoolId);
        assertThat(result.content().getFirst().languageId()).isEqualTo(languageId);
        assertThat(result.content().getFirst().schoolGradeId()).isEqualTo(gradeId);
        assertThat(result.content().getFirst().targetSchoolLevelVersionId()).isEqualTo(levelVersionId);
        assertThat(result.content().getFirst().code()).isEqualTo("ENG_10_A");
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);

        var pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(schoolClassRepository).findBySchoolId(org.mockito.ArgumentMatchers.eq(schoolId), pageRequestCaptor.capture());
        assertThat(pageRequestCaptor.getValue().page()).isEqualTo(1);
        assertThat(pageRequestCaptor.getValue().size()).isEqualTo(20);
    }

    @Test
    void view_school_classes_should_throw_when_current_user_is_missing() {
        var currentUserId = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> viewSchoolClassesUseCase.execute(new ViewSchoolClassesQuery(1, 20)));

        verify(schoolClassRepository, never()).findBySchoolId(any(), any());
    }

    @Test
    void view_school_classes_should_throw_when_current_user_is_inactive() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(user(currentUserId, schoolId, UserStatus.INACTIVE)));

        assertThrows(IllegalStateException.class, () -> viewSchoolClassesUseCase.execute(new ViewSchoolClassesQuery(1, 20)));

        verify(schoolRepository, never()).findById(any());
        verify(schoolClassRepository, never()).findBySchoolId(any(), any());
    }

    @Test
    void view_school_classes_should_throw_when_current_user_has_no_school() {
        var currentUserId = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(user(currentUserId, null)));

        assertThrows(IllegalStateException.class, () -> viewSchoolClassesUseCase.execute(new ViewSchoolClassesQuery(1, 20)));

        verify(schoolClassRepository, never()).findBySchoolId(any(), any());
    }

    @Test
    void view_school_classes_should_throw_when_school_is_missing() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(user(currentUserId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> viewSchoolClassesUseCase.execute(new ViewSchoolClassesQuery(1, 20)));

        verify(schoolClassRepository, never()).findBySchoolId(any(), any());
    }

    @Test
    void view_school_classes_should_throw_when_school_is_inactive() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(user(currentUserId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school(schoolId, false)));

        assertThrows(IllegalStateException.class, () -> viewSchoolClassesUseCase.execute(new ViewSchoolClassesQuery(1, 20)));

        verify(schoolClassRepository, never()).findBySchoolId(any(), any());
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

    private static SchoolClass schoolClass(UUID schoolId, UUID languageId, UUID gradeId, UUID levelVersionId, String code) {
        var now = OffsetDateTime.now();
        return new SchoolClass(
            UUID.randomUUID(),
            schoolId,
            languageId,
            gradeId,
            new ClassCode(code),
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
}
