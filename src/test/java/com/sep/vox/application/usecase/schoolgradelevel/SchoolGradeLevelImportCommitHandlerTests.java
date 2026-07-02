package com.sep.vox.application.usecase.schoolgradelevel;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import com.sep.vox.application.port.input.service.ImportCommitResult;
import com.sep.vox.application.port.input.service.SchoolGradeLevelImportCommitHandler;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;

class SchoolGradeLevelImportCommitHandlerTests {

    private SchoolGradeLevelRepository schoolGradeLevelRepository;
    private FakeJsonSerializationPort jsonSerializationPort;
    private PlatformTransactionManager txManager;
    private SchoolGradeLevelImportCommitHandler handler;

    @BeforeEach
    void setUp() {
        schoolGradeLevelRepository = mock(SchoolGradeLevelRepository.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        handler = new SchoolGradeLevelImportCommitHandler(
            schoolGradeLevelRepository,
            jsonSerializationPort,
            txManager
        );
    }

    @Test
    void should_create_valid_rows_and_mark_invalid_rows() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var rows = List.of(
            row(sessionId, 1L, data("K10", "Khối 10", "1")),
            row(sessionId, 2L, data("", "Thiếu mã", "2"))
        );

        when(schoolGradeLevelRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("K10"))).thenReturn(List.of());
        when(schoolGradeLevelRepository.save(any(SchoolGradeLevel.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.updated()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        verify(schoolGradeLevelRepository).save(any(SchoolGradeLevel.class));
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(1).getErrorsJson()).contains("code");
    }

    @Test
    void should_update_existing_grade_level_when_code_exists() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var createdAt = OffsetDateTime.now().minusDays(5);
        var existing = new SchoolGradeLevel(
            UUID.randomUUID(), schoolId, "K10", "Khối 10 cũ", "Mô tả cũ", 1,
            SchoolGradeLevelStatus.ACTIVE, createdAt, createdAt, createdBy, createdBy
        );
        var rows = List.of(row(sessionId, 1L, dataWithDescription("K10", "Khối 10 mới", "3", "Mô tả mới")));

        when(schoolGradeLevelRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("K10"))).thenReturn(List.of(existing));
        when(schoolGradeLevelRepository.save(any(SchoolGradeLevel.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(sessionWithDescription(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isZero();
        assertThat(result.updated()).isEqualTo(1L);
        assertThat(result.invalid()).isZero();
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(existing.getName()).isEqualTo("Khối 10 mới");
        assertThat(existing.getDescription()).isEqualTo("Mô tả mới");
        assertThat(existing.getOrder()).isEqualTo(3);
        assertThat(existing.getCreatedAt()).isEqualTo(createdAt);
        assertThat(existing.getUpdatedBy()).isEqualTo(createdBy);
        verify(schoolGradeLevelRepository).save(existing);
    }

    @Test
    void should_keep_duplicate_code_in_same_file_invalid() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var rows = List.of(
            row(sessionId, 1L, data("K10", "Khối 10", "1")),
            row(sessionId, 2L, data("K10", "Khối 10 trùng", "2"))
        );

        when(schoolGradeLevelRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("K10"))).thenReturn(List.of());
        when(schoolGradeLevelRepository.save(any(SchoolGradeLevel.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(1).getErrorsJson()).contains("code");
    }

    @Test
    void should_mark_invalid_when_order_not_positive_integer() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var rows = List.of(
            row(sessionId, 1L, data("K10", "Khối 10", "abc")),
            row(sessionId, 2L, data("K11", "Khối 11", "0"))
        );

        when(schoolGradeLevelRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("K10", "K11"))).thenReturn(List.of());

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isZero();
        assertThat(result.invalid()).isEqualTo(2L);
        assertThat(rows.get(0).getErrorsJson()).contains("order");
        assertThat(rows.get(1).getErrorsJson()).contains("order");
        verify(schoolGradeLevelRepository, never()).save(any());
    }

    @Test
    void should_mark_failed_when_unique_constraint_violated() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var rows = List.of(row(sessionId, 1L, data("K10", "Khối 10", "1")));

        when(schoolGradeLevelRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("K10"))).thenReturn(List.of());
        when(schoolGradeLevelRepository.save(any(SchoolGradeLevel.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate order"));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.FAILED);
        assertThat(rows.get(0).getErrorsJson()).contains("code");
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
            id, schoolId, ImportType.SCHOOL_GRADE_LEVEL, "grade-levels.csv", "[]", "{}",
            jsonSerializationPort.toJson(mapping),
            0L, 0L, 0L, 0L, 0L, null, ImportSessionStatus.IMPORTING, null,
            OffsetDateTime.now().plusDays(1), null, null, null, 0,
            OffsetDateTime.now(), OffsetDateTime.now(),
            createdBy, createdBy
        );
    }

    private static Map<String, String> mapping() {
        return Map.of("Mã khối", "code", "Tên khối", "name", "Thứ tự", "order");
    }

    private static Map<String, String> mappingWithDescription() {
        return Map.of("Mã khối", "code", "Tên khối", "name", "Thứ tự", "order", "Mô tả", "description");
    }

    private static Map<String, String> data(String code, String name, String order) {
        return Map.of("Mã khối", code, "Tên khối", name, "Thứ tự", order);
    }

    private static Map<String, String> dataWithDescription(String code, String name, String order, String description) {
        return Map.of("Mã khối", code, "Tên khối", name, "Thứ tự", order, "Mô tả", description);
    }
}
