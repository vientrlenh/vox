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
 *
 * Các ngưỡng gốc là baseline đã điều chỉnh cho học sinh THPT Việt Nam.
 * CLASS_TEST dùng baseline; CENTRALIZED siết thêm vì là kỳ thi chính thức.
 */
@Service
public class ConfidenceReviewCalculator {

    private static final BigDecimal ASR_LOG_SOFT = decimal(0.80);
    private static final BigDecimal ASR_LOG_HARD = decimal(0.60);
    private static final BigDecimal ASR_NOLOG_SOFT = decimal(0.90);
    private static final BigDecimal ASR_NOLOG_HARD = decimal(0.80);
    private static final BigDecimal ASR_NOLOG_SOFT_CODESWITCH = decimal(0.80);
    private static final BigDecimal ASR_NOLOG_HARD_CODESWITCH = decimal(0.70);
    private static final BigDecimal CLIPPING_HARD = decimal(0.01);
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
    private static final BigDecimal LLM_SOFT_LONG = decimal(0.50);
    private static final BigDecimal LLM_SOFT_SHORT = decimal(0.25);
    // Discourse/Coherence có trần đồng thuận NGƯỜI-NGƯỜI thấp hơn hẳn Grammar/Vocabulary
    // (kappa .653-.68 trong literature review correctness/03-bo-sung-con-thieu.md, vì bản
    // thân construct không có 1 cách tổ chức ý "đúng duy nhất") -- dao động Δc giữa 3 lần
    // chấm độc lập vì vậy ít đáng báo động hơn cho discourse so với grammar, nới thêm ngưỡng
    // soft riêng cho discourse thay vì dùng chung LLM_SOFT_LONG/SHORT với 2 tiêu chí kia.
    private static final BigDecimal LLM_DISCOURSE_SOFT_RELAXATION = decimal(0.10);
    private static final BigDecimal CENTRALIZED_SOFT_DELTA = decimal(0.05);
    private static final BigDecimal CENTRALIZED_HARD_DELTA = decimal(0.03);
    private static final BigDecimal CENTRALIZED_CLIPPING_HARD_DELTA = decimal(0.003);

    public record Decision(
        boolean requiresHumanReview,
        String reviewSeverity,
        List<String> reviewReasons
    ) {
    }

    public Decision compute(
            ConfidenceCaseSignals signals,
            ExamKind examKind,
            boolean hasCodeSwitch,
            boolean isShortAnswer) {
        if (signals == null) {
            return new Decision(false, "none", List.of());
        }

        boolean strict = examKind == ExamKind.CENTRALIZED;
        BigDecimal softDelta = strict ? CENTRALIZED_SOFT_DELTA : BigDecimal.ZERO;
        BigDecimal hardDelta = strict ? CENTRALIZED_HARD_DELTA : BigDecimal.ZERO;
        BigDecimal clippingHard = strict
            ? CLIPPING_HARD.subtract(CENTRALIZED_CLIPPING_HARD_DELTA)
            : CLIPPING_HARD;

        List<String> hardReasons = new ArrayList<>();
        List<String> softReasons = new ArrayList<>();
        Set<String> hardGroups = new HashSet<>();
        Set<String> softGroups = new HashSet<>();

        if (signals.clippingRatio() != null
                && signals.clippingRatio().compareTo(clippingHard) > 0) {
            addReason(hardReasons, hardGroups, "AUDIO_CLIPPING", "A");
        }

        if (signals.cAsrLog() != null) {
            evaluateMinimumBound(
                signals.cAsrLog(),
                ASR_LOG_HARD.add(hardDelta),
                ASR_LOG_SOFT.add(softDelta),
                "ASR_LOW_CONF",
                "B",
                hardReasons,
                softReasons,
                hardGroups,
                softGroups
            );
        }

        BigDecimal asrNolog = minimum(
            signals.crossAsrAgreement(),
            signals.qSnr(),
            signals.qSpeech()
        );
        if (asrNolog != null) {
            BigDecimal soft = (
                hasCodeSwitch ? ASR_NOLOG_SOFT_CODESWITCH : ASR_NOLOG_SOFT
            ).add(softDelta);
            BigDecimal hard = (
                hasCodeSwitch ? ASR_NOLOG_HARD_CODESWITCH : ASR_NOLOG_HARD
            ).add(hardDelta);
            evaluateMinimumBound(
                asrNolog,
                hard,
                soft,
                worstAsrNologReason(signals),
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
            ).add(softDelta);
            evaluateMinimumBound(
                signals.cRef(),
                C_REF_HARD.add(hardDelta),
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
            evaluateMinimumBound(
                signals.cAlignAccuracy(),
                C_ALIGN_ACCURACY_HARD.add(hardDelta),
                C_ALIGN_ACCURACY_SOFT.add(softDelta),
                "ALIGNMENT_MISCUE_HIGH",
                "D",
                hardReasons,
                softReasons,
                hardGroups,
                softGroups
            );
        }
        if (signals.cAlignCoverage() != null) {
            evaluateMinimumBound(
                signals.cAlignCoverage(),
                C_ALIGN_COVERAGE_HARD.add(hardDelta),
                C_ALIGN_COVERAGE_SOFT.add(softDelta),
                "ALIGNMENT_COVERAGE_LOW",
                "D",
                hardReasons,
                softReasons,
                hardGroups,
                softGroups
            );
        }
        if (signals.cAlignTiming() != null) {
            evaluateMinimumBound(
                signals.cAlignTiming(),
                C_ALIGN_TIMING_HARD.add(hardDelta),
                C_ALIGN_TIMING_SOFT.add(softDelta),
                "ALIGNMENT_TIMING_ANOMALY",
                "D",
                hardReasons,
                softReasons,
                hardGroups,
                softGroups
            );
        }

        BigDecimal llmSoft = (
            isShortAnswer ? LLM_SOFT_SHORT : LLM_SOFT_LONG
        ).add(softDelta);
        int llmSoftCount = 0;
        llmSoftCount += evaluateLlm(
            signals.cGrammar(),
            llmSoft,
            "LLM_UNSTABLE_GRAMMAR",
            hardReasons,
            softReasons,
            hardGroups,
            softGroups
        );
        llmSoftCount += evaluateLlm(
            signals.cVocabulary(),
            llmSoft,
            "LLM_UNSTABLE_VOCABULARY",
            hardReasons,
            softReasons,
            hardGroups,
            softGroups
        );
        BigDecimal llmSoftDiscourse = llmSoft.subtract(LLM_DISCOURSE_SOFT_RELAXATION);
        llmSoftCount += evaluateLlm(
            signals.cDiscourse(),
            llmSoftDiscourse,
            "LLM_UNSTABLE_DISCOURSE",
            hardReasons,
            softReasons,
            hardGroups,
            softGroups
        );

        String severity;
        if (!hardGroups.isEmpty()) {
            severity = "mandatory";
        } else if (softGroups.size() >= 2 || llmSoftCount >= 2) {
            severity = "recommended";
        } else if (!softGroups.isEmpty()) {
            severity = "soft";
        } else {
            severity = "none";
        }

        List<String> reasons = new ArrayList<>(hardReasons);
        reasons.addAll(softReasons);
        return new Decision(
            !"none".equals(severity),
            severity,
            reasons.stream().distinct().toList()
        );
    }

    private static int evaluateLlm(
            BigDecimal value,
            BigDecimal softThreshold,
            String reason,
            List<String> hardReasons,
            List<String> softReasons,
            Set<String> hardGroups,
            Set<String> softGroups) {
        if (value == null) {
            return 0;
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            addReason(hardReasons, hardGroups, reason, "E");
            return 0;
        }
        if (value.compareTo(softThreshold) < 0) {
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

    private static BigDecimal minimum(BigDecimal... values) {
        BigDecimal result = null;
        for (BigDecimal value : values) {
            if (value != null && (result == null || value.compareTo(result) < 0)) {
                result = value;
            }
        }
        return result;
    }

    private static String worstAsrNologReason(ConfidenceCaseSignals signals) {
        BigDecimal worst = minimum(
            signals.crossAsrAgreement(),
            signals.qSnr(),
            signals.qSpeech()
        );
        if (worst == null) {
            return "ASR_CONTENT_DISAGREEMENT";
        }
        if (signals.qSnr() != null && worst.compareTo(signals.qSnr()) == 0) {
            return "AUDIO_SNR_TOO_LOW";
        }
        if (signals.qSpeech() != null && worst.compareTo(signals.qSpeech()) == 0) {
            return "AUDIO_TOO_MUCH_SILENCE";
        }
        return "ASR_CONTENT_DISAGREEMENT";
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
