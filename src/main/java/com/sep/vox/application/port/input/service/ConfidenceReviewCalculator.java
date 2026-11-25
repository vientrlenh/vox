package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.valueobject.ConfidenceCaseSignals;

/**
 * Kết hợp confidence theo severity, không dùng weighted average.
 */
@Service
public class ConfidenceReviewCalculator {

    private static final BigDecimal C_REF_SOFT = decimal(0.90);
    private static final BigDecimal C_REF_SOFT_CODESWITCH = decimal(0.85);
    private static final BigDecimal C_REF_HARD = decimal(0.80);
    // DISABLED 2026-08-11 together with Group D -- see the javadoc of evaluateAsrAndReferenceSignals().
    // Case (4) -- 3 ngưỡng RIÊNG cho accuracy(1-m)/coverage(c)/timing(1-j), theo ĐÚNG spec
    // (không phải 1 ngưỡng chung áp lên composite min() như bản trước). Áp ngưỡng chung 0.90 lên
    // composite sẽ vô tình ép accuracy phải đạt 0.90 mới qua, xoá mất biên nới Vietnam-adjusted
    // 0.80/0.70 dành riêng cho accuracy (rụng phụ âm cuối) -- xem AlignmentConfidence ở Python.
    // private static final BigDecimal C_ALIGN_ACCURACY_SOFT = decimal(0.80); // 1 - m, m soft > 0.20 (nới từ 0.10)
    // private static final BigDecimal C_ALIGN_ACCURACY_HARD = decimal(0.70); // m hard > 0.30 (nới từ 0.20)
    // private static final BigDecimal C_ALIGN_COVERAGE_SOFT = decimal(0.90); // c soft < 0.90 (giữ nguyên)
    // private static final BigDecimal C_ALIGN_COVERAGE_HARD = decimal(0.80); // c hard < 0.80 (giữ nguyên)
    // private static final BigDecimal C_ALIGN_TIMING_SOFT = decimal(0.85); // 1 - j, j soft > 0.15 (giữ nguyên)
    // private static final BigDecimal C_ALIGN_TIMING_HARD = decimal(0.70); // j hard > 0.30 (giữ nguyên)
    // Coherence có trần đồng thuận NGƯỜI-NGƯỜI thấp hơn hẳn Grammar/Vocabulary
    // (kappa .653-.68 trong literature review correctness/03-bo-sung-con-thieu.md, vì bản
    // thân construct không có 1 cách tổ chức ý "đúng duy nhất") -- dao động Δc giữa 3 lần
    // chấm độc lập vì vậy ít đáng báo động hơn cho coherence so với grammar, nới thêm ngưỡng
    // soft riêng cho coherence thay vì dùng chung LLM_SOFT_LONG/SHORT với 2 tiêu chí kia.
    private static final BigDecimal LLM_COHERENCE_SOFT_RELAXATION = decimal(0.10);
    private static final BigDecimal LLM_COHERENCE_DELTA_SOFT_RELAXATION = decimal(0.20);
    public enum ConfidenceMode {
        PRACTICE,
        MOCK_TEST,
        HIGH_STAKES;

        public static ConfidenceMode fromAssessmentKind(AssessmentKind assessmentKind) {
            return assessmentKind == AssessmentKind.CENTRALIZED ? HIGH_STAKES : MOCK_TEST;
        }
    }

    private record ThresholdProfile(
        BigDecimal asrLogSoft,
        BigDecimal asrLogHard,
        BigDecimal audioSoft,
        BigDecimal softDelta,
        BigDecimal hardDelta,
        BigDecimal clippingHard,
        BigDecimal llmSoftLong,
        BigDecimal llmSoftShort,
        BigDecimal llmDeltaSoftLong,
        BigDecimal llmDeltaHardLong,
        BigDecimal llmDeltaSoftShort,
        BigDecimal llmDeltaHardShort,
        int moderateGroupsRequired,
        boolean reviewSoftSignals
    ) {
    }

    public record Decision(
        boolean requiresHumanReview,
        String reviewSeverity,
        List<String> reviewReasons,
        String confidenceMode,
        String audioGateStatus,
        List<String> audioGateReasons
    ) {
    }

    public Decision compute(
            ConfidenceCaseSignals signals,
            AssessmentKind assessmentKind,
            BigDecimal codeSwitchingRatio,
            boolean isShortAnswer) {
        return compute(signals, null, ConfidenceMode.fromAssessmentKind(assessmentKind), codeSwitchingRatio, isShortAnswer);
    }

    public Decision compute(
            ConfidenceCaseSignals signals,
            BigDecimal audioQuality,
            AssessmentKind assessmentKind,
            BigDecimal codeSwitchingRatio,
            boolean isShortAnswer) {
        return compute(
            signals,
            audioQuality,
            ConfidenceMode.fromAssessmentKind(assessmentKind),
            codeSwitchingRatio,
            isShortAnswer
        );
    }

    public Decision compute(
            ConfidenceCaseSignals signals,
            BigDecimal audioQuality,
            ConfidenceMode mode,
            BigDecimal codeSwitchingRatio,
            boolean isShortAnswer) {
        ThresholdProfile profile = profile(mode);
        if (signals == null) {
            return decisionWithoutSignals(mode, audioQuality);
        }

        Findings findings = new Findings();
        evaluateAudioSignals(signals, audioQuality, profile, findings);
        evaluateAsrAndReferenceSignals(signals, codeSwitchingRatio, profile, findings);
        int llmSoftCount = evaluateLlmSignals(signals, profile, isShortAnswer, findings);

        String severity = severityOf(findings, profile, llmSoftCount);
        return buildDecision(signals, audioQuality, mode, profile, findings, severity);
    }

    private static Decision decisionWithoutSignals(ConfidenceMode mode, BigDecimal audioQuality) {
        return new Decision(
            false,
            "none",
            List.of(),
            mode.name(),
            audioQuality == null ? "UNKNOWN" : "PASS",
            List.of()
        );
    }

    private static void evaluateAudioSignals(
            ConfidenceCaseSignals signals,
            BigDecimal audioQuality,
            ThresholdProfile profile,
            Findings findings) {
        if (signals.clippingRatio() != null
                && signals.clippingRatio().compareTo(profile.clippingHard()) > 0) {
            findings.addHard("AUDIO_CLIPPING", "A");
        }
        if (signals.qSnr() != null && signals.qSnr().compareTo(BigDecimal.ZERO) <= 0) {
            findings.addHard("AUDIO_SNR_TOO_LOW", "A");
        }
        if (signals.qSpeech() != null && signals.qSpeech().compareTo(BigDecimal.ZERO) <= 0) {
            findings.addHard("AUDIO_TOO_MUCH_SILENCE", "A");
        }
        if (audioQuality != null
                && audioQuality.compareTo(BigDecimal.ZERO) > 0
                && audioQuality.compareTo(profile.audioSoft()) < 0) {
            findings.addSoft("AUDIO_QUALITY_LOW", "A");
        }
    }

    /**
     * Group D (forced alignment, case 4) is deliberately not evaluated here: it was switched off on 2026-08-11.
     * Python no longer emits c_align* (see utils/confidence_utils.py), and leaving the evaluation out makes
     * OLD records in the DB, which still carry c_align*, produce no ALIGNMENT_* reason when re-graded.
     *
     * <p>Measured on production, these three signals only produced SOFT reasons (ALIGNMENT_COVERAGE_LOW at 0.84,
     * ALIGNMENT_TIMING_ANOMALY at 0.80, both above the hard threshold), and since reviewSoftSignals = false a
     * soft reason no longer pushes an attempt to a teacher. They only lengthened the review_reason_code
     * string: the teacher reads "4 reasons" while the only mandatory culprit is LLM_UNSTABLE_VOCABULARY.
     */
    private static void evaluateAsrAndReferenceSignals(
            ConfidenceCaseSignals signals,
            BigDecimal codeSwitchingRatio,
            ThresholdProfile profile,
            Findings findings) {
        boolean hasCodeSwitch = codeSwitchingRatio != null
            && codeSwitchingRatio.compareTo(BigDecimal.ZERO) > 0;
        if (signals.cAsrLog() != null) {
            findings.evaluateMinimumBound(
                signals.cAsrLog(), profile.asrLogHard(), profile.asrLogSoft(), "ASR_LOW_CONF", "B");
        }
        if (signals.cRef() != null) {
            BigDecimal soft = (
                hasCodeSwitch ? C_REF_SOFT_CODESWITCH : C_REF_SOFT
            ).add(profile.softDelta());
            findings.evaluateMinimumBound(
                signals.cRef(), C_REF_HARD.add(profile.hardDelta()), soft, "REFERENCE_DRIFT", "C");
        }
    }

    private static int evaluateLlmSignals(
            ConfidenceCaseSignals signals,
            ThresholdProfile profile,
            boolean isShortAnswer,
            Findings findings) {
        BigDecimal llmSoft = isShortAnswer ? profile.llmSoftShort() : profile.llmSoftLong();
        BigDecimal llmDeltaSoft = isShortAnswer
            ? profile.llmDeltaSoftShort()
            : profile.llmDeltaSoftLong();
        BigDecimal llmDeltaHard = isShortAnswer
            ? profile.llmDeltaHardShort()
            : profile.llmDeltaHardLong();
        int llmSoftCount = 0;
        llmSoftCount += evaluateLlm(signals.cGrammar(), signals.grammarScoreDelta(),
            llmSoft, llmDeltaSoft, llmDeltaHard, "LLM_UNSTABLE_GRAMMAR", findings);
        llmSoftCount += evaluateLlm(signals.cVocabulary(), signals.vocabularyScoreDelta(),
            llmSoft, llmDeltaSoft, llmDeltaHard, "LLM_UNSTABLE_VOCABULARY", findings);
        llmSoftCount += evaluateLlm(signals.cCoherence(), signals.coherenceScoreDelta(),
            llmSoft.subtract(LLM_COHERENCE_SOFT_RELAXATION),
            llmDeltaSoft.add(LLM_COHERENCE_DELTA_SOFT_RELAXATION),
            llmDeltaHard, "LLM_UNSTABLE_COHERENCE", findings);
        return llmSoftCount;
    }

    private static String severityOf(Findings findings, ThresholdProfile profile, int llmSoftCount) {
        if (!findings.hardGroups.isEmpty()) {
            return "mandatory";
        }
        if (findings.softGroups.size() >= profile.moderateGroupsRequired()
                || llmSoftCount >= profile.moderateGroupsRequired()) {
            return "recommended";
        }
        if (!findings.softGroups.isEmpty()) {
            return "soft";
        }
        return "none";
    }

    private static Decision buildDecision(
            ConfidenceCaseSignals signals,
            BigDecimal audioQuality,
            ConfidenceMode mode,
            ThresholdProfile profile,
            Findings findings,
            String severity) {
        List<String> reasons = new ArrayList<>(findings.hardReasons);
        reasons.addAll(findings.softReasons);
        List<String> audioGateReasons = reasons.stream()
            .filter(reason -> reason.startsWith("AUDIO_"))
            .distinct()
            .toList();
        boolean requiresHumanReview = "mandatory".equals(severity)
            || (profile.reviewSoftSignals() && !"none".equals(severity));
        return new Decision(
            requiresHumanReview,
            severity,
            reasons.stream().distinct().toList(),
            mode.name(),
            audioGateStatus(signals, audioQuality, findings),
            audioGateReasons
        );
    }

    private static String audioGateStatus(
            ConfidenceCaseSignals signals,
            BigDecimal audioQuality,
            Findings findings) {
        boolean hasAudioSignal = audioQuality != null
            || signals.clippingRatio() != null
            || signals.qSnr() != null
            || signals.qSpeech() != null;
        if (findings.hardGroups.contains("A")) {
            return "HARD_FAIL";
        }
        if (findings.softGroups.contains("A")) {
            return "SOFT_WARN";
        }
        return hasAudioSignal ? "PASS" : "UNKNOWN";
    }

    private static ThresholdProfile profile(ConfidenceMode mode) {
        return switch (mode) {
            case PRACTICE -> new ThresholdProfile(
                decimal(0.65), decimal(0.55), decimal(0.40),
                decimal(-0.05), decimal(-0.03), decimal(0.01),
                decimal(0.35), decimal(0.15),
                decimal(1.50), decimal(2.50), decimal(1.50), decimal(3.00),
                3, false
            );
            case MOCK_TEST -> new ThresholdProfile(
                decimal(0.75), decimal(0.55), decimal(0.50),
                BigDecimal.ZERO, BigDecimal.ZERO, decimal(0.01),
                decimal(0.50), decimal(0.25),
                decimal(1.00), decimal(2.00), decimal(1.50), decimal(3.00),
                2, false
            );
            case HIGH_STAKES -> new ThresholdProfile(
                decimal(0.80), decimal(0.65), decimal(0.60),
                decimal(0.05), decimal(0.03), decimal(0.007),
                decimal(0.60), decimal(0.35),
                decimal(0.80), decimal(1.50), decimal(1.50), decimal(3.00),
                1, false
            );
        };
    }

    private static int evaluateLlm(
            BigDecimal confidence,
            BigDecimal scoreDelta,
            BigDecimal confidenceSoftThreshold,
            BigDecimal deltaSoftThreshold,
            BigDecimal deltaHardThreshold,
            String reason,
            Findings findings) {
        if (confidence == null && scoreDelta == null) {
            return 0;
        }
        if (scoreDelta != null) {
            return evaluateScoreDelta(confidence, scoreDelta, deltaSoftThreshold, deltaHardThreshold, reason, findings);
        }
        // Old payload without a score delta: fall back to confidence so historical data stays readable.
        return evaluateConfidenceFallback(confidence, confidenceSoftThreshold, reason, findings);
    }

    private static int evaluateScoreDelta(
            BigDecimal confidence,
            BigDecimal scoreDelta,
            BigDecimal deltaSoftThreshold,
            BigDecimal deltaHardThreshold,
            String reason,
            Findings findings) {
        // A single valid run is represented by Python as confidence=0, delta=0:
        // still hard, because two samples are needed to measure consistency.
        boolean insufficientRuns = confidence != null
            && confidence.compareTo(BigDecimal.ZERO) <= 0
            && scoreDelta.compareTo(BigDecimal.ZERO) == 0;
        if (insufficientRuns || scoreDelta.compareTo(deltaHardThreshold) >= 0) {
            findings.addHard(reason, "E");
            return 0;
        }
        if (scoreDelta.compareTo(deltaSoftThreshold) > 0) {
            findings.addSoft(reason, "E");
            return 1;
        }
        return 0;
    }

    private static int evaluateConfidenceFallback(
            BigDecimal confidence,
            BigDecimal confidenceSoftThreshold,
            String reason,
            Findings findings) {
        if (confidence == null) {
            return 0;
        }
        if (confidence.compareTo(BigDecimal.ZERO) <= 0) {
            findings.addHard(reason, "E");
            return 0;
        }
        if (confidence.compareTo(confidenceSoftThreshold) < 0) {
            findings.addSoft(reason, "E");
            return 1;
        }
        return 0;
    }

    private static final class Findings {
        private final List<String> hardReasons = new ArrayList<>();
        private final List<String> softReasons = new ArrayList<>();
        private final Set<String> hardGroups = new HashSet<>();
        private final Set<String> softGroups = new HashSet<>();

        private void addHard(String reason, String group) {
            hardReasons.add(reason);
            hardGroups.add(group);
        }

        private void addSoft(String reason, String group) {
            softReasons.add(reason);
            softGroups.add(group);
        }

        private void evaluateMinimumBound(
                BigDecimal value,
                BigDecimal hardThreshold,
                BigDecimal softThreshold,
                String reason,
                String group) {
            if (value.compareTo(hardThreshold) < 0) {
                addHard(reason, group);
            } else if (value.compareTo(softThreshold) < 0) {
                addSoft(reason, group);
            }
        }
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
