package com.sep.vox.domain.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.SchoolDebtEvent;

public interface SchoolDebtEventRepository {
    SchoolDebtEvent save(SchoolDebtEvent event);
    List<SchoolDebtEvent> findAllBySchoolId(UUID schoolId);
}
