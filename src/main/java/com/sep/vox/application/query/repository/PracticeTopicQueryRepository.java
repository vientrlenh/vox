package com.sep.vox.application.query.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.application.query.dto.RankedTopicInfo;
import com.sep.vox.application.query.dto.TopicSearchRowInfo;

public interface PracticeTopicQueryRepository {

    List<RankedTopicInfo> findRankedTopics(UUID studentId, String goal);

    List<RankedTopicInfo> findRankedTopicsByWeakness(
        UUID studentId, String goal, String criterion, String subAttribute
    );

    List<TopicSearchRowInfo> searchTopics(UUID studentId, String pattern, String normalized);

    Optional<TopicSearchRowInfo> findRandomActiveTopic(UUID studentId);

    List<TopicSearchRowInfo> findSavedTopics(UUID studentId);
}
