package com.sep.vox.domain.repository.personalization;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.SubAttributePriority;

public interface SubAttributePriorityRepository {

    /** Xoá priority cũ của các học sinh này rồi ghi lại toàn bộ -- luôn tính lại từ đầu, không patch từng phần. */
    void replaceForStudents(List<UUID> studentIds, List<SubAttributePriority> priorities);

    record PracticeablePriority(String criterionCode, String subAttribute) {
    }

    List<PracticeablePriority> findPracticeablePrioritiesOrderedDesc(UUID studentId);
}
