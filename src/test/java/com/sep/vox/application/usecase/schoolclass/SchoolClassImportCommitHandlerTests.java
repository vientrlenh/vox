package com.sep.vox.application.usecase.schoolclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.application.port.output.RequiresNewTransactionPort;
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
    private RequiresNewTransactionPort requiresNewTransactionPort;
    private SchoolClassImportCommitHandler handler;

    @BeforeEach
    void setUp() {
        schoolClassRepository = mock(SchoolClassRepository.class);
        supportedLanguageRepository = mock(SupportedLanguageRepository.class);
        schoolGradeRepository = mock(SchoolGradeRepository.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        requiresNewTransactionPort = mock(RequiresNewTransactionPort.class);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(requiresNewTransactionPort).runInNewTransaction(any());
        handler = new SchoolClassImportCommitHandler(
            schoolClassRepository,
            supportedLanguageRepository,
            schoolGradeRepository,
            jsonSerializationPort,
            requiresNewTransactionPort
        );
    }

    @Test
    void should_import_valid_rows_and_mark_invalid_rows() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID languageId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        List<ImportRow> rows = List.of(
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
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID existingClassId = UUID.randomUUID();
        UUID oldLanguageId = UUID.randomUUID();
        UUID newLanguageId = UUID.randomUUID();
        UUID oldGradeId = UUID.randomUUID();
        UUID newGradeId = UUID.randomUUID();
        Instant createdAt = Instant.now().minus(10, ChronoUnit.DAYS);
        SchoolClass existingClass = SchoolClass.builder()
            .id(existingClassId)
            .schoolId(schoolId)
            .languageId(oldLanguageId)
            .schoolGradeId(oldGradeId)
            .code(new ClassCode("ENG-01"))
            .name("English Old")
            .description("Old description")
            .status(SchoolClassStatus.ACTIVE)
            .createdAt(createdAt)
            .updatedAt(createdAt)
            .createdBy(createdBy.toString())
            .updatedBy(createdBy.toString())
            .build();
        List<ImportRow> rows = List.of(row(sessionId, 1L, classDataWithDescription("ENG-01", "English Updated", "VI", "G11", "Updated description")));

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
        verify(schoolClassRepository).save(existingClass);
    }

    @Test
    void should_keep_duplicate_code_in_same_file_invalid() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID languageId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        List<ImportRow> rows = List.of(
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
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        List<ImportRow> rows = List.of(row(sessionId, 1L, classData("ENG-01", "English 01", "XX", "G10")));

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
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID languageId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        SchoolGrade inactiveGrade = new SchoolGrade();
        inactiveGrade.setId(gradeId);
        inactiveGrade.setCode("G10");
        inactiveGrade.setStatus(SchoolGradeStatus.INACTIVE);
        List<ImportRow> rows = List.of(row(sessionId, 1L, classData("ENG-01", "English 01", "EN", "G10")));

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
        return ImportRow.builder()
            .id(UUID.randomUUID())
            .sessionId(sessionId)
            .rowNumber(rowNumber)
            .rawDataJson(jsonSerializationPort.toJson(rawData))
            .mappedDataJson(null)
            .errorsJson(null)
            .status(ImportRowStatus.PENDING)
            .build();
    }

    private ImportSession session(UUID id, UUID schoolId, UUID createdBy) {
        return buildSession(id, schoolId, createdBy, mapping());
    }

    private ImportSession sessionWithDescription(UUID id, UUID schoolId, UUID createdBy) {
        return buildSession(id, schoolId, createdBy, mappingWithDescription());
    }

    private ImportSession buildSession(UUID id, UUID schoolId, UUID createdBy, Map<String, String> mapping) {
        return ImportSession.builder()
            .id(id)
            .schoolId(schoolId)
            .type(ImportType.SCHOOL_CLASS)
            .fileName("classes.csv")
            .originalHeadersJson("[]")
            .suggestedMappingJson("{}")
            .confirmedMappingJson(jsonSerializationPort.toJson(mapping))
            .validRows(0L)
            .invalidRows(0L)
            .importedRows(0L)
            .skippedRows(0L)
            .totalRows(0L)
            .failureReason(null)
            .status(ImportSessionStatus.IMPORTING)
            .importedEntityId(null)
            .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
            .claimedAt(null)
            .claimedBy(null)
            .leaseExpiresAt(null)
            .attempts(0)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(createdBy.toString())
            .updatedBy(createdBy.toString())
            .build();
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
        SupportedLanguage language = new SupportedLanguage();
        language.setId(id);
        language.setCode(new LanguageCode(code));
        language.setActive(true);
        return language;
    }

    private static SchoolGrade activeGrade(UUID id, String code) {
        SchoolGrade grade = new SchoolGrade();
        grade.setId(id);
        grade.setCode(code);
        grade.setStatus(SchoolGradeStatus.ACTIVE);
        return grade;
    }

    @Test
    void should_mark_failed_when_repository_reports_a_data_conflict() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        List<ImportRow> rows = List.of(row(sessionId, 1L, classData("ENG-01", "English 01", "EN", "G10")));
        when(supportedLanguageRepository.findByCodeIn(Set.of("EN"))).thenReturn(List.of(activeLanguage(UUID.randomUUID(), "EN")));
        when(schoolGradeRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("G10"))).thenReturn(List.of(activeGrade(UUID.randomUUID(), "G10")));
        when(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("ENG-01"))).thenReturn(List.of());
        when(schoolClassRepository.save(any(SchoolClass.class))).thenThrow(new DataConflictException("unique violation", null));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.FAILED);
        assertThat(rows.get(0).getErrorsJson()).contains("Class code already exists or is invalid");
    }
}
