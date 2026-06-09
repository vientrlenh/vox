package com.sep.vox.application.query.repository;

import java.util.UUID;

public interface QuestionTopicPermissionQuery {
    boolean canCreateTopic(UUID bankId);
    boolean canUpdateTopic(UUID topicId);
    boolean canPublishTopic(UUID topicId);
    boolean canArchiveTopic(UUID topicId);
    boolean canRestoreTopic(UUID topicId);
}
