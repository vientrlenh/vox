package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolSubscriptionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolSubscriptionRepository;

@Repository
public class SchoolSubscriptionRepositoryImpl implements SchoolSubscriptionRepository {

    private final SpringDataSchoolSubscriptionRepository springDataSchoolSubscriptionRepository;

    public SchoolSubscriptionRepositoryImpl(SpringDataSchoolSubscriptionRepository springDataSchoolSubscriptionRepository) {
        this.springDataSchoolSubscriptionRepository = springDataSchoolSubscriptionRepository;
    }

    @Override
    public Optional<SchoolSubscription> findById(UUID id) {
        return springDataSchoolSubscriptionRepository.findById(id).map(SchoolSubscriptionMapper::toDomain);
    }

    @Override
    public SchoolSubscription save(SchoolSubscription subscription) {
        var entity = SchoolSubscriptionMapper.toJpa(subscription);
        var saved = springDataSchoolSubscriptionRepository.save(entity);
        return SchoolSubscriptionMapper.toDomain(saved);
    }

    @Override
    public Optional<SchoolSubscription> findActiveBySchoolId(UUID schoolId) {
        return springDataSchoolSubscriptionRepository
            .findFirstBySchoolIdAndStatus(schoolId, SubscriptionStatus.ACTIVE.name())
            .map(SchoolSubscriptionMapper::toDomain);
    }

    @Override
    public List<SchoolSubscription> findAllBySchoolId(UUID schoolId) {
        return springDataSchoolSubscriptionRepository.findAllBySchoolId(schoolId).stream()
            .map(SchoolSubscriptionMapper::toDomain)
            .toList();
    }

    @Override
    public PageResult<SchoolSubscription> findAllForAdmin(UUID planId, SubscriptionStatus status, String keyword, int page, int size) {
        var result = springDataSchoolSubscriptionRepository.findAllForAdmin(
            planId,
            status == null ? null : status.name(),
            StringNormalization.toLikePattern(keyword),
            PageRequest.of(page, size)
        );
        return new PageResult<>(
            result.getContent().stream().map(SchoolSubscriptionMapper::toDomain).toList(),
            page,
            size,
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Override
    public boolean existsActiveByPlanId(UUID planId) {
        return springDataSchoolSubscriptionRepository.existsByPlanIdAndStatus(planId, SubscriptionStatus.ACTIVE.name());
    }

    @Override
    public int expireOverdue(LocalDate today) {
        return springDataSchoolSubscriptionRepository.expireOverdue(today);
    }

    @Override
    public Optional<UUID> findActiveSubscriptionIdForUser(UUID userId) {
        return springDataSchoolSubscriptionRepository.findActiveSubscriptionIdForUser(userId);
    }

    @Override
    public BigDecimal findPracticeQuotaRemaining(UUID userId) {
        return springDataSchoolSubscriptionRepository.findPracticeQuotaRemaining(userId)
            .stream()
            .findFirst()
            .orElse(BigDecimal.ZERO);
    }

    @Override
    public Integer findMaxTimePerAttemptMinForUser(UUID userId) {
        return springDataSchoolSubscriptionRepository.findMaxTimePerAttemptMinForUser(userId)
            .stream()
            .findFirst()
            .orElse(null);
    }
}
