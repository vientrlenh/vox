package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentResultStatusHistory;
import com.sep.vox.domain.repository.assessment.AssessmentResultStatusHistoryRepository;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentResultStatusHistoryMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentResultStatusHistoryRepository;

@Repository
public class AssessmentResultStatusHistoryRepositoryImpl implements AssessmentResultStatusHistoryRepository {

    private final SpringDataAssessmentResultStatusHistoryRepository springDataAssessmentResultStatusHistoryRepository;

    public AssessmentResultStatusHistoryRepositoryImpl(
            SpringDataAssessmentResultStatusHistoryRepository springDataAssessmentResultStatusHistoryRepository) {
        this.springDataAssessmentResultStatusHistoryRepository = springDataAssessmentResultStatusHistoryRepository;
    }

    @Override
    public AssessmentResultStatusHistory save(AssessmentResultStatusHistory history) {
        var saved = springDataAssessmentResultStatusHistoryRepository.save(
            AssessmentResultStatusHistoryMapper.toJpa(history));
        return AssessmentResultStatusHistoryMapper.toDomain(saved);
    }

    @Override
    public List<AssessmentResultStatusHistory> saveAll(List<AssessmentResultStatusHistory> histories) {
        if (histories.isEmpty()) {
            return List.of();
        }
        var entities = histories.stream().map(AssessmentResultStatusHistoryMapper::toJpa).toList();
        return springDataAssessmentResultStatusHistoryRepository.saveAll(entities).stream()
            .map(AssessmentResultStatusHistoryMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentResultStatusHistory> findByCandidateResultIdOrderByCreatedAtAsc(UUID candidateResultId) {
        return springDataAssessmentResultStatusHistoryRepository
            .findByCandidateResultIdOrderByCreatedAtAsc(candidateResultId).stream()
            .map(AssessmentResultStatusHistoryMapper::toDomain)
            .toList();
    }

    @Override
    public List<AssessmentResultStatusHistory> findByCandidateResultIdIn(Collection<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentResultStatusHistoryRepository
            .findByCandidateResultIdInOrderByCreatedAtAsc(candidateResultIds).stream()
            .map(AssessmentResultStatusHistoryMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteByCandidateResultIdIn(Collection<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return;
        }
        springDataAssessmentResultStatusHistoryRepository.deleteByCandidateResultIdIn(candidateResultIds);
    }
}
