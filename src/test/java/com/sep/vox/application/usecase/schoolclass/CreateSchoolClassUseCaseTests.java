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

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.CreateSchoolClassCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.CreateSchoolClassUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.LanguageCode;

class CreateSchoolClassUseCaseTests {

    private SchoolClassRepository schoolClassRepository;
    private SchoolRepository schoolRepository;
    private UserRepository userRepository;
    private SupportedLanguageRepository supportedLanguageRepository;
    private SchoolGradeRepository schoolGradeRepository;
    private UserContextPort userContextPort;
    private SchoolUserRepository schoolUserRepository;
    private CreateSchoolClassUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userRepository = mock(UserRepository.class);
        supportedLanguageRepository = mock(SupportedLanguageRepository.class);
        schoolGradeRepository = mock(SchoolGradeRepository.class);
        userContextPort = mock(UserContextPort.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        useCase = new CreateSchoolClassUseCase(
            schoolClassRepository,
            schoolRepository,
            userRepository,
            supportedLanguageRepository,
            schoolGradeRepository,
            userContextPort,
            schoolUserRepository
        );
    }

    @Test
    void create_should_save_active_class_for_current_users_school() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
        var savedId = UUID.randomUUID();
        var grade = activeGrade(gradeId, schoolId);
        var command = new CreateSchoolClassCommand(schoolId, languageId, gradeId, "  eng-01  ", "  English   01  ", "  Starter   class  ");

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        var _user = activeUser(userId, schoolId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(supportedLanguageRepository.findById(languageId)).thenReturn(Optional.of(activeLanguage(languageId)));
        when(schoolGradeRepository.findById(gradeId)).thenReturn(Optional.of(grade));
        when(schoolClassRepository.findBySchoolIdAndCode(schoolId, "ENG-01")).thenReturn(Optional.empty());
        when(schoolClassRepository.save(any(SchoolClass.class))).thenAnswer(invocation -> {
            var schoolClass = invocation.getArgument(0, SchoolClass.class);
            schoolClass.setId(savedId);
            return schoolClass;
        });

        var response = useCase.execute(command);

        assertThat(response.schoolClassId()).isEqualTo(savedId);
        var captor = ArgumentCaptor.forClass(SchoolClass.class);
        verify(schoolClassRepository).save(captor.capture());
        assertThat(captor.getValue().getSchoolId()).isEqualTo(schoolId);
        assertThat(captor.getValue().getLanguageId()).isEqualTo(languageId);
        assertThat(captor.getValue().getSchoolGradeId()).isEqualTo(gradeId);
        assertThat(captor.getValue().getCode().value()).isEqualTo("ENG-01");
        assertThat(captor.getValue().getName()).isEqualTo("English 01");
        assertThat(captor.getValue().getDescription()).isEqualTo("Starter class");
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(userId);
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(userId);
    }

    @Test
    void create_should_throw_when_code_already_exists_in_school() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
        var grade = activeGrade(gradeId, schoolId);
        var command = new CreateSchoolClassCommand(schoolId, languageId, gradeId, "ENG-01", "English 01", null);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        var _user1 = activeUser(userId, schoolId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user1));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(supportedLanguageRepository.findById(languageId)).thenReturn(Optional.of(activeLanguage(languageId)));
        when(schoolGradeRepository.findById(gradeId)).thenReturn(Optional.of(grade));
        when(schoolClassRepository.findBySchoolIdAndCode(schoolId, "ENG-01"))
            .thenReturn(Optional.of(SchoolClass.create(schoolId, languageId, gradeId, "ENG-01", "Existing", null, userId, Instant.now())));

        assertThrows(DuplicatedException.class, () -> useCase.execute(command));

        verify(schoolClassRepository).findBySchoolIdAndCode(schoolId, "ENG-01");
        verify(schoolClassRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void create_should_throw_when_grade_belongs_to_another_school() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
        var gradeLevelId = UUID.randomUUID();
        var grade = new SchoolGrade();
        grade.setId(gradeId);
        grade.setCode("G10");
        grade.setGradeLevelId(gradeLevelId);
        // Năm học thuộc trường khác → phải bị chặn.
        grade.setSchoolId(UUID.randomUUID());
        grade.setStatus(SchoolGradeStatus.ACTIVE);
        var command = new CreateSchoolClassCommand(schoolId, languageId, gradeId, "ENG-01", "English 01", null);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        var _user = activeUser(userId, schoolId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(supportedLanguageRepository.findById(languageId)).thenReturn(Optional.of(activeLanguage(languageId)));
        when(schoolGradeRepository.findById(gradeId)).thenReturn(Optional.of(grade));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));

        verify(schoolClassRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void create_should_throw_when_current_user_is_inactive() {
        var userId = UUID.randomUUID();
        var command = new CreateSchoolClassCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ENG-01", "English 01", null);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        var _u0 = user(userId, UUID.randomUUID(), UserStatus.INACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_u0));

        assertThrows(IllegalStateException.class, () -> useCase.execute(command));

        verifyNoInteractions(schoolRepository, supportedLanguageRepository, schoolGradeRepository, schoolClassRepository);
    }

    @Test
    void create_should_throw_when_requested_school_differs_from_current_user_school() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var command = new CreateSchoolClassCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ENG-01", "English 01", null);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        var _user2 = activeUser(userId, schoolId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(_user2));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));
        verifyNoInteractions(schoolRepository, supportedLanguageRepository, schoolGradeRepository, schoolClassRepository);
    }

    private User activeUser(UUID id, UUID schoolId) {
        return user(id, schoolId, UserStatus.ACTIVE);
    }

    private User user(UUID id, UUID schoolId, UserStatus status) {
        var user = new User();
        user.setId(id);
        user.setStatus(status);
        when(schoolUserRepository.findByUserId(id)).thenReturn(
            schoolId != null ? Optional.of(new SchoolUser(schoolId, id, java.time.Instant.now(), java.time.Instant.now().plus(36500, ChronoUnit.DAYS))) : Optional.empty()
        );
        return user;
    }

    private static School activeSchool(UUID id) {
        var school = new School();
        school.setId(id);
        school.setActive(true);
        return school;
    }

    private static SupportedLanguage activeLanguage(UUID id) {
        var language = new SupportedLanguage();
        language.setId(id);
        language.setCode(new LanguageCode("EN"));
        language.setActive(true);
        return language;
    }

    private SchoolGrade activeGrade(UUID id, UUID schoolId) {
        var gradeLevelId = UUID.randomUUID();
        var grade = new SchoolGrade();
        grade.setId(id);
        grade.setCode("G10");
        grade.setGradeLevelId(gradeLevelId);
        grade.setSchoolId(schoolId);
        grade.setStatus(SchoolGradeStatus.ACTIVE);
        return grade;
    }

}
