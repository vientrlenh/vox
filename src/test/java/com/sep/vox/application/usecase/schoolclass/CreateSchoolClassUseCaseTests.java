package com.sep.vox.application.usecase.schoolclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
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
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.LanguageCode;

class CreateSchoolClassUseCaseTests {

    private SchoolClassRepository schoolClassRepository;
    private SchoolRepository schoolRepository;
    private UserRepository userRepository;
    private SupportedLanguageRepository supportedLanguageRepository;
    private SchoolGradeRepository schoolGradeRepository;
    private UserContextPort userContextPort;
    private CreateSchoolClassUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userRepository = mock(UserRepository.class);
        supportedLanguageRepository = mock(SupportedLanguageRepository.class);
        schoolGradeRepository = mock(SchoolGradeRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new CreateSchoolClassUseCase(
            schoolClassRepository,
            schoolRepository,
            userRepository,
            supportedLanguageRepository,
            schoolGradeRepository,
            userContextPort
        );
    }

    @Test
    void create_should_save_active_class_for_current_users_school() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
        var savedId = UUID.randomUUID();
        var command = new CreateSchoolClassCommand(schoolId, languageId, gradeId, "  eng-01  ", "  English   01  ", "  Starter   class  ");

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(supportedLanguageRepository.findById(languageId)).thenReturn(Optional.of(activeLanguage(languageId)));
        when(schoolGradeRepository.findById(gradeId)).thenReturn(Optional.of(activeGrade(gradeId, schoolId)));
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
        var command = new CreateSchoolClassCommand(schoolId, languageId, gradeId, "ENG-01", "English 01", null);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(supportedLanguageRepository.findById(languageId)).thenReturn(Optional.of(activeLanguage(languageId)));
        when(schoolGradeRepository.findById(gradeId)).thenReturn(Optional.of(activeGrade(gradeId, schoolId)));
        when(schoolClassRepository.findBySchoolIdAndCode(schoolId, "ENG-01"))
            .thenReturn(Optional.of(SchoolClass.create(schoolId, languageId, gradeId, "ENG-01", "Existing", null, userId, OffsetDateTime.now())));

        assertThrows(DuplicatedException.class, () -> useCase.execute(command));

        verify(schoolClassRepository).findBySchoolIdAndCode(schoolId, "ENG-01");
        verify(schoolClassRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void create_should_throw_when_current_user_is_inactive() {
        var userId = UUID.randomUUID();
        var command = new CreateSchoolClassCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ENG-01", "English 01", null);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, UUID.randomUUID(), UserStatus.INACTIVE)));

        assertThrows(IllegalStateException.class, () -> useCase.execute(command));

        verifyNoInteractions(schoolRepository, supportedLanguageRepository, schoolGradeRepository, schoolClassRepository);
    }

    @Test
    void create_should_throw_when_requested_school_differs_from_current_user_school() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var command = new CreateSchoolClassCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ENG-01", "English 01", null);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId, schoolId)));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));
        verifyNoInteractions(schoolRepository, supportedLanguageRepository, schoolGradeRepository, schoolClassRepository);
    }

    private static User activeUser(UUID id, UUID schoolId) {
        return user(id, schoolId, UserStatus.ACTIVE);
    }

    private static User user(UUID id, UUID schoolId, UserStatus status) {
        var user = new User();
        user.setId(id);
        user.setSchoolId(schoolId);
        user.setStatus(status);
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

    private static SchoolGrade activeGrade(UUID id, UUID schoolId) {
        var grade = new SchoolGrade();
        grade.setId(id);
        grade.setSchoolId(schoolId);
        grade.setStatus(SchoolGradeStatus.ACTIVE);
        return grade;
    }
}
