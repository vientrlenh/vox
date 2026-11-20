package com.sep.vox.application.port.input.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.valueobject.ConfidenceCaseSignals;

class ConfidenceReviewCalculatorTests {

    private final ConfidenceReviewCalculator calculator = new ConfidenceReviewCalculator();

    @Test
    void returnsNoReviewWhenSignalsAreMissing() {
        ConfidenceReviewCalculator.Decision decision = calculator.compute(
            null,
            AssessmentKind.CLASS_TEST,
            null,
            false
        );

        assertThat(decision.requiresHumanReview()).isFalse();
        assertThat(decision.reviewSeverity()).isEqualTo("none");
        assertThat(decision.reviewReasons()).isEmpty();
    }

    @Test
    void asrHardFailureRequiresMandatoryReview() {
        ConfidenceReviewCalculator.Decision decision = calculator.compute(
            signals(decimal("0.54"), null, null, null, null, null, null, null, null, null),
            AssessmentKind.CLASS_TEST,
            null,
            false
        );

        assertThat(decision.reviewSeverity()).isEqualTo("mandatory");
        assertThat(decision.reviewReasons()).containsExactly("ASR_LOW_CONF");
        // "mandatory" is the ONLY level that still pushes a submission to a human grader on its own
        // -- pinned here so that whoever changes the thresholds next sees that boundary
        // immediately.
        assertThat(decision.requiresHumanReview()).isTrue();
    }

    @Test
    void oneBranchSoftFailureIsReportedAsSoftWithoutForcingReview() {
        // A soft branch is still recorded with severity "soft" so it can be shown to the grader,
        // but it does NOT push the submission into the manual grading queue: reviewSoftSignals =
        // false in all three profiles (PRACTICE/MOCK_TEST/HIGH_STAKES), so requiresHumanReview is
        // only switched on by "mandatory".
        ConfidenceReviewCalculator.Decision decision = calculator.compute(
            signals(decimal("0.70"), null, null, null, null, null, null, null, null, null),
            AssessmentKind.CLASS_TEST,
            null,
            false
        );

        assertThat(decision.reviewSeverity()).isEqualTo("soft");
        assertThat(decision.requiresHumanReview()).isFalse();
    }

    @Test
    void twoUnstableLlmCriteriaRequireRecommendedReview() {
        ConfidenceReviewCalculator.Decision decision = calculator.compute(
            signals(null, null, null, null, null, null, null, decimal("0.40"), decimal("0.40"), decimal("0.90")),
            AssessmentKind.CLASS_TEST,
            null,
            false
        );

        assertThat(decision.reviewSeverity()).isEqualTo("recommended");
        assertThat(decision.reviewReasons())
            .containsExactly("LLM_UNSTABLE_GRAMMAR", "LLM_UNSTABLE_VOCABULARY");
    }

    @Test
    void centralizedModeTightensMinimumBound() {
        ConfidenceReviewCalculator.Decision classTest = calculator.compute(
            signals(decimal("0.78"), null, null, null, null, null, null, null, null, null),
            AssessmentKind.CLASS_TEST,
            null,
            false
        );
        ConfidenceReviewCalculator.Decision centralized = calculator.compute(
            signals(decimal("0.78"), null, null, null, null, null, null, null, null, null),
            AssessmentKind.CENTRALIZED,
            null,
            false
        );

        assertThat(classTest.reviewSeverity()).isEqualTo("none");
        assertThat(centralized.reviewSeverity()).isEqualTo("recommended");
        // A centralized assessment tightens the severity THRESHOLD, but "recommended" still does not push
        // to a grader on its own -- only "mandatory" does.
        assertThat(centralized.requiresHumanReview()).isFalse();
    }

    @Test
    void moderateAudioDoesNotTriggerHardGate() {
        ConfidenceReviewCalculator.Decision decision = calculator.compute(
            signals(null, decimal("0.50"), decimal("0.90"), null, null, null, null, null, null, null),
            AssessmentKind.CLASS_TEST,
            null,
            false
        );

        assertThat(decision.reviewSeverity()).isEqualTo("none");
        assertThat(decision.reviewReasons()).isEmpty();
    }

    private static ConfidenceCaseSignals signals(
            BigDecimal cAsrLog,
            BigDecimal qSnr,
            BigDecimal qSpeech,
            BigDecimal clippingRatio,
            BigDecimal cRef,
            BigDecimal cAlign,
            BigDecimal cPfBranch,
            BigDecimal cGrammar,
            BigDecimal cVocabulary,
            BigDecimal cDiscourse) {
        return alignmentSignals(cAsrLog, qSnr, qSpeech, clippingRatio, cRef,
            cAlign, null, null, null, cPfBranch, cGrammar, cVocabulary, cDiscourse);
    }

    private static ConfidenceCaseSignals alignmentSignals(
            BigDecimal cAsrLog,
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
            cDiscourse,
            null,
            null,
            null
        );
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
