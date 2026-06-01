package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.infrastructure.persistence.mapper.AssessmentPolicyMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataAssessmentPolicyRepository;

@Repository
public class AssessmentPolicyRepositoryImpl implements AssessmentPolicyRepository {

    private final SpringDataAssessmentPolicyRepository springDataAssessmentPolicyRepository;

    public AssessmentPolicyRepositoryImpl(SpringDataAssessmentPolicyRepository springDataAssessmentPolicyRepository) {
        this.springDataAssessmentPolicyRepository = springDataAssessmentPolicyRepository;
    }

    @Override
    public Optional<AssessmentPolicy> findById(UUID id) {
        return springDataAssessmentPolicyRepository.findById(id).map(AssessmentPolicyMapper::toDomain);
    }

    @Override
    public AssessmentPolicy save(AssessmentPolicy policy) {
        var entity = AssessmentPolicyMapper.toJpa(policy);
        var saved = springDataAssessmentPolicyRepository.save(entity);
        return AssessmentPolicyMapper.toDomain(saved);
    }
}
