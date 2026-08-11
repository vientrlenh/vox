package com.sep.vox.application.query.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.application.query.dto.RankedTopicInfo;
import com.sep.vox.application.query.dto.TopicSearchRowInfo;

public interface PracticeTopicQueryRepository {

    List<RankedTopicInfo> findRankedTopics(UUID studentId, String goal);

    List<TopicSearchRowInfo> searchTopics(UUID studentId, String pattern, String normalized);

    /** Nạp lại chủ đề còn active theo id -- hydrate kết quả tìm bằng vector từ Postgres. */
    List<TopicSearchRowInfo> findActiveByIds(UUID studentId, java.util.Collection<UUID> topicIds);

    Optional<TopicSearchRowInfo> findRandomActiveTopic(UUID studentId);

    List<TopicSearchRowInfo> findSavedTopics(UUID studentId);
}
