package com.sep.vox.domain.repository.personalization;

import java.util.UUID;

public interface SavedTopicRepository {

    boolean existsForStudent(UUID studentId, UUID topicId);

    void save(UUID studentId, UUID topicId);

    void delete(UUID studentId, UUID topicId);
}
