package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.infrastructure.persistence.mapper.SubscriptionPlanMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSubscriptionPlanRepository;

@Repository
public class SubscriptionPlanRepositoryImpl implements SubscriptionPlanRepository {

    private final SpringDataSubscriptionPlanRepository springDataSubscriptionPlanRepository;

    public SubscriptionPlanRepositoryImpl(SpringDataSubscriptionPlanRepository springDataSubscriptionPlanRepository) {
        this.springDataSubscriptionPlanRepository = springDataSubscriptionPlanRepository;
    }

    @Override
    public Optional<SubscriptionPlan> findById(UUID id) {
        return springDataSubscriptionPlanRepository.findById(id).map(SubscriptionPlanMapper::toDomain);
    }

    @Override
    public SubscriptionPlan save(SubscriptionPlan plan) {
        var entity = SubscriptionPlanMapper.toJpa(plan);
        var saved = springDataSubscriptionPlanRepository.save(entity);
        return SubscriptionPlanMapper.toDomain(saved);
    }

    @Override
    public List<SubscriptionPlan> findAllByStatus(PlanStatus status) {
        return springDataSubscriptionPlanRepository.findAllByStatus(status.name()).stream()
            .map(SubscriptionPlanMapper::toDomain)
            .toList();
    }

    @Override
    public List<SubscriptionPlan> findByIdIn(Collection<UUID> ids) {
        return springDataSubscriptionPlanRepository.findAllById(ids).stream()
            .map(SubscriptionPlanMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataSubscriptionPlanRepository.deleteById(id);
    }
}
