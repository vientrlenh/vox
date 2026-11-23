package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.StudentQuestionExposureRepository;
import com.sep.vox.infrastructure.persistence.entity.question.StudentQuestionExposureJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataStudentQuestionExposureRepository;

@Repository
@RequiredArgsConstructor
public class StudentQuestionExposureRepositoryImpl implements StudentQuestionExposureRepository {

    private final SpringDataStudentQuestionExposureRepository repository;

    @Override
    @Transactional
    public void recordExposure(UUID studentId, UUID questionId) {
        StudentQuestionExposureJpaEntity exposure = repository
            .findByStudentIdAndPracticeQuestionId(studentId, questionId)
            .orElse(null);
        if (exposure == null) {
            repository.save(StudentQuestionExposureJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .studentId(studentId)
                    .practiceQuestionId(questionId)
                    .seenAt(Instant.now())
                    .build());
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
