package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.application.query.dto.TopicInterestRowInfo;
import com.sep.vox.infrastructure.persistence.entity.TopicInterestScoreJpaEntity;

public interface SpringDataTopicInterestScoreRepository
        extends JpaRepository<TopicInterestScoreJpaEntity, UUID> {

    void deleteByStudentId(UUID studentId);

    @Query(value = """
        SELECT topic.id AS id, topic.name AS name, score.score AS score,
               score.sessions_mentioned AS sessionsMentioned,
               score.last_mentioned_at AS lastMentionedAt
        FROM topic_interest_score score
        JOIN practice_topic topic ON topic.id = score.practice_topic_id
        WHERE score.student_id = :studentId
        ORDER BY score.score DESC, topic.name
        """, nativeQuery = true)
    List<TopicInterestRowInfo> findInterestProfileRows(@Param("studentId") UUID studentId);
}
