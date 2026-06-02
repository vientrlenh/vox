package com.sep.vox.application.usecase.schoolclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ImportValidationException;
import com.sep.vox.application.port.input.command.ImportSchoolClassRowCommand;
import com.sep.vox.application.port.input.command.ImportSchoolClassesCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.ImportSchoolClassesUseCase;
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
import com.sep.vox.domain.valueobject.ClassCode;

class ImportSchoolClassesUseCaseTests {

    private SchoolClassRepository schoolClassRepository;
    private SchoolRepository schoolRepository;
    private UserRepository userRepository;
    private SupportedLanguageRepository supportedLanguageRepository;
    private SchoolGradeRepository schoolGradeRepository;
    private SchoolLevelRepository schoolLevelRepository;
    private SchoolLevelVersionRepository schoolLevelVersionRepository;
    private UserContextPort userContextPort;
    private ImportSchoolClassesUseCase importSchoolClassesUseCase;

    @BeforeEach
    void setUp() {
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userRepository = mock(UserRepository.class);
        supportedLanguageRepository = mock(SupportedLanguageRepository.class);
        schoolGradeRepository = mock(SchoolGradeRepository.class);
        schoolLevelRepository = mock(SchoolLevelRepository.class);
        schoolLevelVersionRepository = mock(SchoolLevelVersionRepository.class);
        userContextPort = mock(UserContextPort.class);

        importSchoolClassesUseCase = new ImportSchoolClassesUseCase(
            schoolClassRepository,
            schoolRepository,
            userRepository,
            supportedLanguageRepository,
            schoolGradeRepository,
            schoolLevelRepository,
            schoolLevelVersionRepository,
            userContextPort
        );
    }

    @Test
    void import_should_create_all_classes_when_rows_are_valid() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(schoolClassRepository.save(any(SchoolClass.class))).thenAnswer(invocation -> {
            var schoolClass = invocation.getArgument(0, SchoolClass.class);
            schoolClass.setId(UUID.randomUUID());
            return schoolClass;
        });

        var result = importSchoolClassesUseCase.execute(new ImportSchoolClassesCommand(List.of(
            row(2, " eng ", " g10 ", " a1 ", "1", " eng_10_a ", "  English   10A  ", " Optional "),
            row(3, "ENG", "G10", "A1", "1", "ENG_10_B", "English 10B", null)
        )));

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.createdCount()).isEqualTo(2);
        assertThat(result.classes()).hasSize(2);
        assertThat(result.classes())
            .extracting(schoolClass -> schoolClass.code())
            .containsExactly("ENG_10_A", "ENG_10_B");
        assertThat(result.classes().getFirst().schoolId()).isEqualTo(ids.schoolId());
        assertThat(result.classes().getFirst().languageId()).isEqualTo(ids.languageId());
        assertThat(result.classes().getFirst().schoolGradeId()).isEqualTo(ids.gradeId());
        assertThat(result.classes().getFirst().targetSchoolLevelVersionId()).isEqualTo(ids.levelVersionId());
    }

    @Test
    void import_should_be_all_or_nothing_when_any_row_has_error() {
        var ids = TestIds.create();
        mockValidDependencies(ids);

        var exception = assertThrows(ImportValidationException.class, () -> importSchoolClassesUseCase.execute(
            new ImportSchoolClassesCommand(List.of(
                row(2, "ENG", "G10", "A1", "1", "ENG_10_A", "English 10A", null),
                row(3, "ENG", "G10", "A1", "1", "", "Missing code", null)
            ))
        ));

        assertSingleError(exception, 3, "code", "Trường bắt buộc không được để trống");
        verify(schoolClassRepository, never()).findBySchoolIdAndCodeIn(any(), any());
        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void import_should_throw_when_current_user_is_inactive() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(userRepository.findById(ids.currentUserId()))
            .thenReturn(Optional.of(user(ids.currentUserId(), ids.schoolId(), UserStatus.INACTIVE)));

        assertThrows(IllegalStateException.class, () -> importSchoolClassesUseCase.execute(
            new ImportSchoolClassesCommand(List.of(row(2, "ENG", "G10", "A1", "1", "ENG_10_A", "English 10A", null)))
        ));

        verify(schoolRepository, never()).findById(any());
        verify(supportedLanguageRepository, never()).findByCode(any());
        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void import_should_stop_at_first_structural_error_before_duplicate_validation() {
        var ids = TestIds.create();
        mockValidDependencies(ids);

        var exception = assertThrows(ImportValidationException.class, () -> importSchoolClassesUseCase.execute(
            new ImportSchoolClassesCommand(List.of(
                row(2, "", "G10", "A1", "1", "ENG_10_A", "Missing language", null),
                row(3, "ENG", "G10", "A1", "1", "ENG_10_A", "Duplicate code", null)
            ))
        ));

        assertSingleError(exception, 2, "languageCode", "Trường bắt buộc không được để trống");
        verify(schoolClassRepository, never()).findBySchoolIdAndCodeIn(any(), any());
        verify(supportedLanguageRepository, never()).findByCode(any());
        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void import_should_reject_duplicate_code_in_file() {
        var ids = TestIds.create();
        mockValidDependencies(ids);

        var exception = assertThrows(ImportValidationException.class, () -> importSchoolClassesUseCase.execute(
            new ImportSchoolClassesCommand(List.of(
                row(2, "ENG", "G10", "A1", "1", "ENG_10_A", "English 10A", null),
                row(3, "ENG", "G10", "A1", "1", " eng_10_a ", "Duplicate", null)
            ))
        ));

        assertSingleError(exception, 3, "code", "Mã lớp học bị trùng với dòng 2");
        verify(schoolClassRepository, never()).findBySchoolIdAndCodeIn(any(), any());
        verify(supportedLanguageRepository, never()).findByCode(any());
        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void import_should_reject_duplicate_code_in_database() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(schoolClassRepository.findBySchoolIdAndCodeIn(any(), any()))
            .thenReturn(List.of(existingClass("ENG_10_A"), existingClass("ENG_10_B")));

        var exception = assertThrows(ImportValidationException.class, () -> importSchoolClassesUseCase.execute(
            new ImportSchoolClassesCommand(List.of(
                row(2, "ENG", "G10", "A1", "1", "ENG_10_A", "English 10A", null),
                row(3, "ENG", "G10", "A1", "1", "ENG_10_B", "English 10B", null)
            ))
        ));

        assertSingleError(exception, 2, "code", "Mã lớp học đã tồn tại trong trường");
        verify(supportedLanguageRepository, never()).findByCode(any());
        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void import_should_reject_missing_or_inactive_language() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(supportedLanguageRepository.findByCode("ENG")).thenReturn(Optional.empty());

        var missing = assertThrows(ImportValidationException.class, () -> importSchoolClassesUseCase.execute(
            new ImportSchoolClassesCommand(List.of(row(2, "ENG", "G10", "A1", "1", "ENG_10_A", "English 10A", null)))
        ));
        assertSingleError(missing, 2, "languageCode", "Không tìm thấy ngôn ngữ");

        mockValidDependencies(ids);
        when(supportedLanguageRepository.findByCode("ENG")).thenReturn(Optional.of(language(ids.languageId(), false)));

        var inactive = assertThrows(ImportValidationException.class, () -> importSchoolClassesUseCase.execute(
            new ImportSchoolClassesCommand(List.of(
                row(2, "ENG", "G10", "A1", "1", "ENG_10_A", "English 10A", null),
                row(3, "FRA", "G10", "A1", "1", "FRA_10_A", "French 10A", null)
            ))
        ));
        assertSingleError(inactive, 2, "languageCode", "Ngôn ngữ không hoạt động");
        verify(supportedLanguageRepository, never()).findByCode("FRA");
        verify(schoolGradeRepository, never()).findBySchoolIdAndCode(any(), any());
        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void import_should_reject_missing_or_inactive_grade() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(schoolGradeRepository.findBySchoolIdAndCode(ids.schoolId(), "G10")).thenReturn(Optional.empty());

        var missing = assertThrows(ImportValidationException.class, () -> importSchoolClassesUseCase.execute(
            new ImportSchoolClassesCommand(List.of(row(2, "ENG", "G10", "A1", "1", "ENG_10_A", "English 10A", null)))
        ));
        assertSingleError(missing, 2, "schoolGradeCode", "Không tìm thấy khối học trong trường hiện tại");

        mockValidDependencies(ids);
        when(schoolGradeRepository.findBySchoolIdAndCode(ids.schoolId(), "G10"))
            .thenReturn(Optional.of(grade(ids.gradeId(), ids.schoolId(), SchoolGradeStatus.INACTIVE)));

        var inactive = assertThrows(ImportValidationException.class, () -> importSchoolClassesUseCase.execute(
            new ImportSchoolClassesCommand(List.of(row(2, "ENG", "G10", "A1", "1", "ENG_10_A", "English 10A", null)))
        ));
        assertSingleError(inactive, 2, "schoolGradeCode", "Khối học không hoạt động");
        verify(schoolLevelRepository, never()).findBySchoolIdAndLanguageIdAndCode(any(), any(), any());
        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    @Test
    void import_should_reject_missing_level_or_unpublished_version() {
        var ids = TestIds.create();
        mockValidDependencies(ids);
        when(schoolLevelRepository.findBySchoolIdAndLanguageIdAndCode(ids.schoolId(), ids.languageId(), "A1"))
            .thenReturn(Optional.empty());

        var missingLevel = assertThrows(ImportValidationException.class, () -> importSchoolClassesUseCase.execute(
            new ImportSchoolClassesCommand(List.of(row(2, "ENG", "G10", "A1", "1", "ENG_10_A", "English 10A", null)))
        ));
        assertSingleError(missingLevel, 2, "targetSchoolLevelCode", "Không tìm thấy cấp độ mục tiêu theo ngôn ngữ đã chọn");

        mockValidDependencies(ids);
        when(schoolLevelVersionRepository.findBySchoolLevelIdAndVersion(ids.levelId(), 1))
            .thenReturn(Optional.of(levelVersion(ids.levelVersionId(), ids.levelId(), LevelStatus.DRAFT)));

        var draftVersion = assertThrows(ImportValidationException.class, () -> importSchoolClassesUseCase.execute(
            new ImportSchoolClassesCommand(List.of(row(2, "ENG", "G10", "A1", "1", "ENG_10_A", "English 10A", null)))
        ));
        assertSingleError(draftVersion, 2, "targetSchoolLevelVersion", "Phiên bản cấp độ mục tiêu chưa được công bố");
        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
    }

    private void mockValidDependencies(TestIds ids) {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ids.currentUserId());
        when(userRepository.findById(ids.currentUserId())).thenReturn(Optional.of(user(ids.currentUserId(), ids.schoolId())));
        when(schoolRepository.findById(ids.schoolId())).thenReturn(Optional.of(school(ids.schoolId(), true)));
        when(supportedLanguageRepository.findByCode("ENG")).thenReturn(Optional.of(language(ids.languageId(), true)));
        when(schoolGradeRepository.findBySchoolIdAndCode(ids.schoolId(), "G10"))
            .thenReturn(Optional.of(grade(ids.gradeId(), ids.schoolId(), SchoolGradeStatus.ACTIVE)));
        when(schoolLevelRepository.findBySchoolIdAndLanguageIdAndCode(ids.schoolId(), ids.languageId(), "A1"))
            .thenReturn(Optional.of(level(ids.levelId(), ids.schoolId(), ids.languageId())));
        when(schoolLevelVersionRepository.findBySchoolLevelIdAndVersion(ids.levelId(), 1))
            .thenReturn(Optional.of(levelVersion(ids.levelVersionId(), ids.levelId(), LevelStatus.PUBLISHED)));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(any(), any())).thenReturn(List.of());
    }

    private static ImportSchoolClassRowCommand row(int rowNumber, String languageCode, String schoolGradeCode,
            String targetSchoolLevelCode, String targetSchoolLevelVersion, String code, String name, String description) {
        return new ImportSchoolClassRowCommand(
            rowNumber,
            languageCode,
            schoolGradeCode,
            targetSchoolLevelCode,
            targetSchoolLevelVersion,
            code,
            name,
            description
        );
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

    private static SupportedLanguage language(UUID languageId, boolean active) {
        var language = new SupportedLanguage();
        language.setId(languageId);
        language.setActive(active);
        return language;
    }

    private static SchoolGrade grade(UUID gradeId, UUID schoolId, SchoolGradeStatus status) {
        var grade = new SchoolGrade();
        grade.setId(gradeId);
        grade.setSchoolId(schoolId);
        grade.setStatus(status);
        return grade;
    }

    private static SchoolLevel level(UUID levelId, UUID schoolId, UUID languageId) {
        var level = new SchoolLevel();
        level.setId(levelId);
        level.setSchoolId(schoolId);
        level.setLanguageId(languageId);
        return level;
    }

    private static SchoolLevelVersion levelVersion(UUID levelVersionId, UUID levelId, LevelStatus status) {
        var levelVersion = new SchoolLevelVersion();
        levelVersion.setId(levelVersionId);
        levelVersion.setSchoolLevelId(levelId);
        levelVersion.setStatus(status);
        return levelVersion;
    }

    private static SchoolClass existingClass(String code) {
        var schoolClass = new SchoolClass();
        schoolClass.setCode(new ClassCode(code));
        return schoolClass;
    }

    private static void assertSingleError(ImportValidationException exception, int rowNumber, String field,
            String message) {
        assertThat(exception.getErrors()).hasSize(1);
        var error = exception.getErrors().getFirst();
        assertThat(error.rowNumber()).isEqualTo(rowNumber);
        assertThat(error.field()).isEqualTo(field);
        assertThat(error.message()).isEqualTo(message);
    }

    private record TestIds(
            UUID currentUserId,
            UUID schoolId,
            UUID languageId,
            UUID gradeId,
            UUID levelId,
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
