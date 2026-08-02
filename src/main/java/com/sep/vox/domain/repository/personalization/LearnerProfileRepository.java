package com.sep.vox.domain.repository.personalization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.LearnerProfile;

public interface LearnerProfileRepository {

    Optional<LearnerProfile> findCurrent(UUID studentId);

    Optional<LearnerProfile> findCurrentForUpdate(UUID studentId);

    LearnerProfile save(LearnerProfile profile);

    // Bậc band ước lượng từ lịch sử thi gần nhất của học sinh (rỗng nếu chưa có dữ liệu)
    List<Integer> findEstimatedResultBandOrder(UUID studentId);

    // Bậc band mục tiêu theo chính sách chấm đang áp dụng cho học sinh
    List<Integer> findPolicyTargetBandOrder(UUID studentId);
}
