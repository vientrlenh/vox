package com.sep.vox.application.mapper.examevaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.examevaluation.ConfidenceCaseSignalsInput;
import com.sep.vox.application.port.input.command.examevaluation.EvaluationSignalsInput;

class ExamEvaluationSignalMapperTests {

    @Test
    void preservesNullableConfidenceBranchesAndClippingPrecision() {
        var confidence = new ConfidenceCaseSignalsInput(
            null,
            0.8,
            1.0,
            0.0075,
            null,
            0.87654,
            null,
            null,
            null,
            null,
            0.5,
            null,
            0.25,
            null,
            null,
            null
        );
        var dto = new EvaluationSignalsInput(
            30,
            20,
            2,
            1.0,
            20,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "SUFFICIENT",
            List.of(),
            confidence
        );

        var mapped = ExamEvaluationSignalMapper.toDomain(dto).confidenceCase();

        assertThat(mapped.cAsrLog()).isNull();
        assertThat(mapped.clippingRatio()).isEqualByComparingTo("0.0075");
        assertThat(mapped.cRef()).isNull();
        assertThat(mapped.cAlign()).isEqualByComparingTo("0.8765");
        assertThat(mapped.cVocabulary()).isNull();
    }
}
