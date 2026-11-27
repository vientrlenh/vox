package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.QuestionTopicInfo;
import com.sep.vox.application.query.dto.RankedTopicInfo;
import com.sep.vox.application.query.dto.TopicNameCardInfo;
import com.sep.vox.application.query.dto.TopicSearchRowInfo;
import com.sep.vox.application.query.repository.PracticeTopicQueryRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticeTopicRepository;

@Repository
@RequiredArgsConstructor
public class JpaPracticeTopicQueryRepository implements PracticeTopicQueryRepository {

    private final SpringDataPracticeTopicRepository repository;

    @Override
    public List<RankedTopicInfo> findRankedTopics(UUID studentId, String goal) {
        return repository.findRankedTopics(studentId, goal);
    }

    @Override
    public List<TopicSearchRowInfo> searchTopics(UUID studentId, String pattern, String normalized) {
        return repository.searchTopics(studentId, pattern, normalized);
    }

    @Override
    public List<TopicSearchRowInfo> findActiveByIds(UUID studentId, java.util.Collection<UUID> topicIds) {
        // An empty IN () is an SQL syntax error, not "matches nothing" -- blocked here.
        return topicIds.isEmpty() ? List.of() : repository.findActiveByIds(studentId, topicIds);
    }

    @Override
    public Optional<TopicSearchRowInfo> findRandomActiveTopic(UUID studentId) {
        return repository.findRandomActiveTopic(studentId);
    }

    @Override
    public List<TopicSearchRowInfo> findSavedTopics(UUID studentId) {
        return repository.findSavedTopics(studentId);
    }

    @Override
    public List<TopicNameCardInfo> findActiveNameCards() {
        return repository.findActiveNameCards();
    }

    @Override
    public List<QuestionTopicInfo> findPublishedAssessmentTopics(UUID schoolId, UUID gradeId) {
        return repository.findPublishedAssessmentTopics(schoolId, gradeId);
    }
}
