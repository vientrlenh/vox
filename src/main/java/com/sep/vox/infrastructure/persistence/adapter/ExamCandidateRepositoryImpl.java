package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamCandidateMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamCandidateRepository;

@Repository
public class ExamCandidateRepositoryImpl implements ExamCandidateRepository {

    private final SpringDataExamCandidateRepository springDataExamCandidateRepository;

    public ExamCandidateRepositoryImpl(SpringDataExamCandidateRepository springDataExamCandidateRepository) {
        this.springDataExamCandidateRepository = springDataExamCandidateRepository;
    }

    @Override
    public ExamCandidate save(ExamCandidate candidate) {
        var saved = springDataExamCandidateRepository.save(ExamCandidateMapper.toJpa(candidate));
        return ExamCandidateMapper.toDomain(saved);
    }

    @Override
    public List<ExamCandidate> saveAll(Collection<ExamCandidate> candidates) {
        var entities = candidates.stream().map(ExamCandidateMapper::toJpa).toList();
        return springDataExamCandidateRepository.saveAll(entities).stream()
            .map(ExamCandidateMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamCandidate> findByExamId(UUID examId) {
        return springDataExamCandidateRepository.findByExamId(examId).stream()
            .map(ExamCandidateMapper::toDomain)
            .toList();
    }
}
