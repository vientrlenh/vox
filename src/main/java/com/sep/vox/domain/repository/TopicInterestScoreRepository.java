package com.sep.vox.domain.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.TopicInterestScoreEntry;

public interface TopicInterestScoreRepository {

    void replaceForStudent(UUID studentId, List<TopicInterestScoreEntry> scores);
}
