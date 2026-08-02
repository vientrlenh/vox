package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.TopicInterestRowInfo;
import com.sep.vox.application.query.repository.TopicInterestScoreQueryRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataTopicInterestScoreRepository;

@Repository
public class JpaTopicInterestScoreQueryRepository implements TopicInterestScoreQueryRepository {

    private final SpringDataTopicInterestScoreRepository repository;

    public JpaTopicInterestScoreQueryRepository(SpringDataTopicInterestScoreRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TopicInterestRowInfo> findInterestProfileRows(UUID studentId) {
        return repository.findInterestProfileRows(studentId);
    }
}
