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

    /**
     * COALESCE trong câu truy vấn đã lo tập rỗng, nhưng vẫn kẹp null một lần nữa ở đây: hợp đồng của
     * cổng này là "không bao giờ null", và giữ nó đúng bằng chính mã của mình rẻ hơn là bằng một lời
     * hứa nằm trong chuỗi JPQL.
     */
    @Override
    public BigDecimal sumUnusedAllocation(UUID schoolSubscriptionId, QuotaType quotaType) {
        var sum = springDataRepository.sumUnusedAllocation(schoolSubscriptionId, quotaType.name());
        return sum == null ? BigDecimal.ZERO : sum;
    }

    /** Kẹp null một lần nữa ở đây, cùng lý do với {@link #sumUnusedAllocation}. */
    @Override
    public BigDecimal sumAllocatedForEligibleUsers(
            UUID schoolSubscriptionId, QuotaType quotaType, UUID schoolId, UUID roleId, String userStatus) {
        var sum = springDataRepository.sumAllocatedForEligibleUsers(
            schoolSubscriptionId, quotaType.name(), schoolId, roleId, userStatus);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Override
    public BigDecimal sumAllocated(UUID schoolSubscriptionId, QuotaType quotaType) {
        var sum = springDataRepository.sumAllocated(schoolSubscriptionId, quotaType.name());
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Override
    public void addUsage(UUID id, BigDecimal amount) {
        springDataRepository.addUsage(id, amount);
    }
}
