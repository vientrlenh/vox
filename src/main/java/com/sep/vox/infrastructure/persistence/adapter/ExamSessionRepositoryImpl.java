package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
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
    public List<ExamSession> findByCandidateId(UUID candidateId) {
        return springDataExamSessionRepository.findByCandidateId(candidateId).stream()
            .map(ExamSessionMapper::toDomain)
            .toList();
    }
}
