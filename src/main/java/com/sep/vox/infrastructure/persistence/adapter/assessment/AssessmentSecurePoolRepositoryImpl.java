package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentSecurePool;
import com.sep.vox.domain.repository.assessment.AssessmentSecurePoolRepository;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentSecurePoolMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentSecurePoolRepository;

@Repository
public class AssessmentSecurePoolRepositoryImpl implements AssessmentSecurePoolRepository {

    private final SpringDataAssessmentSecurePoolRepository springDataAssessmentSecurePoolRepository;

    public AssessmentSecurePoolRepositoryImpl(SpringDataAssessmentSecurePoolRepository springDataAssessmentSecurePoolRepository) {
        this.springDataAssessmentSecurePoolRepository = springDataAssessmentSecurePoolRepository;
    }

    @Override
    public AssessmentSecurePool save(AssessmentSecurePool pool) {
        var saved = springDataAssessmentSecurePoolRepository.save(AssessmentSecurePoolMapper.toJpa(pool));
        return AssessmentSecurePoolMapper.toDomain(saved);
    }

    @Override
    public Optional<AssessmentSecurePool> findById(UUID id) {
        return springDataAssessmentSecurePoolRepository.findById(id)
            .map(AssessmentSecurePoolMapper::toDomain);
    }

    @Override
    public Optional<AssessmentSecurePool> findByExamId(UUID examId) {
        return springDataAssessmentSecurePoolRepository.findByExamId(examId)
            .map(AssessmentSecurePoolMapper::toDomain);
    }

    @Override
    public List<AssessmentSecurePool> findByExamIdIn(Collection<UUID> examIds) {
        if (examIds.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentSecurePoolRepository.findByExamIdIn(examIds).stream()
            .map(AssessmentSecurePoolMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteByExamId(UUID examId) {
        springDataAssessmentSecurePoolRepository.deleteByExamId(examId);
    }
}
