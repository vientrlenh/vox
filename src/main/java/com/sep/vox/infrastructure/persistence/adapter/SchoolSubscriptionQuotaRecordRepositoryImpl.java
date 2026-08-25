package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolSubscriptionQuotaRecordMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolSubscriptionQuotaRecordRepository;

@Repository
public class SchoolSubscriptionQuotaRecordRepositoryImpl implements SchoolSubscriptionQuotaRecordRepository {

    private final SpringDataSchoolSubscriptionQuotaRecordRepository springDataSchoolSubscriptionQuotaRecordRepository;

    public SchoolSubscriptionQuotaRecordRepositoryImpl(SpringDataSchoolSubscriptionQuotaRecordRepository springDataSchoolSubscriptionQuotaRecordRepository) {
        this.springDataSchoolSubscriptionQuotaRecordRepository = springDataSchoolSubscriptionQuotaRecordRepository;
    }

    @Override
    public Optional<SchoolSubscriptionQuotaRecord> findById(UUID id) {
        return springDataSchoolSubscriptionQuotaRecordRepository.findById(id).map(SchoolSubscriptionQuotaRecordMapper::toDomain);
    }

    @Override
    public SchoolSubscriptionQuotaRecord save(SchoolSubscriptionQuotaRecord quota) {
        var entity = SchoolSubscriptionQuotaRecordMapper.toJpa(quota);
        var saved = springDataSchoolSubscriptionQuotaRecordRepository.save(entity);
        return SchoolSubscriptionQuotaRecordMapper.toDomain(saved);
    }

    @Override
    public List<SchoolSubscriptionQuotaRecord> findBySchoolSubscriptionId(UUID schoolSubscriptionId) {
        return springDataSchoolSubscriptionQuotaRecordRepository.findBySchoolSubscriptionId(schoolSubscriptionId).stream()
            .map(SchoolSubscriptionQuotaRecordMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<SchoolSubscriptionQuotaRecord> findBySchoolSubscriptionIdAndQuotaType(UUID schoolSubscriptionId, QuotaType quotaType) {
        return springDataSchoolSubscriptionQuotaRecordRepository
            .findBySchoolSubscriptionIdAndQuotaType(schoolSubscriptionId, quotaType.name())
            .map(SchoolSubscriptionQuotaRecordMapper::toDomain);
    }

    @Override
    public boolean tryConsume(UUID quotaId, BigDecimal amount) {
        return springDataSchoolSubscriptionQuotaRecordRepository.tryConsume(quotaId, amount) > 0;
    }

    @Override
    public void addAllocation(UUID quotaId, BigDecimal amount) {
        springDataSchoolSubscriptionQuotaRecordRepository.addAllocation(quotaId, amount);
    }

    @Override
    public void addUsage(UUID quotaId, BigDecimal amount) {
        springDataSchoolSubscriptionQuotaRecordRepository.addUsage(quotaId, amount);
    }
}
