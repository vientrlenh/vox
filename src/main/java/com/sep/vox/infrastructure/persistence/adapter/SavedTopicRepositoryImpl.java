package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.SavedTopicRepository;
import com.sep.vox.infrastructure.persistence.entity.personalization.SavedTopicJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSavedTopicRepository;

@Repository
@RequiredArgsConstructor
public class SavedTopicRepositoryImpl implements SavedTopicRepository {

    private final SpringDataSavedTopicRepository repository;

    @Override
    public boolean existsForStudent(UUID studentId, UUID topicId) {
        return repository.existsByStudentIdAndPracticeTopicId(studentId, topicId);
    }

    @Override
    @Transactional
    public void save(UUID studentId, UUID topicId) {
        repository.save(SavedTopicJpaEntity.builder()
                .studentId(studentId)
                .practiceTopicId(topicId)
                .savedAt(Instant.now())
                .build());
    }

    @Override
    @Transactional
    public void delete(UUID studentId, UUID topicId) {
        repository.deleteByStudentIdAndPracticeTopicId(studentId, topicId);
    }
}
