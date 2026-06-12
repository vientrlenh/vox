package com.sep.vox.application.usecase.schoolclass;

import com.sep.vox.application.usecase.TestUserSchoolResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;

class ViewSchoolClassDetailsUseCaseTests {

    private SchoolClassRepository schoolClassRepository;
    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private UserContextPort userContextPort;
    private ViewSchoolClassDetailsUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolClassRepository = mock(SchoolClassRepository.class);
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ViewSchoolClassDetailsUseCase(
            schoolClassRepository,
            userRepository,
            schoolRepository,
            userContextPort,
            TestUserSchoolResolver.create()
        );
    }

    @Test
    void view_details_should_return_school_class_response_when_class_belongs_to_current_school() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var schoolClass = newSchoolClass(classId, schoolId, languageId, gradeId, "ENG-01", "English 01");

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, schoolId, UserStatus.ACTIVE)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(schoolClass));

        var result = useCase.execute(new ViewSchoolClassDetailsQuery(classId));

        assertThat(result.id()).isEqualTo(classId);
        assertThat(result.schoolId()).isEqualTo(schoolId);
        assertThat(result.languageId()).isEqualTo(languageId);
        assertThat(result.schoolGradeId()).isEqualTo(gradeId);
        assertThat(result.code()).isEqualTo("ENG-01");
        assertThat(result.name()).isEqualTo("English 01");
        assertThat(result.status()).isEqualTo("ACTIVE");
    }

    @Test
    void view_details_should_throw_when_class_does_not_exist() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, schoolId, UserStatus.ACTIVE)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(new ViewSchoolClassDetailsQuery(classId)));
    }

    @Test
    void view_details_should_throw_when_class_belongs_to_another_school() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var schoolClass = newSchoolClass(
            classId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ENG-01",
            "English 01"
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, schoolId, UserStatus.ACTIVE)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(schoolClass));

        assertThrows(NotFoundException.class, () -> useCase.execute(new ViewSchoolClassDetailsQuery(classId)));
    }

    @Test
    void view_details_should_throw_when_current_user_is_inactive() {
        var userId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, UUID.randomUUID(), UserStatus.INACTIVE)));

        assertThrows(IllegalStateException.class, () -> useCase.execute(new ViewSchoolClassDetailsQuery(UUID.randomUUID())));

        verifyNoInteractions(schoolRepository, schoolClassRepository);
    }

    @Test
    void view_details_should_throw_when_current_user_has_no_school() {
        var userId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, null, UserStatus.ACTIVE)));

        assertThrows(IllegalStateException.class, () -> useCase.execute(new ViewSchoolClassDetailsQuery(UUID.randomUUID())));

        verifyNoInteractions(schoolRepository, schoolClassRepository);
    }

    @Test
    void view_details_should_throw_when_school_is_inactive() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var school = activeSchool(schoolId);
        school.setActive(false);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, schoolId, UserStatus.ACTIVE)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));

        assertThrows(IllegalStateException.class, () -> useCase.execute(new ViewSchoolClassDetailsQuery(UUID.randomUUID())));

        verifyNoInteractions(schoolClassRepository);
    }

    private static SchoolClass newSchoolClass(UUID id, UUID schoolId, UUID languageId, UUID gradeId, String code, String name) {
        var schoolClass = SchoolClass.create(
            schoolId,
            languageId,
            gradeId,
            code,
            name,
            "Repository test class",
            UUID.randomUUID(),
            OffsetDateTime.now()
        );
        schoolClass.setId(id);
        return schoolClass;
    }

    private static User user(UUID id, UUID schoolId, UserStatus status) {
        var user = new User();
        user.setId(id);
        TestUserSchoolResolver.remember(id, schoolId);
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
