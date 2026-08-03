package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.personalization.SavedTopicRepository;
import com.sep.vox.infrastructure.persistence.entity.SavedTopicJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSavedTopicRepository;

@Repository
public class SavedTopicRepositoryImpl implements SavedTopicRepository {

    private final SpringDataSavedTopicRepository repository;

    public SavedTopicRepositoryImpl(SpringDataSavedTopicRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsForStudent(UUID studentId, UUID topicId) {
        return repository.existsByStudentIdAndPracticeTopicId(studentId, topicId);
    }

    @Override
    @Transactional
    public void save(UUID studentId, UUID topicId) {
        repository.save(new SavedTopicJpaEntity(
            UUID.randomUUID(),
            studentId,
            topicId,
            Instant.now()
        ));
    }

    @Override
    @Transactional
    public void delete(UUID studentId, UUID topicId) {
        repository.deleteByStudentIdAndPracticeTopicId(studentId, topicId);
    }
}
