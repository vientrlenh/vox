package com.sep.vox.infrastructure.persistence.adapter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.repository.personalization.PracticeQuestionRepository;
import com.sep.vox.infrastructure.persistence.mapper.personalization.PracticeQuestionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticeQuestionRepository;

@Repository
public class PracticeQuestionRepositoryImpl implements PracticeQuestionRepository {

    private final SpringDataPracticeQuestionRepository repository;

    public PracticeQuestionRepositoryImpl(SpringDataPracticeQuestionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PracticeQuestion> findById(UUID id) {
        return repository.findById(id).map(PracticeQuestionMapper::toDomain);
    }

    @Override
    public List<PracticeQuestion> findUnseenByTopic(UUID topicId, UUID studentId) {
        return repository.findUnseenByTopic(topicId, studentId).stream()
            .map(PracticeQuestionMapper::toDomain)
            .toList();
    }

    @Override
    public List<PracticeQuestion> findUnseenByTopicAndCriterionAndRankRange(
            UUID topicId,
            UUID studentId,
            String criterion,
            int rankMin,
            int rankMax) {
        return repository
            .findUnseenByTopicAndCriterionAndRankRange(
                topicId, studentId, criterion, rankMin, rankMax, cooldownCutoff())
            .stream()
            .map(PracticeQuestionMapper::toDomain)
            .toList();
    }

    @Override
    public List<PracticeQuestion> findUnseenByIds(List<UUID> ids, UUID studentId) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return repository.findUnseenByIds(ids, studentId, cooldownCutoff()).stream()
            .map(PracticeQuestionMapper::toDomain)
            .toList();
    }

    // Gói 11 mục 3.3: câu chưa đạt band mục tiêu chỉ bị loại trong 24h kể từ lần gặp gần nhất.
    private static OffsetDateTime cooldownCutoff() {
        return OffsetDateTime.now().minusDays(1);
    }

    @Override
    public List<PracticeQuestion> findByIds(List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return repository.findAllById(ids).stream()
            .map(PracticeQuestionMapper::toDomain)
            .toList();
    }

    @Override
    public PracticeQuestion save(PracticeQuestion question) {
        return PracticeQuestionMapper.toDomain(repository.save(PracticeQuestionMapper.toJpa(question)));
    }

    @Override
    public void saveGenerated(PracticeQuestion question) {
        repository.insertGeneratedQuestion(
            question.id(),
            question.practiceTopicId(),
            question.questionText(),
            question.targetCriterionCode(),
            question.targetSubAttribute(),
            question.difficultyRank(),
            question.difficultyFeaturesJson(),
            question.evaluationGuideJson(),
            question.suggestedIdeasJson(),
            question.preparationTimeSeconds(),
            question.maxResponseSeconds(),
            question.maxFollowupSeconds(),
            question.vstepPart()
        );
    }

    @Override
    public void incrementUsageCount(UUID id) {
        repository.incrementUsageCount(id);
    }

    @Override
    public Optional<QuestionEvaluationInfo> findQuestionWithTopic(UUID questionId) {
        return repository.findQuestionWithTopic(questionId)
            .map(row -> new QuestionEvaluationInfo(
                row.getQuestionText(),
                row.getEvaluationGuideJson(),
                row.getMaxResponseSeconds(),
                row.getTopicName(),
                row.getTopicDescription()
            ));
    }
}
