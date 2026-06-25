package com.sep.vox.application.usecase.schoolclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import com.sep.vox.application.port.input.service.ImportCommitResult;
import com.sep.vox.application.port.input.service.SchoolClassImportCommitHandler;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.valueobject.ClassCode;
import com.sep.vox.domain.valueobject.LanguageCode;

class SchoolClassImportCommitHandlerTests {

    private SchoolClassRepository schoolClassRepository;
    private SupportedLanguageRepository supportedLanguageRepository;
    private SchoolGradeRepository schoolGradeRepository;
    private FakeJsonSerializationPort jsonSerializationPort;
    private PlatformTransactionManager txManager;
    private SchoolClassImportCommitHandler handler;

    @BeforeEach
    void setUp() {
        schoolClassRepository = mock(SchoolClassRepository.class);
        supportedLanguageRepository = mock(SupportedLanguageRepository.class);
        schoolGradeRepository = mock(SchoolGradeRepository.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        handler = new SchoolClassImportCommitHandler(
            schoolClassRepository,
            supportedLanguageRepository,
            schoolGradeRepository,
            jsonSerializationPort,
            txManager
        );
    }

    @Test
    void should_import_valid_rows_and_mark_invalid_rows() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
        var rows = List.of(
            row(sessionId, 1L, classData("ENG-01", "English 01", "EN", "G10")),
            row(sessionId, 2L, classData("", "Missing code", "EN", "G10"))
        );

        when(supportedLanguageRepository.findByCodeIn(Set.of("EN"))).thenReturn(List.of(activeLanguage(languageId, "EN")));
        when(schoolGradeRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("G10"))).thenReturn(List.of(activeGrade(gradeId, "G10")));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"))).thenReturn(List.of());
        when(schoolClassRepository.save(any(SchoolClass.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.updated()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        verify(schoolClassRepository).save(any(SchoolClass.class));
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(1).getErrorsJson()).contains("code");
    }

    @Test
    void should_update_existing_class_when_code_exists_in_school() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var existingClassId = UUID.randomUUID();
        var oldLanguageId = UUID.randomUUID();
        var newLanguageId = UUID.randomUUID();
        var oldGradeId = UUID.randomUUID();
        var newGradeId = UUID.randomUUID();
        var createdAt = OffsetDateTime.now().minusDays(10);
        var existingClass = new SchoolClass(
            existingClassId, schoolId, oldLanguageId, oldGradeId,
            new ClassCode("ENG-01"), "English Old", "Old description",
            SchoolClassStatus.ACTIVE, createdAt, createdAt, createdBy, createdBy
        );
        var rows = List.of(row(sessionId, 1L, classDataWithDescription("ENG-01", "English Updated", "VI", "G11", "Updated description")));

        when(supportedLanguageRepository.findByCodeIn(Set.of("VI"))).thenReturn(List.of(activeLanguage(newLanguageId, "VI")));
        when(schoolGradeRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("G11"))).thenReturn(List.of(activeGrade(newGradeId, "G11")));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"))).thenReturn(List.of(existingClass));
        when(schoolClassRepository.save(any(SchoolClass.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(sessionWithDescription(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isZero();
        assertThat(result.updated()).isEqualTo(1L);
        assertThat(result.invalid()).isZero();
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(existingClass.getName()).isEqualTo("English Updated");
        assertThat(existingClass.getDescription()).isEqualTo("Updated description");
        assertThat(existingClass.getLanguageId()).isEqualTo(newLanguageId);
        assertThat(existingClass.getSchoolGradeId()).isEqualTo(newGradeId);
        assertThat(existingClass.getCreatedAt()).isEqualTo(createdAt);
        assertThat(existingClass.getUpdatedBy()).isEqualTo(createdBy);
        verify(schoolClassRepository).save(existingClass);
    }

    @Test
    void should_keep_duplicate_code_in_same_file_invalid() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
        var rows = List.of(
            row(sessionId, 1L, classData("ENG-01", "English 01", "EN", "G10")),
            row(sessionId, 2L, classData("ENG-01", "English Duplicate", "EN", "G10"))
        );

        when(supportedLanguageRepository.findByCodeIn(Set.of("EN"))).thenReturn(List.of(activeLanguage(languageId, "EN")));
        when(schoolGradeRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("G10"))).thenReturn(List.of(activeGrade(gradeId, "G10")));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"))).thenReturn(List.of());
        when(schoolClassRepository.save(any(SchoolClass.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.updated()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(1).getErrorsJson()).contains("code");
    }

    @Test
    void should_mark_invalid_when_language_not_found() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
        var rows = List.of(row(sessionId, 1L, classData("ENG-01", "English 01", "XX", "G10")));

        when(supportedLanguageRepository.findByCodeIn(Set.of("XX"))).thenReturn(List.of());
        when(schoolGradeRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("G10"))).thenReturn(List.of(activeGrade(gradeId, "G10")));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"))).thenReturn(List.of());

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(0).getErrorsJson()).contains("languageCode");
        verify(schoolClassRepository, never()).save(any());
    }

    @Test
    void should_mark_invalid_when_grade_inactive() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
        var inactiveGrade = new SchoolGrade();
        inactiveGrade.setId(gradeId);
        inactiveGrade.setCode("G10");
        inactiveGrade.setStatus(SchoolGradeStatus.INACTIVE);
        var rows = List.of(row(sessionId, 1L, classData("ENG-01", "English 01", "EN", "G10")));

        when(supportedLanguageRepository.findByCodeIn(Set.of("EN"))).thenReturn(List.of(activeLanguage(languageId, "EN")));
        when(schoolGradeRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("G10"))).thenReturn(List.of(inactiveGrade));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"))).thenReturn(List.of());

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(0).getErrorsJson()).contains("schoolGradeCode");
        verify(schoolClassRepository, never()).save(any());
    }

    private ImportRow row(UUID sessionId, long rowNumber, Map<String, String> rawData) {
        return new ImportRow(UUID.randomUUID(), sessionId, rowNumber, jsonSerializationPort.toJson(rawData), null, null, ImportRowStatus.PENDING);
    }

    private ImportSession session(UUID id, UUID schoolId, UUID createdBy) {
        return buildSession(id, schoolId, createdBy, mapping());
    }

    private ImportSession sessionWithDescription(UUID id, UUID schoolId, UUID createdBy) {
        return buildSession(id, schoolId, createdBy, mappingWithDescription());
    }

    private ImportSession buildSession(UUID id, UUID schoolId, UUID createdBy, Map<String, String> mapping) {
        return new ImportSession(
            id, schoolId, ImportType.SCHOOL_CLASS, "classes.csv", "[]", "{}",
            jsonSerializationPort.toJson(mapping),
            0L, 0L, 0L, 0L, 0L, null, ImportSessionStatus.IMPORTING, null,
            OffsetDateTime.now().plusDays(1), null, null, null, 0,
            OffsetDateTime.now(), OffsetDateTime.now(),
            createdBy, createdBy
        );
    }

    private static Map<String, String> mapping() {
        return Map.of(
            "Mã lớp", "code", "Tên lớp", "name", "Ngôn ngữ", "languageCode", "Khối", "schoolGradeCode"
        );
    }

    private static Map<String, String> mappingWithDescription() {
        return Map.of(
            "Mã lớp", "code", "Tên lớp", "name", "Ngôn ngữ", "languageCode",
            "Khối", "schoolGradeCode", "Mô tả", "description"
        );
    }

    private static Map<String, String> classData(String code, String name, String languageCode, String gradeCode) {
        return Map.of("Mã lớp", code, "Tên lớp", name, "Ngôn ngữ", languageCode, "Khối", gradeCode);
    }

    private static Map<String, String> classDataWithDescription(String code, String name, String languageCode, String gradeCode, String description) {
        return Map.of("Mã lớp", code, "Tên lớp", name, "Ngôn ngữ", languageCode, "Khối", gradeCode, "Mô tả", description);
    }

    private static SupportedLanguage activeLanguage(UUID id, String code) {
        var language = new SupportedLanguage();
        language.setId(id);
        language.setCode(new LanguageCode(code));
        language.setActive(true);
        return language;
    }

    private static SchoolGrade activeGrade(UUID id, String code) {
        var grade = new SchoolGrade();
        grade.setId(id);
        grade.setCode(code);
        grade.setStatus(SchoolGradeStatus.ACTIVE);
        return grade;
    }
}
