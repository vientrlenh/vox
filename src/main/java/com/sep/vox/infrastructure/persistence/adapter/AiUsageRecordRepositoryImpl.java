package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.metering.AiUsageRecord;
import com.sep.vox.domain.repository.AiUsageRecordRepository;
import com.sep.vox.infrastructure.persistence.mapper.AiUsageRecordMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataAiUsageRecordRepository;

@Repository
public class AiUsageRecordRepositoryImpl implements AiUsageRecordRepository {

    private final SpringDataAiUsageRecordRepository springDataAiUsageRecordRepository;

    public AiUsageRecordRepositoryImpl(SpringDataAiUsageRecordRepository springDataAiUsageRecordRepository) {
        this.springDataAiUsageRecordRepository = springDataAiUsageRecordRepository;
    }

    @Override
    public Optional<AiUsageRecord> findById(UUID id) {
        return springDataAiUsageRecordRepository.findById(id).map(AiUsageRecordMapper::toDomain);
    }

    @Override
    public AiUsageRecord save(AiUsageRecord record) {
        var entity = AiUsageRecordMapper.toJpa(record);
        var saved = springDataAiUsageRecordRepository.save(entity);
        return AiUsageRecordMapper.toDomain(saved);
    }

    @Override
    public List<AiUsageRecord> findByAssessmentSessionId(UUID assessmentSessionId) {
        return springDataAiUsageRecordRepository.findByAssessmentSessionId(assessmentSessionId).stream()
            .map(AiUsageRecordMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsByUsageEventId(UUID usageEventId) {
        return springDataAiUsageRecordRepository.existsByUsageEventId(usageEventId);
    }

    @Override
    public int markChargedByAssessmentSessionId(UUID assessmentSessionId, Instant chargedAt) {
        return springDataAiUsageRecordRepository.markChargedByAssessmentSessionId(assessmentSessionId, chargedAt);
    }

    @Override
    public int markWaivedByAssessmentSessionId(UUID assessmentSessionId, Instant waivedAt) {
        return springDataAiUsageRecordRepository.markWaivedByAssessmentSessionId(assessmentSessionId, waivedAt);
    }

    @Override
    public BigDecimal sumCostVndByAssessmentSessionIdAndChargedAt(UUID assessmentSessionId, Instant chargedAt) {
        return springDataAiUsageRecordRepository
            .sumCostVndByAssessmentSessionIdAndChargedAt(assessmentSessionId, chargedAt);
    }

    @Override
    public BigDecimal sumCostUsdByAssessmentSessionIdAndChargedAt(UUID assessmentSessionId, Instant chargedAt) {
        return springDataAiUsageRecordRepository
            .sumCostUsdByAssessmentSessionIdAndChargedAt(assessmentSessionId, chargedAt);
    }
}