package com.sep.vox.application.usecase.rubricschool;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.application.port.input.service.RubricCriterionImportCommitHandler;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

class RubricCriterionImportCommitHandlerTests {

    private final UUID versionId = UUID.randomUUID();
    private final UUID rubricId = UUID.randomUUID();
    private final UUID frameworkId = UUID.randomUUID();
    private final UUID frameworkVersionId = UUID.randomUUID();

    private RubricCriterionRepository rubricCriterionRepository;
    private FakeJsonSerializationPort jsonSerializationPort;
    private RubricCriterionImportCommitHandler handler;

    @BeforeEach
    void setUp() {
        rubricCriterionRepository = mock(RubricCriterionRepository.class);
        RubricVersionRepository rubricVersionRepository = mock(RubricVersionRepository.class);
        RubricRepository rubricRepository = mock(RubricRepository.class);
        FrameworkCriterionRepository frameworkCriterionRepository = mock(FrameworkCriterionRepository.class);
        FrameworkVersionRepository frameworkVersionRepository = mock(FrameworkVersionRepository.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        handler = new RubricCriterionImportCommitHandler(rubricCriterionRepository, rubricVersionRepository,
            rubricRepository, frameworkCriterionRepository, jsonSerializationPort, frameworkVersionRepository);
        when(rubricVersionRepository.findById(versionId)).thenReturn(Optional.of(RubricVersion.builder()
            .id(versionId).rubricId(rubricId).status(RubricStatus.DRAFT)
            .scoringScaleMin(BigDecimal.ZERO).scoringScaleMax(BigDecimal.TEN).build()));
        when(rubricRepository.findById(rubricId)).thenReturn(Optional.of(Rubric.builder()
            .id(rubricId).frameworkId(frameworkId).build()));
        when(frameworkVersionRepository.findByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED))
            .thenReturn(List.of(FrameworkVersion.builder().id(frameworkVersionId).build()));
        when(frameworkCriterionRepository.findByFrameworkVersionId(frameworkVersionId))
            .thenReturn(List.of(FrameworkCriterion.builder().id(UUID.randomUUID()).code("FLUENCY").build()));
        when(rubricCriterionRepository.findByRubricVersionId(versionId)).thenReturn(List.of());
    }

    @Test
    void should_throw_illegal_argument_when_repository_reports_a_data_conflict() {
        doThrow(new DataConflictException("unique violation", null)).when(rubricCriterionRepository).saveAll(any());

        assertThatThrownBy(() -> handler.commit(session(), List.of(row())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("Data save error: the criterion code or standard Framework is duplicated")
            .hasCauseInstanceOf(DataConflictException.class);
    }

    private ImportRow row() {
        Map<String, String> rawData = Map.of("code", "FLU", "name", "Fluency", "frameworkCriterionCode", "FLUENCY",
            "weight", "20", "minScore", "0", "maxScore", "10", "order", "1");
        return ImportRow.builder()
            .id(UUID.randomUUID())
            .sessionId(UUID.randomUUID())
            .rowNumber(1L)
            .rawDataJson(jsonSerializationPort.toJson(rawData))
            .status(ImportRowStatus.PENDING)
            .build();
    }

    private ImportSession session() {
        Map<String, String> mapping = Map.of("code", "code", "name", "name", "frameworkCriterionCode", "frameworkCriterionCode",
            "weight", "weight", "minScore", "minScore", "maxScore", "maxScore", "order", "order");
        return ImportSession.builder()
            .id(UUID.randomUUID())
            .type(ImportType.RUBRIC_CRITERION)
            .importedEntityId(versionId)
            .confirmedMappingJson(jsonSerializationPort.toJson(mapping))
            .build();
    }
}
