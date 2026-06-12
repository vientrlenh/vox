package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.RubricVersionJpaEntity;

public interface SpringDataRubricVersionRepository extends JpaRepository<RubricVersionJpaEntity, UUID> {
    boolean existsByRubricIdAndIdNot(UUID rubricId, UUID id);

    List<RubricVersionJpaEntity> findByRubricId(UUID rubricId);
}
