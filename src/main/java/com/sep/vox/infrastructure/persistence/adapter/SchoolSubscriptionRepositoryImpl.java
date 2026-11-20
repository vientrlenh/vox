package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
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
            .findInForceBySchoolId(schoolId, Instant.now())
            .stream()
            .findFirst()
            .map(SchoolSubscriptionMapper::toDomain);
    }

    @Override
    public Optional<SchoolSubscription> findMostRecentBySchoolId(UUID schoolId) {
        return springDataSchoolSubscriptionRepository
            .findMostRecentBySchoolId(schoolId)
            .stream()
            .findFirst()
            .map(SchoolSubscriptionMapper::toDomain);
    }

    @Override
    public List<SchoolSubscription> findUnfinishedBySchoolId(UUID schoolId, Instant at) {
        return springDataSchoolSubscriptionRepository.findUnfinishedBySchoolId(schoolId, at).stream()
            .map(SchoolSubscriptionMapper::toDomain)
            .toList();
    }

    @Override
    public List<SchoolSubscription> findBySchoolId(UUID schoolId) {
        return springDataSchoolSubscriptionRepository.findBySchoolId(schoolId).stream()
            .map(SchoolSubscriptionMapper::toDomain)
            .toList();
    }

    @Override
    public PageResult<SchoolSubscription> findForAdmin(
            UUID planId, SchoolSubscriptionStatus status, String keyword, int page, int size) {
        var result = springDataSchoolSubscriptionRepository.findForAdmin(
            planId,
            status == null ? null : status.name(),
            StringNormalization.toLikePattern(keyword),
            // 1-based vào, 0-based xuống PageRequest -- xem OrderRepositoryImpl.findBySchoolId.
            PageRequest.of(page - 1, size)
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
        return springDataSchoolSubscriptionRepository
            .existsBySubscriptionPlanIdAndStatus(planId, SchoolSubscriptionStatus.ACTIVE.name());
    }

    @Override
    public int expireOverdue(Instant cutoff) {
        return springDataSchoolSubscriptionRepository.expireOverdue(cutoff);
    }

    @Override
    public Optional<UUID> findActiveSubscriptionIdForUser(UUID userId) {
        return springDataSchoolSubscriptionRepository.findActiveSubscriptionIdForUser(userId);
    }

    @Override
    public BigDecimal findPracticeSpendableFundsVnd(UUID userId) {
        return springDataSchoolSubscriptionRepository.findPracticeSpendableFundsVnd(userId)
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
