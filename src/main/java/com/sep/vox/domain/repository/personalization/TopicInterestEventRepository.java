package com.sep.vox.domain.repository.personalization;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.TopicInterestEvent;

public interface TopicInterestEventRepository {

    void save(
        UUID studentId,
        UUID topicId,
        UUID sessionId,
        String eventType,
        double signal
    );

    List<TopicInterestEvent> findByStudent(UUID studentId);
}
