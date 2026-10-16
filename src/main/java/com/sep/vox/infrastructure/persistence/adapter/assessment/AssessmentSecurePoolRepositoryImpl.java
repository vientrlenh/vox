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
    public Optional<AssessmentSecurePool> findByAssessmentId(UUID assessmentId) {
        return springDataAssessmentSecurePoolRepository.findByAssessmentId(assessmentId)
            .map(AssessmentSecurePoolMapper::toDomain);
    }

    @Override
    public List<AssessmentSecurePool> findByAssessmentIdIn(Collection<UUID> assessmentIds) {
        if (assessmentIds.isEmpty()) {
            return List.of();
        }
        return springDataAssessmentSecurePoolRepository.findByAssessmentIdIn(assessmentIds).stream()
            .map(AssessmentSecurePoolMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteByAssessmentId(UUID assessmentId) {
        springDataAssessmentSecurePoolRepository.deleteByAssessmentId(assessmentId);
    }
}
