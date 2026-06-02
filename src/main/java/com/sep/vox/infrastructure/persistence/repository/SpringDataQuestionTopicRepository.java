package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.QuestionTopicJpaEntity;

public interface SpringDataQuestionTopicRepository extends JpaRepository<QuestionTopicJpaEntity, UUID> {
    List<QuestionTopicJpaEntity> findByQuestionBankId(UUID bankId);
    Page<QuestionTopicJpaEntity> findByQuestionBankId(UUID bankId, Pageable pageable);

    @Query("""
        SELECT CASE WHEN COUNT(qt) > 0 THEN true ELSE false END
        FROM QuestionTopicJpaEntity qt 
        JOIN QuestionBankJpaEntity qb 
            ON qt.questionBankId = qb.id 
        JOIN SchoolGradeJpaEntity sg 
            ON qb.schoolGradeId = sg.id
        WHERE qt.id = :questionTopicId 
            AND sg.schoolId = :schoolId
    """)
    boolean isTopicBelongToSchool(@Param("questionTopicId") UUID questionTopicId, @Param("schoolId") UUID schoolId);
}
