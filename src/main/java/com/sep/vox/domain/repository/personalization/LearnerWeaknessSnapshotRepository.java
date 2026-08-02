package com.sep.vox.domain.repository.personalization;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.LearnerWeaknessSnapshot;

public interface LearnerWeaknessSnapshotRepository {

    /** Xoá snapshot cũ của các học sinh này rồi ghi lại toàn bộ -- luôn tính lại từ đầu, không patch từng phần. */
    void replaceForStudents(List<UUID> studentIds, List<LearnerWeaknessSnapshot> snapshots);

    /** Mã tiêu chí của học sinh, sắp theo mức yếu giảm dần -- dùng để chọn tiêu chí trọng tâm khi ra đề luyện. */
    List<String> findFocusCriterionCodesOrderedByWeakness(UUID studentId);
}
