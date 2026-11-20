package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ExamRecordingJpaEntity;

public interface SpringDataExamRecordingRepository extends JpaRepository<ExamRecordingJpaEntity, UUID> {
    List<ExamRecordingJpaEntity> findByExamSessionId(UUID examSessionId);
    Optional<ExamRecordingJpaEntity> findByExamSessionIdAndStreamTypeAndSource(
        UUID examSessionId, String streamType, String source);
    void deleteByExamSessionId(UUID examSessionId);
}
