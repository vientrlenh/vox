package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.command.examevaluation.CriterionScoreInput;
import com.sep.vox.domain.model.personalization.WeaknessObservation;
import com.sep.vox.domain.model.personalization.WeaknessObservationSourceType;
import com.sep.vox.domain.model.rubric.RubricCriterion;

@Service
public class WeaknessObservationDerivationService {

    private static final int MAX_EVIDENCE_LENGTH = 200;

    /**
     * Chuyển nhãn điểm yếu do người chấm/LLM trả về thành quan sát lưu được.
     *
     * <p>KHÔNG còn suy nhãn từ SỐ ĐO. Trước đây lớp này còn sinh thêm ba loại nhãn ngoài
     * taxonomy: {@code phoneme_<âm>} (mỗi âm vị đọc dưới ngưỡng), {@code slow_rate} (tốc độ
     * nói thấp) và {@code long_pause} (tỉ lệ im lặng cao). Chúng chảy vào
     * sub_attribute_priority nhưng {@code practiceable} luôn false vì không thuộc taxonomy
     * đóng, nên chưa bao giờ được dùng để chọn đề -- không thể ra đề "hãy nói sai âm /z/ ít
     * lại". Nơi duy nhất chúng xuất hiện là trang hồ sơ điểm yếu, và trang đó đã bỏ.
     *
     * <p>Điểm yếu PHÁT ÂM và TRÔI CHẢY vẫn được đo bình thường -- ở mức TIÊU CHÍ, qua điểm số
     * trong WeaknessScoreObservation. Mất là mất "yếu ở âm /z/", thứ vốn không ra đề được.
     *
     * @param sourceType bài THI hay bài LUYỆN sinh ra quan sát này. Trước đây ghi cứng
     *     EXAM ngay trong {@link #observation}, nên không dùng lại được cho nhánh luyện
     *     -- mà nhánh luyện mới là nơi sinh ra phần lớn dữ liệu điểm yếu, vì học sinh
     *     luyện hằng ngày còn thi thì thỉnh thoảng.
     */
    public List<WeaknessObservation> derive(
            UUID studentId,
            UUID evaluationId,
            WeaknessObservationSourceType sourceType,
            Instant observedAt,
            boolean markedInvalid,
            boolean candidateBlocked,
            Map<String, CriterionScoreInput> criteria,
            Map<String, RubricCriterion> rubricCriteriaByCode) {
        if (markedInvalid || candidateBlocked || studentId == null || evaluationId == null) {
            return List.of();
        }

        var observations = new ArrayList<WeaknessObservation>();
        var safeCriteria = criteria == null ? Map.<String, CriterionScoreInput>of() : criteria;
        for (var entry : safeCriteria.entrySet()) {
            var score = entry.getValue();
            if (!hasScoredEvidence(score)) {
                continue;
            }
            var criterion = rubricCriteriaByCode.get(normalizeCode(entry.getKey()));
            if (criterion == null) {
                continue;
            }
            var labels = score.weaknessLabels() == null ? List.<String>of() : score.weaknessLabels();
            var spans = score.evidenceSpans() == null ? List.<String>of() : score.evidenceSpans();
            for (int index = 0; index < labels.size(); index++) {
                var label = labels.get(index);
                if (label == null || label.isBlank()) {
                    continue;
                }
                var evidence = index < spans.size() ? spans.get(index) : "";
                observations.add(observation(
                    studentId,
                    evaluationId,
                    sourceType,
                    criterion,
                    entry.getKey(),
                    label,
                    evidence,
                    observedAt
                ));
            }
        }

        return observations;
    }

    private WeaknessObservation observation(
            UUID studentId,
            UUID evaluationId,
            WeaknessObservationSourceType sourceType,
            RubricCriterion criterion,
            String criterionCode,
            String subAttribute,
            String evidence,
            Instant observedAt) {
        return new WeaknessObservation(
            studentId,
            sourceType,
            evaluationId,
            criterion.getFrameworkCriterionId(),
            truncate(criterionCode, 32),
            truncate(subAttribute, 64),
            truncate(evidence, MAX_EVIDENCE_LENGTH),
            observedAt
        );
    }

    private boolean hasScoredEvidence(CriterionScoreInput score) {
        if (score == null) {
            return false;
        }
        var status = score.status() == null ? "" : score.status().trim().toLowerCase(Locale.ROOT);
        return !"zeroed".equals(status) && !"not_scored".equals(status);
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String truncate(String value, int maxLength) {
        var safe = value == null ? "" : value;
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }
}
