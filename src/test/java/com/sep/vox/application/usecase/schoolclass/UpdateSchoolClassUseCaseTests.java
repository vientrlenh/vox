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
import com.sep.vox.application.port.input.command.UpdateSchoolClassCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.UpdateSchoolClassUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.languagelevel.LevelStatus;
import com.sep.vox.domain.model.languagelevel.SchoolLevel;
import com.sep.vox.domain.model.languagelevel.SchoolLevelVersion;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.schoolclass.SchoolClass;
import com.sep.vox.domain.model.schoolclass.SchoolClassStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolLevelRepository;
import com.sep.vox.domain.repository.SchoolLevelVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.ClassCode;

class UpdateSchoolClassUseCaseTests {

    private SchoolClassRepository schoolClassRepository;
    private SchoolRepository schoolRepository;
    private UserRepository userRepository;
    private SchoolLevelRepository schoolLevelRepository;
    private SchoolLevelVersionRepository schoolLevelVersionRepository;
    private UserContextPort userContextPort;
    private UpdateSchoolClassUseCase updateSchoolClassUseCase;

    @BeforeEach
    void setUp() {
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userRepository = mock(UserRepository.class);
        schoolLevelRepository = mock(SchoolLevelRepository.class);
        schoolLevelVersionRepository = mock(SchoolLevelVersionRepository.class);
        userContextPort = mock(UserContextPort.class);
        updateSchoolClassUseCase = new UpdateSchoolClassUseCase(
            schoolClassRepository,
            schoolRepository,
            userRepository,
            schoolLevelRepository,
            schoolLevelVersionRepository,
            userContextPort
        );
    }

    @Test
    void update_school_class_should_update_mutable_fields_only() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(schoolClassRepository.save(any(SchoolClass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = updateSchoolClassUseCase.execute(validCommand(ids));

        assertThat(result.id()).isEqualTo(ids.classId());
        assertThat(result.schoolId()).isEqualTo(ids.schoolId());
        assertThat(result.languageId()).isEqualTo(ids.languageId());
        assertThat(result.schoolGradeId()).isEqualTo(ids.gradeId());
        assertThat(result.code()).isEqualTo("ENG_10_A");
        assertThat(result.name()).isEqualTo("English 10A Updated");
        assertThat(result.description()).isEqualTo("Updated description");
        assertThat(result.targetSchoolLevelVersionId()).isEqualTo(ids.targetLevelVersionId());
        assertThat(result.status()).isEqualTo("INACTIVE");
        assertThat(result.updatedBy()).isEqualTo(ids.currentUserId());

        var captor = ArgumentCaptor.forClass(SchoolClass.class);
        verify(schoolClassRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getSchoolId()).isEqualTo(ids.schoolId());
        assertThat(saved.getLanguageId()).isEqualTo(ids.languageId());
        assertThat(saved.getSchoolGradeId()).isEqualTo(ids.gradeId());
        assertThat(saved.getCode().value()).isEqualTo("ENG_10_A");
        assertThat(saved.getName()).isEqualTo("English 10A Updated");
        assertThat(saved.getDescription()).isEqualTo("Updated description");
        assertThat(saved.getTargetSchoolLevelVersionId()).isEqualTo(ids.targetLevelVersionId());
        assertThat(saved.getStatus()).isEqualTo(SchoolClassStatus.INACTIVE);
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUpdatedBy()).isEqualTo(ids.currentUserId());
    }

    @Test
    void update_school_class_should_throw_when_current_user_is_missing() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(userRepository.findById(ids.currentUserId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> updateSchoolClassUseCase.execute(validCommand(ids)));

        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void update_school_class_should_throw_when_current_user_has_no_school() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(userRepository.findById(ids.currentUserId())).thenReturn(Optional.of(user(ids.currentUserId(), null)));

        assertThrows(IllegalStateException.class, () -> updateSchoolClassUseCase.execute(validCommand(ids)));

        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void update_school_class_should_throw_when_school_is_missing_or_inactive() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(schoolRepository.findById(ids.schoolId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> updateSchoolClassUseCase.execute(validCommand(ids)));

        mockValidDependencies(ids);
        when(schoolRepository.findById(ids.schoolId())).thenReturn(Optional.of(school(ids.schoolId(), false)));

        assertThrows(IllegalStateException.class, () -> updateSchoolClassUseCase.execute(validCommand(ids)));
        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void update_school_class_should_throw_when_class_is_missing_or_belongs_to_other_school() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(schoolClassRepository.findById(ids.classId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> updateSchoolClassUseCase.execute(validCommand(ids)));

        mockValidDependencies(ids);
        when(schoolClassRepository.findById(ids.classId()))
            .thenReturn(Optional.of(schoolClass(ids.classId(), UUID.randomUUID(), ids.languageId(), ids.gradeId(), ids.originalLevelVersionId())));

        assertThrows(NotFoundException.class, () -> updateSchoolClassUseCase.execute(validCommand(ids)));
        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void update_school_class_should_throw_when_target_level_version_is_missing_or_not_published() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(schoolLevelVersionRepository.findById(ids.targetLevelVersionId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> updateSchoolClassUseCase.execute(validCommand(ids)));

        mockValidDependencies(ids);
        when(schoolLevelVersionRepository.findById(ids.targetLevelVersionId()))
            .thenReturn(Optional.of(levelVersion(ids.targetLevelVersionId(), ids.targetLevelId(), LevelStatus.DRAFT)));

        assertThrows(IllegalStateException.class, () -> updateSchoolClassUseCase.execute(validCommand(ids)));
        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void update_school_class_should_throw_when_target_level_mismatches_school_or_language() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(schoolLevelRepository.findById(ids.targetLevelId()))
            .thenReturn(Optional.of(level(ids.targetLevelId(), UUID.randomUUID(), ids.languageId())));

        assertThrows(IllegalArgumentException.class, () -> updateSchoolClassUseCase.execute(validCommand(ids)));

        mockValidDependencies(ids);
        when(schoolLevelRepository.findById(ids.targetLevelId()))
            .thenReturn(Optional.of(level(ids.targetLevelId(), ids.schoolId(), UUID.randomUUID())));

        assertThrows(IllegalArgumentException.class, () -> updateSchoolClassUseCase.execute(validCommand(ids)));
        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void update_school_class_should_throw_when_status_is_invalid() {
        var ids = TestIds.create();
        mockValidDependencies(ids);

        assertThrows(IllegalArgumentException.class, () -> updateSchoolClassUseCase.execute(
            new UpdateSchoolClassCommand(
                ids.classId(),
                "English 10A Updated",
                "Updated description",
                ids.targetLevelVersionId(),
                "UNKNOWN"
            )
        ));

        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    private void mockValidDependencies(TestIds ids) {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ids.currentUserId());
        when(userRepository.findById(ids.currentUserId())).thenReturn(Optional.of(user(ids.currentUserId(), ids.schoolId())));
        when(schoolRepository.findById(ids.schoolId())).thenReturn(Optional.of(school(ids.schoolId(), true)));
        when(schoolClassRepository.findById(ids.classId()))
            .thenReturn(Optional.of(schoolClass(ids.classId(), ids.schoolId(), ids.languageId(), ids.gradeId(), ids.originalLevelVersionId())));
        when(schoolLevelVersionRepository.findById(ids.targetLevelVersionId()))
            .thenReturn(Optional.of(levelVersion(ids.targetLevelVersionId(), ids.targetLevelId(), LevelStatus.PUBLISHED)));
        when(schoolLevelRepository.findById(ids.targetLevelId()))
            .thenReturn(Optional.of(level(ids.targetLevelId(), ids.schoolId(), ids.languageId())));
    }

    private UpdateSchoolClassCommand validCommand(TestIds ids) {
        return new UpdateSchoolClassCommand(
            ids.classId(),
            "  English   10A   Updated  ",
            "  Updated   description  ",
            ids.targetLevelVersionId(),
            " INACTIVE "
        );
    }

    private static User user(UUID userId, UUID schoolId) {
        var user = new User();
        user.setId(userId);
        user.setSchoolId(schoolId);
        return user;
    }

    private static School school(UUID schoolId, boolean active) {
        var school = new School();
        school.setId(schoolId);
        school.setActive(active);
        return school;
    }

    private static SchoolClass schoolClass(UUID classId, UUID schoolId, UUID languageId, UUID gradeId,
            UUID targetLevelVersionId) {
        var now = OffsetDateTime.now();
        return new SchoolClass(
            classId,
            schoolId,
            languageId,
            gradeId,
            new ClassCode("ENG_10_A"),
            "English 10A",
            "Original description",
            targetLevelVersionId,
            SchoolClassStatus.ACTIVE,
            now.minusDays(1),
            now.minusDays(1),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }

    private static SchoolLevelVersion levelVersion(UUID id, UUID levelId, LevelStatus status) {
        var version = new SchoolLevelVersion();
        version.setId(id);
        version.setSchoolLevelId(levelId);
        version.setStatus(status);
        return version;
    }

    private static SchoolLevel level(UUID levelId, UUID schoolId, UUID languageId) {
        var level = new SchoolLevel();
        level.setId(levelId);
        level.setSchoolId(schoolId);
        level.setLanguageId(languageId);
        return level;
    }

    private record TestIds(
            UUID currentUserId,
            UUID schoolId,
            UUID classId,
            UUID languageId,
            UUID gradeId,
            UUID originalLevelVersionId,
            UUID targetLevelId,
            UUID targetLevelVersionId) {

        private static TestIds create() {
            return new TestIds(
                UUID.randomUUID(),
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
