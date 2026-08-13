package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionQuota;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;
import com.sep.vox.infrastructure.persistence.mapper.SubscriptionQuotaMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSubscriptionQuotaRepository;

@Repository
public class SubscriptionQuotaRepositoryImpl implements SubscriptionQuotaRepository {

    private final SpringDataSubscriptionQuotaRepository springDataSubscriptionQuotaRepository;

    public SubscriptionQuotaRepositoryImpl(SpringDataSubscriptionQuotaRepository springDataSubscriptionQuotaRepository) {
        this.springDataSubscriptionQuotaRepository = springDataSubscriptionQuotaRepository;
    }

    @Override
    public Optional<SubscriptionQuota> findById(UUID id) {
        return springDataSubscriptionQuotaRepository.findById(id).map(SubscriptionQuotaMapper::toDomain);
    }

    @Override
    public SubscriptionQuota save(SubscriptionQuota quota) {
        var entity = SubscriptionQuotaMapper.toJpa(quota);
        var saved = springDataSubscriptionQuotaRepository.save(entity);
        return SubscriptionQuotaMapper.toDomain(saved);
    }

    @Override
    public List<SubscriptionQuota> findAllBySubscriptionId(UUID subscriptionId) {
        return springDataSubscriptionQuotaRepository.findAllBySubscriptionId(subscriptionId).stream()
            .map(SubscriptionQuotaMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<SubscriptionQuota> findBySubscriptionIdAndQuotaType(UUID subscriptionId, QuotaType quotaType) {
        return springDataSubscriptionQuotaRepository.findBySubscriptionIdAndQuotaType(subscriptionId, quotaType.name())
            .map(SubscriptionQuotaMapper::toDomain);
    }

    @Override
    public boolean tryConsume(UUID quotaId, BigDecimal amount) {
        return springDataSubscriptionQuotaRepository.tryConsume(quotaId, amount) > 0;
    }

    @Override
    public void addAllocation(UUID quotaId, BigDecimal amount) {
        springDataSubscriptionQuotaRepository.addAllocation(quotaId, amount);
    }

    @Override
    public void addUsage(UUID quotaId, BigDecimal amount) {
        springDataSubscriptionQuotaRepository.addUsage(quotaId, amount);
    }
}
