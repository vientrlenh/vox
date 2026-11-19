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
        ConfidenceReviewCalculator.Decision decision = calculator.compute(
            null,
            ExamKind.CLASS_TEST,
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
            ExamKind.CLASS_TEST,
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
            ExamKind.CLASS_TEST,
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
            ExamKind.CLASS_TEST,
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
            ExamKind.CLASS_TEST,
            null,
            false
        );
        ConfidenceReviewCalculator.Decision centralized = calculator.compute(
            signals(decimal("0.78"), null, null, null, null, null, null, null, null, null),
            ExamKind.CENTRALIZED,
            null,
            false
        );

        assertThat(classTest.reviewSeverity()).isEqualTo("none");
        assertThat(centralized.reviewSeverity()).isEqualTo("recommended");
        // A centralized exam tightens the severity THRESHOLD, but "recommended" still does not push
        // to a grader on its own -- only "mandatory" does.
        assertThat(centralized.requiresHumanReview()).isFalse();
    }

    // ==============================================================================================
    // TẮT 2026-08-11 cùng Group D (case 4) -- xem ConfidenceReviewCalculator.compute().
    //
    // Bốn test dưới đây khẳng định ĐÚNG hành vi vừa bị tắt, nên để nguyên là chúng sẽ đỏ. Không
    // xoá mà chú thích lại: chúng ghi lại phần khó nhất của case (4) -- vì sao ngưỡng accuracy
    // được nới riêng (0.80/0.70) thay vì dùng chung ngưỡng của coverage, và vì sao code-switching
    // phải MIỄN cho alignment đúng bằng tỉ lệ chuyển mã quan sát được. Bật lại Group D mà không có
    // bốn test này là bật lại kèm nguyên chùm lỗi cũ.
    //
    // @Test
    // void alignmentAccuracyUsesItsOwnVietnamAdjustedThreshold_notCompositeThreshold() {
    //     // accuracy=0.75 (m=0.25): dưới ngưỡng composite cũ 0.90, nhưng NẰM TRONG khoảng nới
    //     // Vietnam-adjusted đúng (soft 0.80/hard 0.70) -- đây là caseg soft-fail thật (0.75<0.80),
    //     // không phải hard-fail. coverage/timing đều tốt (không tự trigger group D).
    //     var decision = calculator.compute(
    //         alignmentSignals(null, null, null, null, null, null,
    //             decimal("0.75"), decimal("0.95"), decimal("0.90"), null, null, null, null),
    //         ExamKind.CLASS_TEST,
    //         null,
    //         false
    //     );
    //
    //     assertThat(decision.reviewSeverity()).isEqualTo("soft");
    //     assertThat(decision.reviewReasons()).containsExactly("ALIGNMENT_MISCUE_HIGH");
    // }
    //
    // @Test
    // void alignmentAccuracyBelowVietnamAdjustedFloorDoesNotFalsePositive() {
    //     // accuracy=0.85 (m=0.15): dưới ngưỡng composite cũ 0.90 (sẽ SAI bị flag ở bản trước khi
    //     // sửa), nhưng ĐẠT ngưỡng soft Vietnam-adjusted thật (0.80) -- không được trigger gì cả.
    //     var decision = calculator.compute(
    //         alignmentSignals(null, null, null, null, null, null,
    //             decimal("0.85"), decimal("0.95"), decimal("0.90"), null, null, null, null),
    //         ExamKind.CLASS_TEST,
    //         null,
    //         false
    //     );
    //
    //     assertThat(decision.reviewSeverity()).isEqualTo("none");
    //     assertThat(decision.requiresHumanReview()).isFalse();
    // }
    //
    // @Test
    // void codeSwitchSkipsAlignmentReasonsWhenItExplainsObservedGap() {
    //     var decision = calculator.compute(
    //         alignmentSignals(null, null, null, null, null, null,
    //             decimal("0.75"), decimal("0.80"), decimal("0.90"), null, null, null, null),
    //         ExamKind.CLASS_TEST,
    //         decimal("0.25"),
    //         false
    //     );
    //
    //     assertThat(decision.reviewReasons())
    //         .doesNotContain("ALIGNMENT_MISCUE_HIGH", "ALIGNMENT_COVERAGE_LOW");
    // }
    //
    // @Test
    // void codeSwitchKeepsAlignmentReasonsWhenItDoesNotExplainObservedGap() {
    //     var decision = calculator.compute(
    //         alignmentSignals(null, null, null, null, null, null,
    //             decimal("0.60"), decimal("0.70"), decimal("0.90"), null, null, null, null),
    //         ExamKind.CLASS_TEST,
    //         decimal("0.10"),
    //         false
    //     );
    //
    //     assertThat(decision.reviewReasons())
    //         .contains("ALIGNMENT_MISCUE_HIGH", "ALIGNMENT_COVERAGE_LOW");
    // }
    // ==============================================================================================

    @Test
    void moderateAudioDoesNotTriggerHardGate() {
        ConfidenceReviewCalculator.Decision decision = calculator.compute(
            signals(null, decimal("0.50"), decimal("0.90"), null, null, null, null, null, null, null),
            ExamKind.CLASS_TEST,
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
