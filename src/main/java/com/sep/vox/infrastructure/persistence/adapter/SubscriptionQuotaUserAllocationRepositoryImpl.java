package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionQuotaUserAllocation;
import com.sep.vox.domain.repository.SubscriptionQuotaUserAllocationRepository;
import com.sep.vox.infrastructure.persistence.entity.SubscriptionQuotaUserAllocationJpaEntity;
import com.sep.vox.infrastructure.persistence.mapper.SubscriptionQuotaUserAllocationMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSubscriptionQuotaUserAllocationRepository;

@Repository
public class SubscriptionQuotaUserAllocationRepositoryImpl implements SubscriptionQuotaUserAllocationRepository {

    private final SpringDataSubscriptionQuotaUserAllocationRepository springDataRepository;

    public SubscriptionQuotaUserAllocationRepositoryImpl(SpringDataSubscriptionQuotaUserAllocationRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public List<SubscriptionQuotaUserAllocation> findAllBySubscriptionIdAndQuotaType(UUID subscriptionId, QuotaType quotaType) {
        return springDataRepository.findAllBySubscriptionIdAndQuotaType(subscriptionId, quotaType.name()).stream()
            .map(SubscriptionQuotaUserAllocationMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<SubscriptionQuotaUserAllocation> findBySubscriptionIdAndQuotaTypeAndUserId(UUID subscriptionId, QuotaType quotaType, UUID userId) {
        return springDataRepository.findBySubscriptionIdAndQuotaTypeAndUserId(subscriptionId, quotaType.name(), userId)
            .map(SubscriptionQuotaUserAllocationMapper::toDomain);
    }

    @Override
    public SubscriptionQuotaUserAllocation upsertAllocation(UUID subscriptionId, QuotaType quotaType, UUID userId, int allocatedQuantity) {
        var existing = springDataRepository.findBySubscriptionIdAndQuotaTypeAndUserId(subscriptionId, quotaType.name(), userId);
        SubscriptionQuotaUserAllocationJpaEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setAllocatedQuantity(allocatedQuantity);
        } else {
            entity = new SubscriptionQuotaUserAllocationJpaEntity(null, subscriptionId, quotaType.name(), userId, allocatedQuantity, 0);
        }
        var saved = springDataRepository.save(entity);
        return SubscriptionQuotaUserAllocationMapper.toDomain(saved);
    }

    @Override
    public boolean tryConsume(UUID id, int amount) {
        return springDataRepository.tryConsume(id, amount) > 0;
    }
}
