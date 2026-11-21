package com.sep.vox.application.usecase.rubricschool;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.application.port.input.service.RubricResultBandImportCommitHandler;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

class RubricResultBandImportCommitHandlerTests {

    private final UUID versionId = UUID.randomUUID();

    private RubricResultBandRepository rubricResultBandRepository;
    private FakeJsonSerializationPort jsonSerializationPort;
    private RubricResultBandImportCommitHandler handler;

    @BeforeEach
    void setUp() {
        rubricResultBandRepository = mock(RubricResultBandRepository.class);
        RubricVersionRepository rubricVersionRepository = mock(RubricVersionRepository.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        handler = new RubricResultBandImportCommitHandler(
            rubricResultBandRepository, rubricVersionRepository, jsonSerializationPort);
        when(rubricVersionRepository.findById(versionId)).thenReturn(Optional.of(RubricVersion.builder()
            .id(versionId).status(RubricStatus.DRAFT)
            .scoringScaleMin(BigDecimal.ZERO).scoringScaleMax(BigDecimal.TEN).build()));
        when(rubricResultBandRepository.findByRubricVersionId(versionId)).thenReturn(List.of());
    }

    @Test
    void should_throw_illegal_state_when_repository_reports_a_data_conflict() {
        doThrow(new DataConflictException("unique violation", null)).when(rubricResultBandRepository).saveAll(any());

        assertThatThrownBy(() -> handler.commit(session(), List.of(row())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Lỗi lưu dữ liệu: Mã xếp loại (Result Band) bị trùng lặp trong Phiên bản Rubric này.")
            .hasCauseInstanceOf(DataConflictException.class);
    }

    private ImportRow row() {
        Map<String, String> rawData = Map.of("code", "GOOD", "name", "Good", "scoreMin", "5", "scoreMax", "10", "order", "1");
        return ImportRow.builder()
            .id(UUID.randomUUID())
            .sessionId(UUID.randomUUID())
            .rowNumber(1L)
            .rawDataJson(jsonSerializationPort.toJson(rawData))
            .status(ImportRowStatus.PENDING)
            .build();
    }

    private ImportSession session() {
        Map<String, String> mapping = Map.of("code", "code", "name", "name", "scoreMin", "scoreMin",
            "scoreMax", "scoreMax", "order", "order");
        return ImportSession.builder()
            .id(UUID.randomUUID())
            .type(ImportType.RUBRIC_RESULT_BAND)
            .importedEntityId(versionId)
            .confirmedMappingJson(jsonSerializationPort.toJson(mapping))
            .build();
    }
}
