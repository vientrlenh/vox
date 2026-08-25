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
    public SubscriptionPlanQuota save(SubscriptionPlanQuota planQuota) {
        var entity = SubscriptionPlanQuotaMapper.toJpa(planQuota);
        var saved = springDataSubscriptionPlanQuotaRepository.save(entity);
        return SubscriptionPlanQuotaMapper.toDomain(saved);
    }

    @Override
    public List<SubscriptionPlanQuota> findAllByPlanId(UUID planId) {
        return springDataSubscriptionPlanQuotaRepository.findAllByPlanId(planId).stream()
            .map(SubscriptionPlanQuotaMapper::toDomain)
            .toList();
    }

    @Override
    public List<SubscriptionPlanQuota> findAllByPlanIdIn(Collection<UUID> planIds) {
        return springDataSubscriptionPlanQuotaRepository.findAllByPlanIdIn(planIds).stream()
            .map(SubscriptionPlanQuotaMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteAllByPlanId(UUID planId) {
        springDataSubscriptionPlanQuotaRepository.deleteAllByPlanId(planId);
    }
}
