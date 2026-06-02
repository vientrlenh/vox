package com.sep.vox.application.usecase.schoolclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateSchoolClassCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.CreateSchoolClassUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.languagelevel.LevelStatus;
import com.sep.vox.domain.model.languagelevel.SchoolLevel;
import com.sep.vox.domain.model.languagelevel.SchoolLevelVersion;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.schoolclass.SchoolClass;
import com.sep.vox.domain.model.schoolgrade.SchoolGrade;
import com.sep.vox.domain.model.schoolgrade.SchoolGradeStatus;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolLevelRepository;
import com.sep.vox.domain.repository.SchoolLevelVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.repository.UserRepository;

public class CreateSchoolClassUseCaseTests {

    private SchoolClassRepository schoolClassRepository;
    private SchoolRepository schoolRepository;
    private UserRepository userRepository;
    private SupportedLanguageRepository supportedLanguageRepository;
    private SchoolGradeRepository schoolGradeRepository;
    private SchoolLevelVersionRepository schoolLevelVersionRepository;
    private SchoolLevelRepository schoolLevelRepository;
    private UserContextPort userContextPort;
    private CreateSchoolClassUseCase createSchoolClassUseCase;

    @BeforeEach
    void setUp() {
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userRepository = mock(UserRepository.class);
        supportedLanguageRepository = mock(SupportedLanguageRepository.class);
        schoolGradeRepository = mock(SchoolGradeRepository.class);
        schoolLevelVersionRepository = mock(SchoolLevelVersionRepository.class);
        schoolLevelRepository = mock(SchoolLevelRepository.class);
        userContextPort = mock(UserContextPort.class);

        createSchoolClassUseCase = new CreateSchoolClassUseCase(
            schoolClassRepository,
            schoolRepository,
            userRepository,
            supportedLanguageRepository,
            schoolGradeRepository,
            schoolLevelVersionRepository,
            schoolLevelRepository,
            userContextPort
        );
    }

    @Test
    void create_school_class_should_save_active_class_for_current_users_school() {
        var ids = TestIds.create();
        var command = validCommand(ids);
        mockValidDependencies(ids);
        when(schoolClassRepository.save(any(SchoolClass.class))).thenAnswer(invocation -> {
            var schoolClass = invocation.getArgument(0, SchoolClass.class);
            schoolClass.setId(ids.savedClassId());
            return schoolClass;
        });

        var response = createSchoolClassUseCase.execute(command);

        assertThat(response.id()).isEqualTo(ids.savedClassId());
        assertThat(response.schoolId()).isEqualTo(ids.schoolId());
        assertThat(response.languageId()).isEqualTo(ids.languageId());
        assertThat(response.schoolGradeId()).isEqualTo(ids.schoolGradeId());
        assertThat(response.code()).isEqualTo("ENG_10-A");
        assertThat(response.name()).isEqualTo("English 10A");
        assertThat(response.description()).isEqualTo("Optional description");
        assertThat(response.targetSchoolLevelVersionId()).isEqualTo(ids.targetSchoolLevelVersionId());
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.createdBy()).isEqualTo(ids.currentUserId());
        assertThat(response.updatedBy()).isEqualTo(ids.currentUserId());

        var classCaptor = ArgumentCaptor.forClass(SchoolClass.class);
        verify(schoolClassRepository).save(classCaptor.capture());
        var savedClass = classCaptor.getValue();
        assertThat(savedClass.getSchoolId()).isEqualTo(ids.schoolId());
        assertThat(savedClass.getCode().value()).isEqualTo("ENG_10-A");
        assertThat(savedClass.getName()).isEqualTo("English 10A");
        assertThat(savedClass.getDescription()).isEqualTo("Optional description");
        assertThat(savedClass.getStatus().name()).isEqualTo("ACTIVE");
        assertThat(savedClass.getCreatedAt()).isNotNull();
        assertThat(savedClass.getUpdatedAt()).isEqualTo(savedClass.getCreatedAt());
    }

    @Test
    void create_school_class_should_throw_when_class_code_already_exists_in_school() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(schoolClassRepository.findBySchoolIdAndCode(ids.schoolId(), "ENG_10-A"))
            .thenReturn(Optional.of(new SchoolClass()));

        assertThrows(DuplicatedException.class, () -> createSchoolClassUseCase.execute(validCommand(ids)));

        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void create_school_class_should_throw_when_current_user_is_inactive() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(userRepository.findById(ids.currentUserId()))
            .thenReturn(Optional.of(user(ids.currentUserId(), ids.schoolId(), UserStatus.INACTIVE)));

        assertThrows(IllegalStateException.class, () -> createSchoolClassUseCase.execute(validCommand(ids)));

        verify(schoolRepository, never()).findById(any());
        verify(supportedLanguageRepository, never()).findById(any());
        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void create_school_class_should_throw_when_current_user_has_no_school() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(userRepository.findById(ids.currentUserId())).thenReturn(Optional.of(user(ids.currentUserId(), null)));

        assertThrows(IllegalStateException.class, () -> createSchoolClassUseCase.execute(validCommand(ids)));

        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void create_school_class_should_throw_when_language_is_missing() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(supportedLanguageRepository.findById(ids.languageId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> createSchoolClassUseCase.execute(validCommand(ids)));

        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void create_school_class_should_throw_when_language_is_inactive() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(supportedLanguageRepository.findById(ids.languageId()))
            .thenReturn(Optional.of(language(ids.languageId(), false)));

        assertThrows(IllegalStateException.class, () -> createSchoolClassUseCase.execute(validCommand(ids)));

        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void create_school_class_should_throw_when_grade_does_not_belong_to_current_school() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(schoolGradeRepository.findById(ids.schoolGradeId()))
            .thenReturn(Optional.of(grade(ids.schoolGradeId(), UUID.randomUUID(), SchoolGradeStatus.ACTIVE)));

        assertThrows(IllegalArgumentException.class, () -> createSchoolClassUseCase.execute(validCommand(ids)));

        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void create_school_class_should_throw_when_target_level_version_is_not_published() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(schoolLevelVersionRepository.findById(ids.targetSchoolLevelVersionId()))
            .thenReturn(Optional.of(levelVersion(ids.targetSchoolLevelVersionId(), ids.schoolLevelId(), LevelStatus.DRAFT)));

        assertThrows(IllegalStateException.class, () -> createSchoolClassUseCase.execute(validCommand(ids)));

        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void create_school_class_should_throw_when_target_level_does_not_match_school_or_language() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(schoolLevelRepository.findById(ids.schoolLevelId()))
            .thenReturn(Optional.of(schoolLevel(ids.schoolLevelId(), ids.schoolId(), UUID.randomUUID())));

        assertThrows(IllegalArgumentException.class, () -> createSchoolClassUseCase.execute(validCommand(ids)));

        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    private void mockValidDependencies(TestIds ids) {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ids.currentUserId());
        when(userRepository.findById(ids.currentUserId())).thenReturn(Optional.of(user(ids.currentUserId(), ids.schoolId())));
        when(schoolRepository.findById(ids.schoolId())).thenReturn(Optional.of(school(ids.schoolId(), true)));
        when(supportedLanguageRepository.findById(ids.languageId())).thenReturn(Optional.of(language(ids.languageId(), true)));
        when(schoolGradeRepository.findById(ids.schoolGradeId()))
            .thenReturn(Optional.of(grade(ids.schoolGradeId(), ids.schoolId(), SchoolGradeStatus.ACTIVE)));
        when(schoolLevelVersionRepository.findById(ids.targetSchoolLevelVersionId()))
            .thenReturn(Optional.of(levelVersion(ids.targetSchoolLevelVersionId(), ids.schoolLevelId(), LevelStatus.PUBLISHED)));
        when(schoolLevelRepository.findById(ids.schoolLevelId()))
            .thenReturn(Optional.of(schoolLevel(ids.schoolLevelId(), ids.schoolId(), ids.languageId())));
        when(schoolClassRepository.findBySchoolIdAndCode(ids.schoolId(), "ENG_10-A")).thenReturn(Optional.empty());
    }

    private CreateSchoolClassCommand validCommand(TestIds ids) {
        return new CreateSchoolClassCommand(
            ids.languageId(),
            ids.schoolGradeId(),
            " eng_10-a ",
            "  English   10A  ",
            "  Optional   description  ",
            ids.targetSchoolLevelVersionId()
        );
    }

    private User user(UUID userId, UUID schoolId) {
        return user(userId, schoolId, UserStatus.ACTIVE);
    }

    private User user(UUID userId, UUID schoolId, UserStatus status) {
        var user = new User();
        user.setId(userId);
        user.setSchoolId(schoolId);
        user.setStatus(status);
        return user;
    }

    private School school(UUID schoolId, boolean active) {
        var school = new School();
        school.setId(schoolId);
        school.setActive(active);
        return school;
    }

    private SupportedLanguage language(UUID languageId, boolean active) {
        var language = new SupportedLanguage();
        language.setId(languageId);
        language.setActive(active);
        return language;
    }

    private SchoolGrade grade(UUID gradeId, UUID schoolId, SchoolGradeStatus status) {
        var grade = new SchoolGrade();
        grade.setId(gradeId);
        grade.setSchoolId(schoolId);
        grade.setStatus(status);
        return grade;
    }

    private SchoolLevelVersion levelVersion(UUID levelVersionId, UUID schoolLevelId, LevelStatus status) {
        var levelVersion = new SchoolLevelVersion();
        levelVersion.setId(levelVersionId);
        levelVersion.setSchoolLevelId(schoolLevelId);
        levelVersion.setStatus(status);
        return levelVersion;
    }

    private SchoolLevel schoolLevel(UUID schoolLevelId, UUID schoolId, UUID languageId) {
        var schoolLevel = new SchoolLevel();
        schoolLevel.setId(schoolLevelId);
        schoolLevel.setSchoolId(schoolId);
        schoolLevel.setLanguageId(languageId);
        return schoolLevel;
    }

    private record TestIds(
        UUID currentUserId,
        UUID schoolId,
        UUID languageId,
        UUID schoolGradeId,
        UUID schoolLevelId,
        UUID targetSchoolLevelVersionId,
        UUID savedClassId
    ) {
        private static TestIds create() {
            return new TestIds(
                UUID.randomUUID(),
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
