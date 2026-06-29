package com.sep.vox.application.usecase.schoolgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;

class SchoolGradeImportCommitHandlerTests {

    private SchoolGradeRepository schoolGradeRepository;
    private SchoolGradeLevelRepository schoolGradeLevelRepository;
    private FakeJsonSerializationPort jsonSerializationPort;
    private PlatformTransactionManager txManager;
    private SchoolGradeImportCommitHandler handler;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID createdBy = UUID.randomUUID();
    private final UUID levelId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolGradeRepository = mock(SchoolGradeRepository.class);
        schoolGradeLevelRepository = mock(SchoolGradeLevelRepository.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        handler = new SchoolGradeImportCommitHandler(
            schoolGradeRepository,
            schoolGradeLevelRepository,
            jsonSerializationPort,
            txManager
        );
    }

    @Test
    void should_create_valid_rows_and_mark_invalid_rows() {
        var sessionId = UUID.randomUUID();
        var rows = List.of(
            row(sessionId, 1L, data("K10", "2024", "Năm 2024", "2024-09-05", "2025-05-30")),
            row(sessionId, 2L, data("K10", "", "Thiếu mã", "2024-09-05", "2025-05-30"))
        );

        when(schoolGradeLevelRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("K10"))).thenReturn(List.of(gradeLevel("K10")));
        when(schoolGradeRepository.findBySchoolGradeLevelIdAndCode(eq(levelId), any())).thenReturn(Optional.empty());
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
    void should_update_existing_grade_when_code_exists() {
        var sessionId = UUID.randomUUID();
        var createdAt = OffsetDateTime.now().minusDays(5);
        var existing = new SchoolGrade(
            UUID.randomUUID(), levelId, "2024", "Năm cũ", "Mô tả cũ",
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 1),
            SchoolGradeStatus.ACTIVE, createdAt, createdAt, createdBy, createdBy
        );
        var rows = List.of(row(sessionId, 1L, data("K10", "2024", "Năm mới", "2024-09-05", "2025-05-30")));

        when(schoolGradeLevelRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("K10"))).thenReturn(List.of(gradeLevel("K10")));
        when(schoolGradeRepository.findBySchoolGradeLevelIdAndCode(levelId, "2024")).thenReturn(Optional.of(existing));
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
        var sessionId = UUID.randomUUID();
        var rows = List.of(row(sessionId, 1L, data("KXX", "2024", "Năm 2024", "2024-09-05", "2025-05-30")));

        when(schoolGradeLevelRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("KXX"))).thenReturn(List.of());

        ImportCommitResult result = handler.commit(session(sessionId), rows);

        assertThat(result.created()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(0).getErrorsJson()).contains("schoolGradeLevelCode");
        verify(schoolGradeRepository, never()).save(any());
    }

    @Test
    void should_mark_invalid_when_dates_invalid() {
        var sessionId = UUID.randomUUID();
        var rows = List.of(
            row(sessionId, 1L, data("K10", "2024", "Năm 2024", "khong-phai-ngay", "2025-05-30")),
            row(sessionId, 2L, data("K10", "2025", "Năm 2025", "2025-05-30", "2024-09-05"))
        );

        when(schoolGradeLevelRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("K10"))).thenReturn(List.of(gradeLevel("K10")));

        ImportCommitResult result = handler.commit(session(sessionId), rows);

        assertThat(result.created()).isZero();
        assertThat(result.invalid()).isEqualTo(2L);
        assertThat(rows.get(0).getErrorsJson()).contains("startDate");
        assertThat(rows.get(1).getErrorsJson()).contains("endDate");
        verify(schoolGradeRepository, never()).save(any());
    }

    @Test
    void should_keep_duplicate_level_code_pair_invalid() {
        var sessionId = UUID.randomUUID();
        var rows = List.of(
            row(sessionId, 1L, data("K10", "2024", "Năm 2024", "2024-09-05", "2025-05-30")),
            row(sessionId, 2L, data("K10", "2024", "Năm 2024 trùng", "2024-09-05", "2025-05-30"))
        );

        when(schoolGradeLevelRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("K10"))).thenReturn(List.of(gradeLevel("K10")));
        when(schoolGradeRepository.findBySchoolGradeLevelIdAndCode(eq(levelId), any())).thenReturn(Optional.empty());
        when(schoolGradeRepository.save(any(SchoolGrade.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(1).getErrorsJson()).contains("code");
    }

    @Test
    void should_mark_failed_when_unique_constraint_violated() {
        var sessionId = UUID.randomUUID();
        var rows = List.of(row(sessionId, 1L, data("K10", "2024", "Năm 2024", "2024-09-05", "2025-05-30")));

        when(schoolGradeLevelRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("K10"))).thenReturn(List.of(gradeLevel("K10")));
        when(schoolGradeRepository.findBySchoolGradeLevelIdAndCode(eq(levelId), any())).thenReturn(Optional.empty());
        when(schoolGradeRepository.save(any(SchoolGrade.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate code"));

        ImportCommitResult result = handler.commit(session(sessionId), rows);

        assertThat(result.created()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.FAILED);
        assertThat(rows.get(0).getErrorsJson()).contains("code");
    }

    private SchoolGradeLevel gradeLevel(String code) {
        var now = OffsetDateTime.now();
        return new SchoolGradeLevel(levelId, schoolId, code, "Khối " + code, null, 1,
            SchoolGradeLevelStatus.ACTIVE, now, now, createdBy, createdBy);
    }

    private ImportRow row(UUID sessionId, long rowNumber, Map<String, String> rawData) {
        return new ImportRow(UUID.randomUUID(), sessionId, rowNumber, jsonSerializationPort.toJson(rawData), null, null, ImportRowStatus.PENDING);
    }

    private ImportSession session(UUID id) {
        return new ImportSession(
            id, schoolId, ImportType.SCHOOL_GRADE, "grades.csv", "[]", "{}",
            jsonSerializationPort.toJson(mapping()),
            0L, 0L, 0L, 0L, 0L, null, ImportSessionStatus.IMPORTING, null,
            OffsetDateTime.now().plusDays(1), null, null, null, 0,
            OffsetDateTime.now(), OffsetDateTime.now(),
            createdBy, createdBy
        );
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
