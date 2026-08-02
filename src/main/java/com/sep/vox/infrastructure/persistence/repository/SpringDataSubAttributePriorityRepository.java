package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.application.query.dto.CriterionSubAttributeInfo;
import com.sep.vox.infrastructure.persistence.entity.SubAttributePriorityJpaEntity;

public interface SpringDataSubAttributePriorityRepository
        extends JpaRepository<SubAttributePriorityJpaEntity, UUID> {

    void deleteByStudentIdIn(List<UUID> studentIds);

    @Query(value = """
        SELECT criterion.code AS criterionCode, priority.sub_attribute AS subAttribute
        FROM sub_attribute_priority priority
        JOIN framework_criteria criterion
          ON criterion.id = priority.framework_criterion_id
        WHERE priority.student_id = :studentId
          AND priority.practiceable = true
        ORDER BY priority.priority DESC
        """, nativeQuery = true)
    List<CriterionSubAttributeInfo> findPracticeablePrioritiesOrderedDesc(@Param("studentId") UUID studentId);
}
