package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SavedTopicJpaEntity;

public interface SpringDataSavedTopicRepository
        extends JpaRepository<SavedTopicJpaEntity, UUID> {

    boolean existsByStudentIdAndPracticeTopicId(UUID studentId, UUID practiceTopicId);

    long deleteByStudentIdAndPracticeTopicId(UUID studentId, UUID practiceTopicId);
}
