package com.sep.vox.application.usecase.schoolclass;

import com.sep.vox.application.usecase.TestSchoolUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.ViewSchoolClassesQuery;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassesUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;

class ViewSchoolClassesUseCaseTests {

    private SchoolClassRepository schoolClassRepository;
    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private UserContextPort userContextPort;
    private ViewSchoolClassesUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolClassRepository = mock(SchoolClassRepository.class);
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ViewSchoolClassesUseCase(
            schoolClassRepository,
            userRepository,
            schoolRepository,
            userContextPort,
            TestSchoolUserRepository.create()
        );
    }

    @Test
    void view_should_return_school_classes_with_search_and_filters() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
        var schoolClass = SchoolClass.create(
            schoolId,
            languageId,
            gradeId,
            "ENG-01",
            "English 01",
            "Starter class",
            userId,
            OffsetDateTime.now()
        );
        schoolClass.setId(UUID.randomUUID());
        var page = new PageResult<>(List.of(schoolClass), 1, 20, 1, 1);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, schoolId, UserStatus.ACTIVE)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(schoolClassRepository.findBySchoolId(
            schoolId,
            "English 01",
            SchoolClassStatus.ACTIVE,
            languageId,
            gradeId,
            new PageRequest(1, 20)
        )).thenReturn(page);

        var result = useCase.execute(new ViewSchoolClassesQuery(1, 20, "  English   01  ", "ACTIVE", languageId, gradeId));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().id()).isEqualTo(schoolClass.getId());
        assertThat(result.content().getFirst().code()).isEqualTo("ENG-01");
        assertThat(result.totalElements()).isEqualTo(1);
        verify(schoolClassRepository).findBySchoolId(
            schoolId,
            "English 01",
            SchoolClassStatus.ACTIVE,
            languageId,
            gradeId,
            new PageRequest(1, 20)
        );
    }

    @Test
    void view_should_throw_when_status_is_invalid() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, schoolId, UserStatus.ACTIVE)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));

        assertThrows(
            IllegalArgumentException.class,
            () -> useCase.execute(new ViewSchoolClassesQuery(1, 20, null, "DELETED", null, null))
        );
    }

    @Test
    void view_should_throw_when_current_user_is_inactive() {
        var userId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, UUID.randomUUID(), UserStatus.INACTIVE)));

        assertThrows(
            IllegalStateException.class,
            () -> useCase.execute(new ViewSchoolClassesQuery(1, 20, null, null, null, null))
        );

        verifyNoInteractions(schoolRepository, schoolClassRepository);
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
}
