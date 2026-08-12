package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.StudentQuestionExposureRepository;
import com.sep.vox.infrastructure.persistence.entity.StudentQuestionExposureJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataStudentQuestionExposureRepository;

@Repository
public class StudentQuestionExposureRepositoryImpl implements StudentQuestionExposureRepository {

    private final SpringDataStudentQuestionExposureRepository repository;

    public StudentQuestionExposureRepositoryImpl(SpringDataStudentQuestionExposureRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void recordExposure(UUID studentId, UUID questionId) {
        var exposure = repository
            .findByStudentIdAndPracticeQuestionId(studentId, questionId)
            .orElse(null);
        if (exposure == null) {
            repository.save(new StudentQuestionExposureJpaEntity(
                UUID.randomUUID(),
                studentId,
                questionId,
                Instant.now()
            ));
        } else {
            exposure.setSeenAt(Instant.now());
            repository.save(exposure);
        }
    }

    @Override
    @Transactional
    public void removeExposure(UUID studentId, UUID questionId) {
        repository.deleteByStudentIdAndPracticeQuestionId(studentId, questionId);
    }
}
