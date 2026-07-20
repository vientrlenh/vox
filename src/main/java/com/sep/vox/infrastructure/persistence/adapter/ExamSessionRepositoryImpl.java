package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamSessionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamSessionRepository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

@Repository
public class ExamSessionRepositoryImpl implements ExamSessionRepository {

    private final SpringDataExamSessionRepository springDataExamSessionRepository;

    public ExamSessionRepositoryImpl(SpringDataExamSessionRepository springDataExamSessionRepository) {
        this.springDataExamSessionRepository = springDataExamSessionRepository;
    }

    @Override
    public ExamSession save(ExamSession session) {
        var entity = ExamSessionMapper.toJpa(session);
        var saved = springDataExamSessionRepository.save(entity);
        return ExamSessionMapper.toDomain(saved);
    }

    @Override
    public Optional<ExamSession> findActiveByExamIdAndCandidateId(UUID examId, UUID candidateId) {
        return springDataExamSessionRepository.findByExamIdAndCandidateIdAndStatus(examId, candidateId, ExamStatus.IN_PROGRESS.name()).map(ExamSessionMapper::toDomain);
    }

    @Override
    public Optional<ExamSession> findByIdAndInProgress(UUID id) {
        return springDataExamSessionRepository.findByIdAndStatus(id, ExamSessionStatus.IN_PROGRESS.name())
            .map(ExamSessionMapper::toDomain);
    }
}
