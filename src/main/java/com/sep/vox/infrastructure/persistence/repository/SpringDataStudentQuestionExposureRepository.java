package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.StudentQuestionExposureJpaEntity;

public interface SpringDataStudentQuestionExposureRepository
        extends JpaRepository<StudentQuestionExposureJpaEntity, UUID> {

    Optional<StudentQuestionExposureJpaEntity> findByStudentIdAndPracticeQuestionId(
        UUID studentId,
        UUID practiceQuestionId
    );
}
