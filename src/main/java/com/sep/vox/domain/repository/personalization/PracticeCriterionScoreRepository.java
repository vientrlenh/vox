package com.sep.vox.domain.repository.personalization;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.dto.personalization.PracticeCriterionScoreDto;

public interface PracticeCriterionScoreRepository {

    /**
     * Ghi hoặc cập nhật điểm 1 tiêu chí cho 1 bản chấm, định danh bằng MÃ tiêu chí.
     *
     * <p>Từ V13 không còn tra {@code rubric_criterions} để lấy id: luyện tập chấm trên thang
     * 0-100 cố định, không thuộc rubric nào. Bản cũ tra rubric rồi {@code continue} khi không
     * khớp -- nghĩa là điểm biến mất không dấu vết, đúng cách bug 2026-08-06 ở đường thi ẩn được.
     *
     * <p>Không nhận {@code matchedBandCode}: luyện tập chấm đối chiếu ĐÚNG MỘT bậc học sinh đã
     * chọn, không xếp loại nên không có bậc nào để gán.
     */
    void upsertByCode(
        UUID practiceEvaluationId,
        String criterionCode,
        double finalScore
    );

    List<PracticeCriterionScoreDto> findScoresBySessionId(UUID sessionId);
}
