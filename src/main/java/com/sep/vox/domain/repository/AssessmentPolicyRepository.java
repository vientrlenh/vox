package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;

public interface AssessmentPolicyRepository {
    Optional<AssessmentPolicy> findById(UUID id);
    AssessmentPolicy save(AssessmentPolicy policy);
}
