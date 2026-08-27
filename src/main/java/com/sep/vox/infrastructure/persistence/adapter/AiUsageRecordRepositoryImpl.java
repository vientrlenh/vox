package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.metering.AiUsageRecord;
import com.sep.vox.domain.repository.AiUsageRecordRepository;
import com.sep.vox.domain.repository.SessionCostAggregate;
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
    public List<AiUsageRecord> findByExamSessionId(UUID examSessionId) {
        return springDataAiUsageRecordRepository.findByExamSessionId(examSessionId).stream()
            .map(AiUsageRecordMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsByUsageEventId(UUID usageEventId) {
        return springDataAiUsageRecordRepository.existsByUsageEventId(usageEventId);
    }

    @Override
    public BigDecimal sumCostUsdByExamSessionId(UUID examSessionId) {
        return springDataAiUsageRecordRepository.sumCostUsdByExamSessionId(examSessionId);
    }

    @Override
    public BigDecimal sumCostVndByExamSessionId(UUID examSessionId) {
        return springDataAiUsageRecordRepository.sumCostVndByExamSessionId(examSessionId);
    }

    @Override
    public List<SessionCostAggregate> sumCostUsdGroupedBySessionSince(Instant since) {
        return springDataAiUsageRecordRepository.sumCostUsdGroupedBySessionSince(since);
    }
}