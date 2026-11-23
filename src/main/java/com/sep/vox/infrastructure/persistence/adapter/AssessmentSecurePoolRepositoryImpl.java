package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentSecurePoolJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentSecurePool;
import com.sep.vox.domain.repository.AssessmentSecurePoolRepository;
import com.sep.vox.infrastructure.persistence.mapper.AssessmentSecurePoolMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataAssessmentSecurePoolRepository;

@Repository
@RequiredArgsConstructor
public class AssessmentSecurePoolRepositoryImpl implements AssessmentSecurePoolRepository {

    private final SpringDataAssessmentSecurePoolRepository springDataAssessmentSecurePoolRepository;

    @Override
    public AssessmentSecurePool save(AssessmentSecurePool pool) {
        AssessmentSecurePoolJpaEntity saved = springDataAssessmentSecurePoolRepository.save(AssessmentSecurePoolMapper.toJpa(pool));
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
