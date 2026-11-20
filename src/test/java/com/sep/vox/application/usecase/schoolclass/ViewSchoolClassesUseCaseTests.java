package com.sep.vox.application.usecase.schoolclass;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.ViewSchoolClassesQuery;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassesUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

class ViewSchoolClassesUseCaseTests {

    private SchoolClassRepository schoolClassRepository;
    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private UserContextPort userContextPort;
    private SchoolUserRepository schoolUserRepository;
    private ViewSchoolClassesUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolClassRepository = mock(SchoolClassRepository.class);
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userContextPort = mock(UserContextPort.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        useCase = new ViewSchoolClassesUseCase(
            schoolClassRepository,
            userRepository,
            schoolRepository,
            userContextPort,
            schoolUserRepository
        );
    }

    @Test
    void view_should_return_school_classes_with_search_and_filters() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID languageId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        SchoolClass schoolClass = SchoolClass.create(
            schoolId,
            languageId,
            gradeId,
            "ENG-01",
            "English 01",
            "Starter class",
            userId,
            Instant.now()
        );
        schoolClass.setId(UUID.randomUUID());
        PageResult<SchoolClass> page = new PageResult<>(List.of(schoolClass), 1, 20, 1, 1);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        User _user = user(userId, schoolId, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(schoolClassRepository.findBySchoolId(
            schoolId,
            "English 01",
            SchoolClassStatus.ACTIVE,
            languageId,
            gradeId,
            1, 
            20
        )).thenReturn(page);

        PageResult<SchoolClassDto> result = useCase.execute(new ViewSchoolClassesQuery(1, 20, "  English   01  ", SchoolClassStatus.ACTIVE, languageId, gradeId));

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
            1, 
            20
        );
    }

    @Test
    void view_should_throw_when_current_user_is_inactive() {
        UUID userId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        User _u3 = user(userId, UUID.randomUUID(), UserStatus.INACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_u3));

        assertThrows(
            IllegalStateException.class,
            () -> useCase.execute(new ViewSchoolClassesQuery(1, 20, null, null, null, null))
        );

        verifyNoInteractions(schoolRepository, schoolClassRepository);
    }

    private User user(UUID id, UUID schoolId, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        when(schoolUserRepository.findByUserId(id)).thenReturn(
            schoolId != null ? Optional.of(new SchoolUser(schoolId, id, java.time.Instant.now(), java.time.Instant.now().plus(36500, ChronoUnit.DAYS))) : Optional.empty()
        );
        return user;
    }

    private static School activeSchool(UUID id) {
        School school = new School();
        school.setId(id);
        school.setActive(true);
        return school;
    }
}
