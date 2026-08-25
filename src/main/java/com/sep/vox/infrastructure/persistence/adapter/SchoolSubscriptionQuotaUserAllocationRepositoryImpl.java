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
    public List<SchoolSubscriptionQuotaUserAllocation> findAllBySubscriptionIdAndQuotaType(UUID subscriptionId, QuotaType quotaType) {
        return springDataRepository.findAllBySubscriptionIdAndQuotaType(subscriptionId, quotaType.name()).stream()
            .map(SchoolSubscriptionQuotaUserAllocationMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<SchoolSubscriptionQuotaUserAllocation> findBySubscriptionIdAndQuotaTypeAndUserId(UUID subscriptionId, QuotaType quotaType, UUID userId) {
        return springDataRepository.findBySubscriptionIdAndQuotaTypeAndUserId(subscriptionId, quotaType.name(), userId)
            .map(SchoolSubscriptionQuotaUserAllocationMapper::toDomain);
    }

    @Override
    public SchoolSubscriptionQuotaUserAllocation upsertAllocation(UUID subscriptionId, QuotaType quotaType, UUID userId, BigDecimal allocatedQuantity) {
        var existing = springDataRepository.findBySubscriptionIdAndQuotaTypeAndUserId(subscriptionId, quotaType.name(), userId);
        SchoolSubscriptionQuotaUserAllocationJpaEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setAllocatedQuantity(allocatedQuantity);
        } else {
            entity = new SchoolSubscriptionQuotaUserAllocationJpaEntity(null, subscriptionId, quotaType.name(), userId, allocatedQuantity, BigDecimal.ZERO);
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
