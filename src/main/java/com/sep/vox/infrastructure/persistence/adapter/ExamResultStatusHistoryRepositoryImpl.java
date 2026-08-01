package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamResultStatusHistory;
import com.sep.vox.domain.repository.ExamResultStatusHistoryRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamResultStatusHistoryMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamResultStatusHistoryRepository;

@Repository
public class ExamResultStatusHistoryRepositoryImpl implements ExamResultStatusHistoryRepository {

    private final SpringDataExamResultStatusHistoryRepository springDataExamResultStatusHistoryRepository;

    public ExamResultStatusHistoryRepositoryImpl(
            SpringDataExamResultStatusHistoryRepository springDataExamResultStatusHistoryRepository) {
        this.springDataExamResultStatusHistoryRepository = springDataExamResultStatusHistoryRepository;
    }

    @Override
    public ExamResultStatusHistory save(ExamResultStatusHistory history) {
        var saved = springDataExamResultStatusHistoryRepository.save(
            ExamResultStatusHistoryMapper.toJpa(history));
        return ExamResultStatusHistoryMapper.toDomain(saved);
    }

    @Override
    public List<ExamResultStatusHistory> saveAll(List<ExamResultStatusHistory> histories) {
        if (histories.isEmpty()) {
            return List.of();
        }
        var entities = histories.stream().map(ExamResultStatusHistoryMapper::toJpa).toList();
        return springDataExamResultStatusHistoryRepository.saveAll(entities).stream()
            .map(ExamResultStatusHistoryMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamResultStatusHistory> findByCandidateResultIdOrderByCreatedAtAsc(UUID candidateResultId) {
        return springDataExamResultStatusHistoryRepository
            .findByCandidateResultIdOrderByCreatedAtAsc(candidateResultId).stream()
            .map(ExamResultStatusHistoryMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamResultStatusHistory> findByCandidateResultIdIn(Collection<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return List.of();
        }
        return springDataExamResultStatusHistoryRepository
            .findByCandidateResultIdInOrderByCreatedAtAsc(candidateResultIds).stream()
            .map(ExamResultStatusHistoryMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteByCandidateResultIdIn(Collection<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return;
        }
        springDataExamResultStatusHistoryRepository.deleteByCandidateResultIdIn(candidateResultIds);
    }
}
