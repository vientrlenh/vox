package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;
import com.sep.vox.infrastructure.persistence.entity.SchoolSubscriptionQuotaUserAllocationJpaEntity;
import com.sep.vox.infrastructure.persistence.mapper.SchoolSubscriptionQuotaUserAllocationMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolSubscriptionQuotaUserAllocationRepository;

@Repository
public class SchoolSubscriptionQuotaUserAllocationRepositoryImpl implements SchoolSubscriptionQuotaUserAllocationRepository {

    private final SpringDataSchoolSubscriptionQuotaUserAllocationRepository springDataRepository;

    public SchoolSubscriptionQuotaUserAllocationRepositoryImpl(SpringDataSchoolSubscriptionQuotaUserAllocationRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public List<SchoolSubscriptionQuotaUserAllocation> findBySchoolSubscriptionIdAndQuotaType(UUID schoolSubscriptionId, QuotaType quotaType) {
        return springDataRepository.findBySchoolSubscriptionIdAndQuotaType(schoolSubscriptionId, quotaType.name()).stream()
            .map(SchoolSubscriptionQuotaUserAllocationMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<SchoolSubscriptionQuotaUserAllocation> findBySchoolSubscriptionIdAndQuotaTypeAndUserId(UUID schoolSubscriptionId, QuotaType quotaType, UUID userId) {
        return springDataRepository.findBySchoolSubscriptionIdAndQuotaTypeAndUserId(schoolSubscriptionId, quotaType.name(), userId)
            .map(SchoolSubscriptionQuotaUserAllocationMapper::toDomain);
    }

    @Override
    public SchoolSubscriptionQuotaUserAllocation upsertAllocation(UUID schoolSubscriptionId, QuotaType quotaType, UUID userId, BigDecimal allocatedAmountVnd) {
        var existing = springDataRepository.findBySchoolSubscriptionIdAndQuotaTypeAndUserId(schoolSubscriptionId, quotaType.name(), userId);
        SchoolSubscriptionQuotaUserAllocationJpaEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setAllocatedAmountVnd(allocatedAmountVnd);
        } else {
            entity = new SchoolSubscriptionQuotaUserAllocationJpaEntity(
                null, schoolSubscriptionId, quotaType.name(), userId, allocatedAmountVnd, BigDecimal.ZERO);
        }
        var saved = springDataRepository.save(entity);
        return SchoolSubscriptionQuotaUserAllocationMapper.toDomain(saved);
    }

    @Override
    public boolean tryConsume(UUID id, BigDecimal amount) {
        return springDataRepository.tryConsume(id, amount) > 0;
    }

    @Override
    public void addUsage(UUID id, BigDecimal amount) {
        springDataRepository.addUsage(id, amount);
    }
}
