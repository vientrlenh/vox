package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentResultAppealItemJpaEntity;

public interface SpringDataAssessmentResultAppealItemRepository
        extends JpaRepository<AssessmentResultAppealItemJpaEntity, UUID> {

    /** id is a uuidv7, so ordering by id keeps the order in which the student picked the parts. */
    List<AssessmentResultAppealItemJpaEntity> findByAppealIdOrderByIdAsc(UUID appealId);

    void deleteByAppealIdIn(Collection<UUID> appealIds);
}
