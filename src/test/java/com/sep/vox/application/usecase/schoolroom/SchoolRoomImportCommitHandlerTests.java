package com.sep.vox.application.usecase.schoolroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import com.sep.vox.application.port.input.service.ImportCommitResult;
import com.sep.vox.application.port.input.service.SchoolRoomImportCommitHandler;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.repository.SchoolRoomRepository;

class SchoolRoomImportCommitHandlerTests {

    private SchoolRoomRepository schoolRoomRepository;
    private FakeJsonSerializationPort jsonSerializationPort;
    private PlatformTransactionManager txManager;
    private SchoolRoomImportCommitHandler handler;

    @BeforeEach
    void setUp() {
        schoolRoomRepository = mock(SchoolRoomRepository.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        handler = new SchoolRoomImportCommitHandler(
            schoolRoomRepository,
            jsonSerializationPort,
            txManager
        );
    }

    @Test
    void should_create_valid_rows_and_mark_invalid_rows() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        List<ImportRow> rows = List.of(
            row(sessionId, 1L, data("P101", "Phòng 101")),
            row(sessionId, 2L, data("", "Thiếu mã"))
        );

        when(schoolRoomRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("P101"))).thenReturn(List.of());
        when(schoolRoomRepository.save(any(SchoolRoom.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.updated()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        verify(schoolRoomRepository).save(any(SchoolRoom.class));
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(1).getErrorsJson()).contains("code");
    }

    @Test
    void should_create_room_active_by_default() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        List<ImportRow> rows = List.of(row(sessionId, 1L, data("P101", "Phòng 101")));

        when(schoolRoomRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("P101"))).thenReturn(List.of());
        ArgumentCaptor<SchoolRoom> captor = org.mockito.ArgumentCaptor.forClass(SchoolRoom.class);
        when(schoolRoomRepository.save(any(SchoolRoom.class))).thenAnswer(inv -> inv.getArgument(0));

        handler.commit(session(sessionId, schoolId, createdBy), rows);

        verify(schoolRoomRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(captor.getValue().getName()).isEqualTo("Phòng 101");
    }

    @Test
    void should_update_existing_room_when_code_exists() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant createdAt = Instant.now().minus(5, ChronoUnit.DAYS);
        SchoolRoom existing = SchoolRoom.builder()
            .id(UUID.randomUUID())
            .schoolId(schoolId)
            .code("P101")
            .name("Phòng cũ")
            .description("Mô tả cũ")
            .active(true)
            .createdAt(createdAt)
            .updatedAt(createdAt)
            .createdBy(createdBy.toString())
            .updatedBy(createdBy.toString())
            .build();
        List<ImportRow> rows = List.of(row(sessionId, 1L, dataWithDescription("P101", "Phòng mới", "Mô tả mới")));

        when(schoolRoomRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("P101"))).thenReturn(List.of(existing));
        when(schoolRoomRepository.save(any(SchoolRoom.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(sessionWithDescription(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isZero();
        assertThat(result.updated()).isEqualTo(1L);
        assertThat(result.invalid()).isZero();
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        assertThat(existing.getName()).isEqualTo("Phòng mới");
        assertThat(existing.getDescription()).isEqualTo("Mô tả mới");
        assertThat(existing.getCreatedAt()).isEqualTo(createdAt);
        verify(schoolRoomRepository).save(existing);
    }

    @Test
    void should_reactivate_existing_inactive_room() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant createdAt = Instant.now().minus(5, ChronoUnit.DAYS);
        SchoolRoom existing = SchoolRoom.builder()
            .id(UUID.randomUUID())
            .schoolId(schoolId)
            .code("P101")
            .name("Phòng cũ")
            .description("Mô tả cũ")
            .active(false)
            .createdAt(createdAt)
            .updatedAt(createdAt)
            .createdBy(createdBy.toString())
            .updatedBy(createdBy.toString())
            .build();
        List<ImportRow> rows = List.of(row(sessionId, 1L, dataWithDescription("P101", "Phòng mới", "Mô tả mới")));

        when(schoolRoomRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("P101"))).thenReturn(List.of(existing));
        when(schoolRoomRepository.save(any(SchoolRoom.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(sessionWithDescription(sessionId, schoolId, createdBy), rows);

        assertThat(result.updated()).isEqualTo(1L);
        assertThat(existing.isActive()).isTrue();
        assertThat(existing.getName()).isEqualTo("Phòng mới");
        assertThat(existing.getDescription()).isEqualTo("Mô tả mới");
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.IMPORTED);
        verify(schoolRoomRepository).save(existing);
    }

    @Test
    void should_keep_duplicate_code_in_same_file_invalid() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        List<ImportRow> rows = List.of(
            row(sessionId, 1L, data("P101", "Phòng 101")),
            row(sessionId, 2L, data("P101", "Phòng trùng"))
        );

        when(schoolRoomRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("P101"))).thenReturn(List.of());
        when(schoolRoomRepository.save(any(SchoolRoom.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isEqualTo(1L);
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(1).getStatus()).isEqualTo(ImportRowStatus.INVALID);
        assertThat(rows.get(1).getErrorsJson()).contains("code");
    }

    @Test
    void should_mark_failed_when_unique_constraint_violated() {
        UUID schoolId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        List<ImportRow> rows = List.of(row(sessionId, 1L, data("P101", "Phòng 101")));

        when(schoolRoomRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("P101"))).thenReturn(List.of());
        when(schoolRoomRepository.save(any(SchoolRoom.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate code"));

        ImportCommitResult result = handler.commit(session(sessionId, schoolId, createdBy), rows);

        assertThat(result.created()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ImportRowStatus.FAILED);
        assertThat(rows.get(0).getErrorsJson()).contains("code");
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
            .type(ImportType.SCHOOL_ROOM)
            .fileName("rooms.csv")
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
        return Map.of("Mã phòng", "code", "Tên phòng", "name");
    }

    private static Map<String, String> mappingWithDescription() {
        return Map.of("Mã phòng", "code", "Tên phòng", "name", "Mô tả", "description");
    }

    private static Map<String, String> data(String code, String name) {
        return Map.of("Mã phòng", code, "Tên phòng", name);
    }

    private static Map<String, String> dataWithDescription(String code, String name, String description) {
        return Map.of("Mã phòng", code, "Tên phòng", name, "Mô tả", description);
    }
}
