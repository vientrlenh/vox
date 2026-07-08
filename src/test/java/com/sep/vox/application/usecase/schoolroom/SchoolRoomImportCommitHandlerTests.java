package com.sep.vox.application.usecase.schoolroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var rows = List.of(
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
    void should_create_room_inactive_by_default() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var rows = List.of(row(sessionId, 1L, data("P101", "Phòng 101")));

        when(schoolRoomRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("P101"))).thenReturn(List.of());
        var captor = org.mockito.ArgumentCaptor.forClass(SchoolRoom.class);
        when(schoolRoomRepository.save(any(SchoolRoom.class))).thenAnswer(inv -> inv.getArgument(0));

        handler.commit(session(sessionId, schoolId, createdBy), rows);

        verify(schoolRoomRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
        assertThat(captor.getValue().getName()).isEqualTo("Phòng 101");
    }

    @Test
    void should_update_existing_room_when_code_exists() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var createdAt = OffsetDateTime.now().minusDays(5);
        var existing = new SchoolRoom(
            UUID.randomUUID(), schoolId, "P101", "Phòng cũ", "Mô tả cũ", true,
            createdAt, createdAt, createdBy, createdBy
        );
        var rows = List.of(row(sessionId, 1L, dataWithDescription("P101", "Phòng mới", "Mô tả mới")));

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
        assertThat(existing.getUpdatedBy()).isEqualTo(createdBy);
        verify(schoolRoomRepository).save(existing);
    }

    @Test
    void should_keep_duplicate_code_in_same_file_invalid() {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var rows = List.of(
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
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var rows = List.of(row(sessionId, 1L, data("P101", "Phòng 101")));

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
            id, schoolId, ImportType.SCHOOL_ROOM, "rooms.csv", "[]", "{}",
            jsonSerializationPort.toJson(mapping),
            0L, 0L, 0L, 0L, 0L, null, ImportSessionStatus.IMPORTING, null,
            OffsetDateTime.now().plusDays(1), null, null, null, 0,
            OffsetDateTime.now(), OffsetDateTime.now(),
            createdBy, createdBy
        );
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
