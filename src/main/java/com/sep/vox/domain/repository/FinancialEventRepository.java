package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.FinancialEvent;

public interface FinancialEventRepository {
    Optional<FinancialEvent> findById(UUID id);
    FinancialEvent save(FinancialEvent event);
    List<FinancialEvent> findAllBySchoolId(UUID schoolId);
}
