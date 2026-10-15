package com.sep.vox.domain.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentSecurePool;

public interface AssessmentSecurePoolRepository {
    AssessmentSecurePool save(AssessmentSecurePool pool);
    Optional<AssessmentSecurePool> findById(UUID id);
    Optional<AssessmentSecurePool> findByExamId(UUID examId);
    List<AssessmentSecurePool> findByExamIdIn(Collection<UUID> examIds);
    void deleteByExamId(UUID examId);
}
