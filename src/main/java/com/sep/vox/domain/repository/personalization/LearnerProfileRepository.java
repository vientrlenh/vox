package com.sep.vox.domain.repository.personalization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.personalization.LearnerProfile;

public interface LearnerProfileRepository {

    Optional<LearnerProfile> findCurrent(UUID studentId);

    Optional<LearnerProfile> findCurrentForUpdate(UUID studentId);

    LearnerProfile save(LearnerProfile profile);

    // Bậc band ước lượng từ lịch sử thi gần nhất của học sinh (rỗng nếu chưa có dữ liệu)
    List<Integer> findEstimatedResultBandOrder(UUID studentId);

    // Bậc band mục tiêu theo chính sách chấm đang áp dụng cho học sinh
    List<Integer> findPolicyTargetBandOrder(UUID studentId);

    /** Số bậc của thang năng lực đang áp (VSTEP 6, CEFR 6, IELTS 9...) -- rỗng nếu chưa có
     * chính sách chấm nào. Đọc từ dữ liệu thay vì giả định cứng 6 bậc. */
    List<Integer> findFrameworkBandCount(UUID studentId);

    /** Cả thang bậc (thứ tự/mã/mô tả) của framework đang áp -- để mô tả thang cho LLM. */
    List<FrameworkResultBand> findFrameworkBandLadder(UUID studentId);
}
