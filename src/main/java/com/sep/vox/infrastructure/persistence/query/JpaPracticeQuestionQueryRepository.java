package com.sep.vox.infrastructure.persistence.query;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.QuestionEvaluationInfo;
import com.sep.vox.application.query.repository.PracticeQuestionQueryRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticeQuestionRepository;

@Repository
@RequiredArgsConstructor
public class JpaPracticeQuestionQueryRepository implements PracticeQuestionQueryRepository {

    private final SpringDataPracticeQuestionRepository repository;

    @Override
    public Optional<QuestionEvaluationInfo> findQuestionWithTopic(UUID questionId) {
        return repository.findQuestionWithTopic(questionId)
            .map(row -> new QuestionEvaluationInfo(
                row.getQuestionText(),
                row.getEvaluationGuideJson(),
                row.getQuestionType(),
                row.getMinResponseSeconds(),
                row.getMaxResponseSeconds(),
                row.getTopicName(),
                row.getTopicDescription()
            ));
    }
}
