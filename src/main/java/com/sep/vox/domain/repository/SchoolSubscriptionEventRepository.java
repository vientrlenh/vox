package com.sep.vox.domain.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.SchoolSubscriptionEvent;

public interface SchoolSubscriptionEventRepository {

    SchoolSubscriptionEvent save(SchoolSubscriptionEvent event);

    /** Lịch sử can thiệp của System Admin lên gói của một trường, mới nhất trước. */
    List<SchoolSubscriptionEvent> findBySchoolId(UUID schoolId);
}
