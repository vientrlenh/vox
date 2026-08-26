package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.infrastructure.persistence.mapper.SubscriptionPlanQuotaMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSubscriptionPlanQuotaRepository;

@Repository
public class SubscriptionPlanQuotaRepositoryImpl implements SubscriptionPlanQuotaRepository {

    private final SpringDataSubscriptionPlanQuotaRepository springDataSubscriptionPlanQuotaRepository;

    public SubscriptionPlanQuotaRepositoryImpl(SpringDataSubscriptionPlanQuotaRepository springDataSubscriptionPlanQuotaRepository) {
        this.springDataSubscriptionPlanQuotaRepository = springDataSubscriptionPlanQuotaRepository;
    }

    @Override
    public Optional<SubscriptionPlanQuota> findById(UUID id) {
        return springDataSubscriptionPlanQuotaRepository.findById(id).map(SubscriptionPlanQuotaMapper::toDomain);
    }

    @Override
    public SubscriptionPlanQuota save(SubscriptionPlanQuota quota) {
        var entity = SubscriptionPlanQuotaMapper.toJpa(quota);
        var saved = springDataSubscriptionPlanQuotaRepository.save(entity);
        return SubscriptionPlanQuotaMapper.toDomain(saved);
    }

    @Override
    public List<SubscriptionPlanQuota> findBySubscriptionPlanId(UUID subscriptionPlanId) {
        return springDataSubscriptionPlanQuotaRepository.findBySubscriptionPlanId(subscriptionPlanId).stream()
            .map(SubscriptionPlanQuotaMapper::toDomain)
            .toList();
    }

    @Override
    public List<SubscriptionPlanQuota> findBySubscriptionPlanIdIn(Collection<UUID> planIds) {
        return springDataSubscriptionPlanQuotaRepository.findBySubscriptionPlanIdIn(planIds).stream()
            .map(SubscriptionPlanQuotaMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteBySubscriptionPlanId(UUID subscriptionPlanId) {
        springDataSubscriptionPlanQuotaRepository.deleteBySubscriptionPlanId(subscriptionPlanId);
    }

    @Override
    public List<SubscriptionPlanQuota> saveAll(Collection<SubscriptionPlanQuota> quotas) {
        var entities = quotas.stream().map(SubscriptionPlanQuotaMapper::toJpa).toList();
        var saved = springDataSubscriptionPlanQuotaRepository.saveAll(entities);
        return saved.stream().map(SubscriptionPlanQuotaMapper::toDomain).toList();
    }
}
