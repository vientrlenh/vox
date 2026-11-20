package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.AssessmentCandidateResultJpaEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.infrastructure.persistence.mapper.AssessmentCandidateResultMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataAssessmentCandidateResultRepository;

@Repository
public class AssessmentCandidateResultRepositoryImpl implements AssessmentCandidateResultRepository {

    private final SpringDataAssessmentCandidateResultRepository springDataAssessmentCandidateResultRepository;

    public AssessmentCandidateResultRepositoryImpl(SpringDataAssessmentCandidateResultRepository springDataAssessmentCandidateResultRepository) {
        this.springDataAssessmentCandidateResultRepository = springDataAssessmentCandidateResultRepository;
    }

    @Override
    public Optional<AssessmentCandidateResult> findById(UUID id) {
        return springDataAssessmentCandidateResultRepository.findById(id)
            .map(AssessmentCandidateResultMapper::toDomain);
    }

    @Override
    public List<AssessmentCandidateResult> findByIdIn(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentCandidateResultRepository.findByIdIn(ids).stream()
            .map(AssessmentCandidateResultMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<AssessmentCandidateResult> findBySessionId(UUID sessionId) {
        return springDataAssessmentCandidateResultRepository.findBySessionId(sessionId)
            .map(AssessmentCandidateResultMapper::toDomain);
    }

    @Override
    public List<AssessmentCandidateResult> findBySessionIdIn(Collection<UUID> sessionIds) {
        return springDataAssessmentCandidateResultRepository.findBySessionIdIn(sessionIds).stream()
            .map(AssessmentCandidateResultMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentCandidateResult> findByAssessmentId(UUID assessmentId) {
        return springDataAssessmentCandidateResultRepository.findByAssessmentId(assessmentId).stream()
            .map(AssessmentCandidateResultMapper::toDomain)
            .toList();
    }

    @Override
    public AssessmentCandidateResult save(AssessmentCandidateResult result) {
        AssessmentCandidateResultJpaEntity saved = springDataAssessmentCandidateResultRepository.save(AssessmentCandidateResultMapper.toJpa(result));
        return AssessmentCandidateResultMapper.toDomain(saved);
    }

    @Override
    public void deleteBySessionId(UUID sessionId) {
        springDataAssessmentCandidateResultRepository.deleteBySessionId(sessionId);
    }

    @Override
    public int softDeleteBySessionId(UUID sessionId, Instant deletedAt, String reason) {
        return springDataAssessmentCandidateResultRepository.softDeleteBySessionId(sessionId, deletedAt, reason);
    }
}
