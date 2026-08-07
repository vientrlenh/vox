package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import com.sep.vox.infrastructure.persistence.entity.TopicSuggestionJpaEntity;

public interface SpringDataTopicSuggestionRepository
        extends JpaRepository<TopicSuggestionJpaEntity, UUID> {

    @Query(value = """
        SELECT COUNT(*)::int FROM topic_suggestion
        WHERE student_id = :studentId
          AND keyword IS NOT NULL
          AND created_at >= DATE_TRUNC('week', CURRENT_TIMESTAMP)
        """, nativeQuery = true)
    int countWeeklyKeywordRequests(@Param("studentId") UUID studentId);

}
