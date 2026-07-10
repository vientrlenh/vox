package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamSessionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamSessionRepository;

@Repository
public class ExamSessionRepositoryImpl implements ExamSessionRepository {

    private final SpringDataExamSessionRepository springDataExamSessionRepository;

    public ExamSessionRepositoryImpl(SpringDataExamSessionRepository springDataExamSessionRepository) {
        this.springDataExamSessionRepository = springDataExamSessionRepository;
    }

    @Override
    public Optional<ExamSession> findById(UUID id) {
        return springDataExamSessionRepository.findById(id)
            .map(ExamSessionMapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataExamSessionRepository.existsById(id);
    }

    @Override
    public ExamSession save(ExamSession session) {
        var saved = springDataExamSessionRepository.save(ExamSessionMapper.toJpa(session));
        return ExamSessionMapper.toDomain(saved);
    }
}
