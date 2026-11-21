package com.sep.vox.application.usecase.schooldirectory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.application.port.input.service.ImportCommitResult;
import com.sep.vox.application.port.input.service.SchoolDirectoryImportCommitHandler;
import com.sep.vox.application.port.output.RequiresNewTransactionPort;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;

class SchoolDirectoryImportCommitHandlerTests {

    private SchoolDirectoryRepository schoolDirectoryRepository;
    private RequiresNewTransactionPort requiresNewTransactionPort;
    private FakeJsonSerializationPort jsonSerializationPort;
    private SchoolDirectoryImportCommitHandler handler;

    @BeforeEach
    void setUp() {
        schoolDirectoryRepository = mock(SchoolDirectoryRepository.class);
        requiresNewTransactionPort = mock(RequiresNewTransactionPort.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        handler = new SchoolDirectoryImportCommitHandler(
            schoolDirectoryRepository, jsonSerializationPort, requiresNewTransactionPort);
    }

    @Test
    void should_mark_failed_when_the_transaction_reports_a_data_conflict() {
        doThrow(new DataConflictException("unique violation", null)).when(requiresNewTransactionPort).runInNewTransaction(any());
        ImportRow row = row(Map.of("Mã", "HS001", "Tên", "Trường A", "Mã tỉnh", "79", "Tỉnh", "TP HCM",
            "Quận", "Quận 1", "Địa chỉ", "1 Đường A"));

        ImportCommitResult result = handler.commit(session(), List.of(row));

        assertThat(result.created()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(row.getStatus()).isEqualTo(ImportRowStatus.FAILED);
        assertThat(row.getErrorsJson()).contains("Mã trường đã tồn tại");
    }

    private ImportRow row(Map<String, String> rawData) {
        return ImportRow.builder()
            .id(UUID.randomUUID())
            .sessionId(UUID.randomUUID())
            .rowNumber(1L)
            .rawDataJson(jsonSerializationPort.toJson(rawData))
            .status(ImportRowStatus.PENDING)
            .build();
    }

    private ImportSession session() {
        Map<String, String> mapping = Map.of("Mã", "code", "Tên", "name", "Mã tỉnh", "provinceCode",
            "Tỉnh", "provinceName", "Quận", "districtName", "Địa chỉ", "address");
        return ImportSession.builder()
            .id(UUID.randomUUID())
            .type(ImportType.SCHOOL_DIRECTORY)
            .confirmedMappingJson(jsonSerializationPort.toJson(mapping))
            .build();
    }
}
