package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.valueobject.ConfidenceCaseSignals;

/**
 * Kết hợp confidence theo severity, không dùng weighted average.
 */
@Service
public class ConfidenceReviewCalculator {

    private static final BigDecimal C_REF_SOFT = decimal(0.90);
    private static final BigDecimal C_REF_SOFT_CODESWITCH = decimal(0.85);
    private static final BigDecimal C_REF_HARD = decimal(0.80);
    // Case (4) -- 3 ngưỡng RIÊNG cho accuracy(1-m)/coverage(c)/timing(1-j), theo ĐÚNG spec
    // (không phải 1 ngưỡng chung áp lên composite min() như bản trước). Áp ngưỡng chung 0.90 lên
    // composite sẽ vô tình ép accuracy phải đạt 0.90 mới qua, xoá mất biên nới Vietnam-adjusted
    // 0.80/0.70 dành riêng cho accuracy (rụng phụ âm cuối) -- xem AlignmentConfidence ở Python.
    private static final BigDecimal C_ALIGN_ACCURACY_SOFT = decimal(0.80); // 1 - m, m soft > 0.20 (nới từ 0.10)
    private static final BigDecimal C_ALIGN_ACCURACY_HARD = decimal(0.70); // m hard > 0.30 (nới từ 0.20)
    private static final BigDecimal C_ALIGN_COVERAGE_SOFT = decimal(0.90); // c soft < 0.90 (giữ nguyên)
    private static final BigDecimal C_ALIGN_COVERAGE_HARD = decimal(0.80); // c hard < 0.80 (giữ nguyên)
    private static final BigDecimal C_ALIGN_TIMING_SOFT = decimal(0.85); // 1 - j, j soft > 0.15 (giữ nguyên)
    private static final BigDecimal C_ALIGN_TIMING_HARD = decimal(0.70); // j hard > 0.30 (giữ nguyên)
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

        public static ConfidenceMode fromExamKind(ExamKind examKind) {
            return examKind == ExamKind.CENTRALIZED ? HIGH_STAKES : MOCK_TEST;
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
            ExamKind examKind,
            BigDecimal codeSwitchingRatio,
            boolean isShortAnswer) {
        return compute(signals, null, ConfidenceMode.fromExamKind(examKind), codeSwitchingRatio, isShortAnswer);
    }

    public Decision compute(
            ConfidenceCaseSignals signals,
            BigDecimal audioQuality,
            ExamKind examKind,
            BigDecimal codeSwitchingRatio,
            boolean isShortAnswer) {
        return compute(
            signals,
            audioQuality,
            ConfidenceMode.fromExamKind(examKind),
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
        var profile = profile(mode);
        boolean hasCodeSwitch = codeSwitchingRatio != null
            && codeSwitchingRatio.compareTo(BigDecimal.ZERO) > 0;
        if (signals == null) {
            return new Decision(
                false,
                "none",
                List.of(),
                mode.name(),
                audioQuality == null ? "UNKNOWN" : "PASS",
                List.of()
            );
        }

        List<String> hardReasons = new ArrayList<>();
        List<String> softReasons = new ArrayList<>();
        Set<String> hardGroups = new HashSet<>();
        Set<String> softGroups = new HashSet<>();

        if (signals.clippingRatio() != null
                && signals.clippingRatio().compareTo(profile.clippingHard()) > 0) {
            addReason(hardReasons, hardGroups, "AUDIO_CLIPPING", "A");
        }
        if (signals.qSnr() != null && signals.qSnr().compareTo(BigDecimal.ZERO) <= 0) {
            addReason(hardReasons, hardGroups, "AUDIO_SNR_TOO_LOW", "A");
        }
        if (signals.qSpeech() != null && signals.qSpeech().compareTo(BigDecimal.ZERO) <= 0) {
            addReason(hardReasons, hardGroups, "AUDIO_TOO_MUCH_SILENCE", "A");
        }
        if (audioQuality != null
                && audioQuality.compareTo(BigDecimal.ZERO) > 0
                && audioQuality.compareTo(profile.audioSoft()) < 0) {
            addReason(softReasons, softGroups, "AUDIO_QUALITY_LOW", "A");
        }

        if (signals.cAsrLog() != null) {
            evaluateMinimumBound(
                signals.cAsrLog(),
                profile.asrLogHard(),
                profile.asrLogSoft(),
                "ASR_LOW_CONF",
                "B",
                hardReasons,
                softReasons,
                hardGroups,
                softGroups
            );
        }

        if (signals.cRef() != null) {
            BigDecimal soft = (
                hasCodeSwitch ? C_REF_SOFT_CODESWITCH : C_REF_SOFT
            ).add(profile.softDelta());
            evaluateMinimumBound(
                signals.cRef(),
                C_REF_HARD.add(profile.hardDelta()),
                soft,
                "REFERENCE_DRIFT",
                "C",
                hardReasons,
                softReasons,
                hardGroups,
                softGroups
            );
        }

        // Group D: forced alignment (case 4) -- 3 thành phần độc lập, MỖI thành phần so với
        // đúng ngưỡng riêng của nó (không gộp qua composite) để giữ đúng biên nới Vietnam-
        // adjusted chỉ áp cho accuracy (1-m), không lây sang coverage/timing.
        if (signals.cAlignAccuracy() != null) {
            boolean explainedByCodeSwitch = codeSwitchingRatio != null
                && codeSwitchingRatio.compareTo(BigDecimal.ONE.subtract(signals.cAlignAccuracy())) >= 0;
            if (!explainedByCodeSwitch) {
                evaluateMinimumBound(
                    signals.cAlignAccuracy(),
                    C_ALIGN_ACCURACY_HARD.add(profile.hardDelta()),
                    C_ALIGN_ACCURACY_SOFT.add(profile.softDelta()),
                    "ALIGNMENT_MISCUE_HIGH",
                    "D",
                    hardReasons,
                    softReasons,
                    hardGroups,
                    softGroups
                );
            }
        }
        if (signals.cAlignCoverage() != null) {
            boolean explainedByCodeSwitch = codeSwitchingRatio != null
                && codeSwitchingRatio.compareTo(BigDecimal.ONE.subtract(signals.cAlignCoverage())) >= 0;
            if (!explainedByCodeSwitch) {
                evaluateMinimumBound(
                    signals.cAlignCoverage(),
                    C_ALIGN_COVERAGE_HARD.add(profile.hardDelta()),
                    C_ALIGN_COVERAGE_SOFT.add(profile.softDelta()),
                    "ALIGNMENT_COVERAGE_LOW",
                    "D",
                    hardReasons,
                    softReasons,
                    hardGroups,
                    softGroups
                );
            }
        }
        if (signals.cAlignTiming() != null) {
            evaluateMinimumBound(
                signals.cAlignTiming(),
                C_ALIGN_TIMING_HARD.add(profile.hardDelta()),
                C_ALIGN_TIMING_SOFT.add(profile.softDelta()),
                "ALIGNMENT_TIMING_ANOMALY",
                "D",
                hardReasons,
                softReasons,
                hardGroups,
                softGroups
            );
        }

        BigDecimal llmSoft = isShortAnswer ? profile.llmSoftShort() : profile.llmSoftLong();
        BigDecimal llmDeltaSoft = isShortAnswer
            ? profile.llmDeltaSoftShort()
            : profile.llmDeltaSoftLong();
        BigDecimal llmDeltaHard = isShortAnswer
            ? profile.llmDeltaHardShort()
            : profile.llmDeltaHardLong();
        int llmSoftCount = 0;
        llmSoftCount += evaluateLlm(
            signals.cGrammar(),
            signals.grammarScoreDelta(),
            llmSoft,
            llmDeltaSoft,
            llmDeltaHard,
            "LLM_UNSTABLE_GRAMMAR",
            hardReasons,
            softReasons,
            hardGroups,
            softGroups
        );
        llmSoftCount += evaluateLlm(
            signals.cVocabulary(),
            signals.vocabularyScoreDelta(),
            llmSoft,
            llmDeltaSoft,
            llmDeltaHard,
            "LLM_UNSTABLE_VOCABULARY",
            hardReasons,
            softReasons,
            hardGroups,
            softGroups
        );
        BigDecimal llmSoftCoherence = llmSoft.subtract(LLM_COHERENCE_SOFT_RELAXATION);
        BigDecimal llmDeltaSoftCoherence = llmDeltaSoft.add(
            LLM_COHERENCE_DELTA_SOFT_RELAXATION
        );
        llmSoftCount += evaluateLlm(
            signals.cCoherence(),
            signals.coherenceScoreDelta(),
            llmSoftCoherence,
            llmDeltaSoftCoherence,
            llmDeltaHard,
            "LLM_UNSTABLE_COHERENCE",
            hardReasons,
            softReasons,
            hardGroups,
            softGroups
        );

        String severity;
        if (!hardGroups.isEmpty()) {
            severity = "mandatory";
        } else if (softGroups.size() >= profile.moderateGroupsRequired()
                || llmSoftCount >= profile.moderateGroupsRequired()) {
            severity = "recommended";
        } else if (!softGroups.isEmpty()) {
            severity = "soft";
        } else {
            severity = "none";
        }

        List<String> reasons = new ArrayList<>(hardReasons);
        reasons.addAll(softReasons);
        List<String> audioGateReasons = reasons.stream()
            .filter(reason -> reason.startsWith("AUDIO_"))
            .distinct()
            .toList();
        boolean hasAudioSignal = audioQuality != null
            || signals.clippingRatio() != null
            || signals.qSnr() != null
            || signals.qSpeech() != null;
        String audioGateStatus;
        if (hardGroups.contains("A")) {
            audioGateStatus = "HARD_FAIL";
        } else if (softGroups.contains("A")) {
            audioGateStatus = "SOFT_WARN";
        } else {
            audioGateStatus = hasAudioSignal ? "PASS" : "UNKNOWN";
        }
        boolean requiresHumanReview = "mandatory".equals(severity)
            || (profile.reviewSoftSignals() && !"none".equals(severity));
        return new Decision(
            requiresHumanReview,
            severity,
            reasons.stream().distinct().toList(),
            mode.name(),
            audioGateStatus,
            audioGateReasons
        );
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
                2, true
            );
            case HIGH_STAKES -> new ThresholdProfile(
                decimal(0.80), decimal(0.65), decimal(0.60),
                decimal(0.05), decimal(0.03), decimal(0.007),
                decimal(0.60), decimal(0.35),
                decimal(0.80), decimal(1.50), decimal(1.50), decimal(3.00),
                1, true
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
            List<String> hardReasons,
            List<String> softReasons,
            Set<String> hardGroups,
            Set<String> softGroups) {
        if (confidence == null && scoreDelta == null) {
            return 0;
        }
        if (scoreDelta != null) {
            // Một lượt hợp lệ duy nhất được Python biểu diễn confidence=0, delta=0:
            // vẫn là hard vì không đủ hai mẫu để đo consistency.
            boolean insufficientRuns = confidence != null
                && confidence.compareTo(BigDecimal.ZERO) <= 0
                && scoreDelta.compareTo(BigDecimal.ZERO) == 0;
            if (insufficientRuns || scoreDelta.compareTo(deltaHardThreshold) >= 0) {
                addReason(hardReasons, hardGroups, reason, "E");
                return 0;
            }
            if (scoreDelta.compareTo(deltaSoftThreshold) > 0) {
                addReason(softReasons, softGroups, reason, "E");
                return 1;
            }
            return 0;
        }
        // Payload cũ chưa có score delta: fallback sang confidence để đọc được dữ liệu lịch sử.
        if (confidence.compareTo(BigDecimal.ZERO) <= 0) {
            addReason(hardReasons, hardGroups, reason, "E");
            return 0;
        }
        if (confidence.compareTo(confidenceSoftThreshold) < 0) {
            addReason(softReasons, softGroups, reason, "E");
            return 1;
        }
        return 0;
    }

    private static void evaluateMinimumBound(
            BigDecimal value,
            BigDecimal hardThreshold,
            BigDecimal softThreshold,
            String reason,
            String group,
            List<String> hardReasons,
            List<String> softReasons,
            Set<String> hardGroups,
            Set<String> softGroups) {
        if (value.compareTo(hardThreshold) < 0) {
            addReason(hardReasons, hardGroups, reason, group);
        } else if (value.compareTo(softThreshold) < 0) {
            addReason(softReasons, softGroups, reason, group);
        }
    }

    private static void addReason(
            List<String> reasons,
            Set<String> groups,
            String reason,
            String group) {
        reasons.add(reason);
        groups.add(group);
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
