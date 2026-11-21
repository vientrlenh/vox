package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.personalization.TopicInterestEventJpaEntity;

public interface SpringDataTopicInterestEventRepository
        extends JpaRepository<TopicInterestEventJpaEntity, UUID> {

    List<TopicInterestEventJpaEntity> findByStudentIdOrderByOccurredAtAscIdAsc(UUID studentId);
}
