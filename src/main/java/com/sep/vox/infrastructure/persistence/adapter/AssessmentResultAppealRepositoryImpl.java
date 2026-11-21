package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentResultAppealJpaEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;
import com.sep.vox.infrastructure.persistence.mapper.AssessmentResultAppealMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataAssessmentResultAppealRepository;

@Repository
public class AssessmentResultAppealRepositoryImpl implements AssessmentResultAppealRepository {

    private final SpringDataAssessmentResultAppealRepository springDataAssessmentResultAppealRepository;

    public AssessmentResultAppealRepositoryImpl(
            SpringDataAssessmentResultAppealRepository springDataAssessmentResultAppealRepository) {
        this.springDataAssessmentResultAppealRepository = springDataAssessmentResultAppealRepository;
    }

    @Override
    public Optional<AssessmentResultAppeal> findById(UUID id) {
        return springDataAssessmentResultAppealRepository.findById(id)
            .map(AssessmentResultAppealMapper::toDomain);
    }

    @Override
    public AssessmentResultAppeal save(AssessmentResultAppeal appeal) {
        AssessmentResultAppealJpaEntity saved = springDataAssessmentResultAppealRepository.save(AssessmentResultAppealMapper.toJpa(appeal));
        return AssessmentResultAppealMapper.toDomain(saved);
    }

    @Override
    public boolean existsOpenByCandidateResultId(UUID candidateResultId) {
        return springDataAssessmentResultAppealRepository.existsOpenByCandidateResultId(candidateResultId);
    }

    @Override
    public long countPublishedByCandidateResultId(UUID candidateResultId) {
        return springDataAssessmentResultAppealRepository.countPublishedByCandidateResultId(candidateResultId);
    }

    @Override
    public List<AssessmentResultAppeal> findByCandidateResultId(UUID candidateResultId) {
        return springDataAssessmentResultAppealRepository.findByCandidateResultId(candidateResultId).stream()
            .map(AssessmentResultAppealMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteByIdIn(Collection<UUID> ids) {
        springDataAssessmentResultAppealRepository.deleteAllByIdInBatch(ids);
    }

    @Override
    public long countBySchoolIdAndStatusIn(UUID schoolId, Collection<AssessmentAppealStatus> statuses) {
        List<String> statusNames = statuses.stream().map(e -> e.name()).toList();
        return springDataAssessmentResultAppealRepository.countBySchoolIdAndStatusIn(schoolId, statusNames);
    }

    @Override
    public Instant findOldestPendingRequestedAt(UUID schoolId) {
        return springDataAssessmentResultAppealRepository.findOldestPendingRequestedAtBySchoolId(schoolId);
    }
}
