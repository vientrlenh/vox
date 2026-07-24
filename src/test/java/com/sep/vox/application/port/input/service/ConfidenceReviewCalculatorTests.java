package com.sep.vox.application.port.input.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.valueobject.ConfidenceCaseSignals;

class ConfidenceReviewCalculatorTests {

    private final ConfidenceReviewCalculator calculator = new ConfidenceReviewCalculator();

    @Test
    void returnsNoReviewWhenSignalsAreMissing() {
        var decision = calculator.compute(
            null,
            ExamKind.CLASS_TEST,
            false,
            false
        );

        assertThat(decision.requiresHumanReview()).isFalse();
        assertThat(decision.reviewSeverity()).isEqualTo("none");
        assertThat(decision.reviewReasons()).isEmpty();
    }

    @Test
    void asrHardFailureRequiresMandatoryReview() {
        var decision = calculator.compute(
            signals(decimal("0.59"), null, null, null, null, null, null, null, null, null, null),
            ExamKind.CLASS_TEST,
            false,
            false
        );

        assertThat(decision.reviewSeverity()).isEqualTo("mandatory");
        assertThat(decision.reviewReasons()).containsExactly("ASR_LOW_CONF");
    }

    @Test
    void oneBranchSoftFailureRequiresSoftReview() {
        var decision = calculator.compute(
            signals(decimal("0.70"), null, null, null, null, null, null, null, null, null, null),
            ExamKind.CLASS_TEST,
            false,
            false
        );

        assertThat(decision.reviewSeverity()).isEqualTo("soft");
        assertThat(decision.requiresHumanReview()).isTrue();
    }

    @Test
    void twoUnstableLlmCriteriaRequireRecommendedReview() {
        var decision = calculator.compute(
            signals(null, null, null, null, null, null, null, null, decimal("0.40"), decimal("0.40"), decimal("0.90")),
            ExamKind.CLASS_TEST,
            false,
            false
        );

        assertThat(decision.reviewSeverity()).isEqualTo("recommended");
        assertThat(decision.reviewReasons())
            .containsExactly("LLM_UNSTABLE_GRAMMAR", "LLM_UNSTABLE_VOCABULARY");
    }

    @Test
    void centralizedModeTightensMinimumBound() {
        var classTest = calculator.compute(
            signals(decimal("0.82"), null, null, null, null, null, null, null, null, null, null),
            ExamKind.CLASS_TEST,
            false,
            false
        );
        var centralized = calculator.compute(
            signals(decimal("0.82"), null, null, null, null, null, null, null, null, null, null),
            ExamKind.CENTRALIZED,
            false,
            false
        );

        assertThat(classTest.reviewSeverity()).isEqualTo("none");
        assertThat(centralized.reviewSeverity()).isEqualTo("soft");
    }

    @Test
    void alignmentAccuracyUsesItsOwnVietnamAdjustedThreshold_notCompositeThreshold() {
        // accuracy=0.75 (m=0.25): dưới ngưỡng composite cũ 0.90, nhưng NẰM TRONG khoảng nới
        // Vietnam-adjusted đúng (soft 0.80/hard 0.70) -- đây là caseg soft-fail thật (0.75<0.80),
        // không phải hard-fail. coverage/timing đều tốt (không tự trigger group D).
        var decision = calculator.compute(
            alignmentSignals(null, null, null, null, null, null, null,
                decimal("0.75"), decimal("0.95"), decimal("0.90"), null, null, null, null),
            ExamKind.CLASS_TEST,
            false,
            false
        );

        assertThat(decision.reviewSeverity()).isEqualTo("soft");
        assertThat(decision.reviewReasons()).containsExactly("ALIGNMENT_MISCUE_HIGH");
    }

    @Test
    void alignmentAccuracyBelowVietnamAdjustedFloorDoesNotFalsePositive() {
        // accuracy=0.85 (m=0.15): dưới ngưỡng composite cũ 0.90 (sẽ SAI bị flag ở bản trước khi
        // sửa), nhưng ĐẠT ngưỡng soft Vietnam-adjusted thật (0.80) -- không được trigger gì cả.
        var decision = calculator.compute(
            alignmentSignals(null, null, null, null, null, null, null,
                decimal("0.85"), decimal("0.95"), decimal("0.90"), null, null, null, null),
            ExamKind.CLASS_TEST,
            false,
            false
        );

        assertThat(decision.reviewSeverity()).isEqualTo("none");
        assertThat(decision.requiresHumanReview()).isFalse();
    }

    @Test
    void nologCompositeReportsItsWorstComponent() {
        var decision = calculator.compute(
            signals(null, decimal("0.95"), decimal("0.50"), decimal("0.90"), null, null, null, null, null, null, null),
            ExamKind.CLASS_TEST,
            false,
            false
        );

        assertThat(decision.reviewSeverity()).isEqualTo("mandatory");
        assertThat(decision.reviewReasons()).containsExactly("AUDIO_SNR_TOO_LOW");
    }

    private static ConfidenceCaseSignals signals(
            BigDecimal cAsrLog,
            BigDecimal crossAsrAgreement,
            BigDecimal qSnr,
            BigDecimal qSpeech,
            BigDecimal clippingRatio,
            BigDecimal cRef,
            BigDecimal cAlign,
            BigDecimal cPfBranch,
            BigDecimal cGrammar,
            BigDecimal cVocabulary,
            BigDecimal cDiscourse) {
        return alignmentSignals(cAsrLog, crossAsrAgreement, qSnr, qSpeech, clippingRatio, cRef,
            cAlign, null, null, null, cPfBranch, cGrammar, cVocabulary, cDiscourse);
    }

    private static ConfidenceCaseSignals alignmentSignals(
            BigDecimal cAsrLog,
            BigDecimal crossAsrAgreement,
            BigDecimal qSnr,
            BigDecimal qSpeech,
            BigDecimal clippingRatio,
            BigDecimal cRef,
            BigDecimal cAlign,
            BigDecimal cAlignAccuracy,
            BigDecimal cAlignCoverage,
            BigDecimal cAlignTiming,
            BigDecimal cPfBranch,
            BigDecimal cGrammar,
            BigDecimal cVocabulary,
            BigDecimal cDiscourse) {
        return new ConfidenceCaseSignals(
            cAsrLog,
            crossAsrAgreement,
            qSnr,
            qSpeech,
            clippingRatio,
            cRef,
            cAlign,
            cAlignAccuracy,
            cAlignCoverage,
            cAlignTiming,
            cPfBranch,
            cGrammar,
            cVocabulary,
            cDiscourse
        );
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
