package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamCandidateResultMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamCandidateResultRepository;

@Repository
public class ExamCandidateResultRepositoryImpl implements ExamCandidateResultRepository {

    private final SpringDataExamCandidateResultRepository springDataExamCandidateResultRepository;

    public ExamCandidateResultRepositoryImpl(SpringDataExamCandidateResultRepository springDataExamCandidateResultRepository) {
        this.springDataExamCandidateResultRepository = springDataExamCandidateResultRepository;
    }

    @Override
    public Optional<ExamCandidateResult> findById(UUID id) {
        return springDataExamCandidateResultRepository.findById(id)
            .map(ExamCandidateResultMapper::toDomain);
    }

    @Override
    public List<ExamCandidateResult> findByIdIn(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return springDataExamCandidateResultRepository.findByIdIn(ids).stream()
            .map(ExamCandidateResultMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<ExamCandidateResult> findBySessionId(UUID sessionId) {
        return springDataExamCandidateResultRepository.findBySessionId(sessionId)
            .map(ExamCandidateResultMapper::toDomain);
    }

    @Override
    public List<ExamCandidateResult> findBySessionIdIn(Collection<UUID> sessionIds) {
        return springDataExamCandidateResultRepository.findBySessionIdIn(sessionIds).stream()
            .map(ExamCandidateResultMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamCandidateResult> findByExamId(UUID examId) {
        return springDataExamCandidateResultRepository.findByExamId(examId).stream()
            .map(ExamCandidateResultMapper::toDomain)
            .toList();
    }

    @Override
    public ExamCandidateResult save(ExamCandidateResult result) {
        var saved = springDataExamCandidateResultRepository.save(ExamCandidateResultMapper.toJpa(result));
        return ExamCandidateResultMapper.toDomain(saved);
    }

    @Override
    public void deleteBySessionId(UUID sessionId) {
        springDataExamCandidateResultRepository.deleteBySessionId(sessionId);
    }

    @Override
    public int softDeleteBySessionId(UUID sessionId, Instant deletedAt, String reason) {
        return springDataExamCandidateResultRepository.softDeleteBySessionId(sessionId, deletedAt, reason);
    }
}
