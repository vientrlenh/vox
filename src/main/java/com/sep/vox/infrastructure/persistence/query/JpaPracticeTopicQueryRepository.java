package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.RankedTopicInfo;
import com.sep.vox.application.query.dto.TopicSearchRowInfo;
import com.sep.vox.application.query.repository.PracticeTopicQueryRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticeTopicRepository;

@Repository
public class JpaPracticeTopicQueryRepository implements PracticeTopicQueryRepository {

    private final SpringDataPracticeTopicRepository repository;

    public JpaPracticeTopicQueryRepository(SpringDataPracticeTopicRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RankedTopicInfo> findRankedTopics(UUID studentId, String goal) {
        return repository.findRankedTopics(studentId, goal);
    }

    @Override
    public List<TopicSearchRowInfo> searchTopics(UUID studentId, String pattern, String normalized) {
        return repository.searchTopics(studentId, pattern, normalized);
    }

    @Override
    public Optional<TopicSearchRowInfo> findRandomActiveTopic(UUID studentId) {
        return repository.findRandomActiveTopic(studentId);
    }

    @Override
    public List<TopicSearchRowInfo> findSavedTopics(UUID studentId) {
        return repository.findSavedTopics(studentId);
    }
}
