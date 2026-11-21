package com.sep.vox.application.usecase.schoolgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import com.sep.vox.application.port.input.service.ImportCommitResult;
import com.sep.vox.application.port.input.service.SchoolGradeImportCommitHandler;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;

class SchoolGradeImportCommitHandlerTests {

    private SchoolGradeRepository schoolGradeRepository;
    private GradeLevelRepository gradeLevelRepository;
    private FakeJsonSerializationPort jsonSerializationPort;
    private PlatformTransactionManager txManager;
    private SchoolGradeImportCommitHandler handler;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID createdBy = UUID.randomUUID();
    private final UUID levelId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolGradeRepository = mock(SchoolGradeRepository.class);
        gradeLevelRepository = mock(GradeLevelRepository.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        handler = new SchoolGradeImportCommitHandler(
            schoolGradeRepository,
            gradeLevelRepository,
            jsonSerializationPort,
            txManager
        );
    }

    @Test
    void should_create_valid_rows_and_mark_invalid_rows() {
        UUID sessionId = UUID.randomUUID();
        List<ImportRow> rows = List.of(
            row(sessionId, 1L, data("K10", "2024", "Năm 2024", "2024-09-05", "2025-05-30")),
            row(sessionId, 2L, data("K10", "", "Thiếu mã", "2024-09-05", "2025-05-30"))
        );

        when(gradeLevelRepository.findByCodeIn(Set.of("K10"))).thenReturn(List.of(gradeLevel("K10")));
        when(schoolGradeRepository.findBySchoolIdAndGradeLevelIdAndCode(eq(schoolId), eq(levelId), any())).thenReturn(Optional.empty());
        when(schoolGradeRepository.save(any(SchoolGrade.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.updated()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(1).getErrorsJson()).contains("code");
        verify(schoolGradeRepository).save(any(SchoolGrade.class));
    }

    @Test
    void should_create_grade_with_active_status() {
        UUID sessionId = UUID.randomUUID();
        List<ImportRow> rows = List.of(row(sessionId, 1L, data("K10", "2024", "Năm 2024", "2024-09-05", "2025-05-30")));

        when(gradeLevelRepository.findByCodeIn(Set.of("K10"))).thenReturn(List.of(gradeLevel("K10")));
        when(schoolGradeRepository.findBySchoolIdAndGradeLevelIdAndCode(eq(schoolId), eq(levelId), any())).thenReturn(Optional.empty());
        when(schoolGradeRepository.save(any(SchoolGrade.class))).thenAnswer(inv -> inv.getArgument(0));

        handler.commit(session(sessionId), rows);

        ArgumentCaptor<SchoolGrade> captor = ArgumentCaptor.forClass(SchoolGrade.class);
        verify(schoolGradeRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SchoolGradeStatus.ACTIVE);
    }

    @Test
    void should_update_existing_grade_when_code_exists() {
        UUID sessionId = UUID.randomUUID();
        Instant createdAt = Instant.now().minus(5, ChronoUnit.DAYS);
        SchoolGrade existing = SchoolGrade.builder()
            .id(UUID.randomUUID())
            .schoolId(schoolId)
            .gradeLevelId(levelId)
            .code("2024")
            .name("Năm cũ")
            .description("Mô tả cũ")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 6, 1))
            .status(SchoolGradeStatus.ACTIVE)
            .createdAt(createdAt)
            .updatedAt(createdAt)
            .createdBy(createdBy.toString())
            .updatedBy(createdBy.toString())
            .build();
        List<ImportRow> rows = List.of(row(sessionId, 1L, data("K10", "2024", "Năm mới", "2024-09-05", "2025-05-30")));

        when(gradeLevelRepository.findByCodeIn(Set.of("K10"))).thenReturn(List.of(gradeLevel("K10")));
        when(schoolGradeRepository.findBySchoolIdAndGradeLevelIdAndCode(schoolId, levelId, "2024")).thenReturn(Optional.of(existing));
        when(schoolGradeRepository.save(any(SchoolGrade.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId), rows);

        assertThat(result.created()).isZero();
        assertThat(result.updated()).isEqualTo(1L);
        assertThat(result.invalid()).isZero();
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(existing.getName()).isEqualTo("Năm mới");
        assertThat(existing.getStartDate()).isEqualTo(LocalDate.of(2024, 9, 5));
        assertThat(existing.getEndDate()).isEqualTo(LocalDate.of(2025, 5, 30));
        assertThat(existing.getCreatedAt()).isEqualTo(createdAt);
        verify(schoolGradeRepository).save(existing);
    }

    @Test
    void should_mark_invalid_when_grade_level_not_found() {
        UUID sessionId = UUID.randomUUID();
        List<ImportRow> rows = List.of(row(sessionId, 1L, data("KXX", "2024", "Năm 2024", "2024-09-05", "2025-05-30")));

        when(gradeLevelRepository.findByCodeIn(Set.of("KXX"))).thenReturn(List.of());

        ImportCommitResult result = handler.commit(session(sessionId), rows);

        assertThat(result.created()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(0).getErrorsJson()).contains("schoolGradeLevelCode");
        verify(schoolGradeRepository, never()).save(any());
    }

    @Test
    void should_mark_invalid_when_dates_invalid() {
        UUID sessionId = UUID.randomUUID();
        List<ImportRow> rows = List.of(
            row(sessionId, 1L, data("K10", "2024", "Năm 2024", "khong-phai-ngay", "2025-05-30")),
            row(sessionId, 2L, data("K10", "2025", "Năm 2025", "2025-05-30", "2024-09-05"))
        );

        when(gradeLevelRepository.findByCodeIn(Set.of("K10"))).thenReturn(List.of(gradeLevel("K10")));

        ImportCommitResult result = handler.commit(session(sessionId), rows);

        assertThat(result.created()).isZero();
        assertThat(result.invalid()).isEqualTo(2L);
        assertThat(rows.get(0).getErrorsJson()).contains("startDate");
        assertThat(rows.get(1).getErrorsJson()).contains("endDate");
        verify(schoolGradeRepository, never()).save(any());
    }

    @Test
    void should_keep_duplicate_level_code_pair_invalid() {
        UUID sessionId = UUID.randomUUID();
        List<ImportRow> rows = List.of(
            row(sessionId, 1L, data("K10", "2024", "Năm 2024", "2024-09-05", "2025-05-30")),
            row(sessionId, 2L, data("K10", "2024", "Năm 2024 trùng", "2024-09-05", "2025-05-30"))
        );

        when(gradeLevelRepository.findByCodeIn(Set.of("K10"))).thenReturn(List.of(gradeLevel("K10")));
        when(schoolGradeRepository.findBySchoolIdAndGradeLevelIdAndCode(eq(schoolId), eq(levelId), any())).thenReturn(Optional.empty());
        when(schoolGradeRepository.save(any(SchoolGrade.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(1).getErrorsJson()).contains("code");
    }

    @Test
    void should_mark_failed_when_unique_constraint_violated() {
        UUID sessionId = UUID.randomUUID();
        List<ImportRow> rows = List.of(row(sessionId, 1L, data("K10", "2024", "Năm 2024", "2024-09-05", "2025-05-30")));

        when(gradeLevelRepository.findByCodeIn(Set.of("K10"))).thenReturn(List.of(gradeLevel("K10")));
        when(schoolGradeRepository.findBySchoolIdAndGradeLevelIdAndCode(eq(schoolId), eq(levelId), any())).thenReturn(Optional.empty());
        when(schoolGradeRepository.save(any(SchoolGrade.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate code"));

        ImportCommitResult result = handler.commit(session(sessionId), rows);

        assertThat(result.created()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.FAILED);
        assertThat(rows.get(0).getErrorsJson()).contains("code");
    }

    private GradeLevel gradeLevel(String code) {
        return GradeLevel.builder()
            .id(levelId)
            .code(code)
            .name("Khối " + code)
            .description(null)
            .order(1)
            .status(GradeLevelStatus.ACTIVE)
            .build();
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

    private ImportSession session(UUID id) {
        return ImportSession.builder()
            .id(id)
            .schoolId(schoolId)
            .type(ImportType.SCHOOL_GRADE)
            .fileName("grades.csv")
            .originalHeadersJson("[]")
            .suggestedMappingJson("{}")
            .confirmedMappingJson(jsonSerializationPort.toJson(mapping()))
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
            "Mã khối", "schoolGradeLevelCode",
            "Mã năm học", "code",
            "Tên", "name",
            "Bắt đầu", "startDate",
            "Kết thúc", "endDate");
    }

    private static Map<String, String> data(String levelCode, String code, String name, String startDate, String endDate) {
        return Map.of(
            "Mã khối", levelCode,
            "Mã năm học", code,
            "Tên", name,
            "Bắt đầu", startDate,
            "Kết thúc", endDate);
    }
}
