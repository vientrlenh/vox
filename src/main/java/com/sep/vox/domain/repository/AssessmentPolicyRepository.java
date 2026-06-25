package com.sep.vox.domain.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;

public interface AssessmentPolicyRepository {
    Optional<AssessmentPolicy> findById(UUID id);
    AssessmentPolicy save(AssessmentPolicy policy);
    Optional<AssessmentPolicy> findActivePolicy(UUID schoolId, UUID languageId, UUID classId, UUID gradeId, UUID gradeLevelId, OffsetDateTime atTime);
}
