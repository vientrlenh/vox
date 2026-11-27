package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.repository.PracticeQuestionRepository;
import com.sep.vox.infrastructure.persistence.mapper.personalization.PracticeQuestionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticeQuestionRepository;

@Repository
@RequiredArgsConstructor
public class PracticeQuestionRepositoryImpl implements PracticeQuestionRepository {

    private final SpringDataPracticeQuestionRepository repository;

    @Override
    public Optional<PracticeQuestion> findById(UUID id) {
        return repository.findById(id).map(PracticeQuestionMapper::toDomain);
    }

    @Override
    public List<PracticeQuestion> findUnseenByTopicAndCriterionAndRankRange(
            UUID topicId,
            UUID studentId,
            String criterion,
            String tense,
            int rankMin,
            int rankMax) {
        return repository
            .findUnseenByTopicAndCriterionAndRankRange(
                topicId, studentId, criterion, tense, rankMin, rankMax)
            .stream()
            .map(PracticeQuestionMapper::toDomain)
            .toList();
    }

    // REMOVED 2026-08-06: cooldownCutoff (plan 11 section 3.3 -- "a question below the target band is only
    // excluded for 24h"). The new rule has no exception for an already-seen question, so there is no
    // time threshold left to compute.

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

    /**
     * SHORT transaction, placed exactly here and not in the upper layer.
     *
     * <p>insertGeneratedQuestion is @Modifying, so Hibernate requires an open transaction,
     * otherwise it throws TransactionRequiredException. It used to borrow the
     * transaction of BuildPracticePaperUseCase; when that transaction was removed (so as not to hold a
     * HikariCP connection for the 10-40 seconds of the LLM call) this write lost its support and broke.
     *
     * <p>The adapter layer is the right place: it wraps exactly one INSERT that runs AFTER the LLM
     * call has returned, so it does not extend the time the connection is held.
     */
    @Override
    @Transactional
    public void saveGenerated(PracticeQuestion question) {
        repository.insertGeneratedQuestion(
            question.getId(),
            question.getPracticeTopicId(),
            question.getQuestionText(),
            question.getTargetCriterionCode(),
            question.getTargetSubAttribute(),
            question.getTargetTense(),
            question.getDifficultyRank(),
            question.getDifficultyFeaturesJson(),
            question.getEvaluationGuideJson(),
            question.getSuggestedIdeasJson(),
            question.getQuestionType(),
            question.getMaxResponseSeconds(),
            question.getMinResponseSeconds(),
            question.getVstepPart()
        );
    }

    /** Also @Modifying like saveGenerated -- needs its own transaction because the calling layer
     * (resolveNextQuestion) intentionally runs outside a transaction. It has not failed yet only because the flow dies in
     * saveGenerated before reaching this method. */
    @Override
    @Transactional
    public List<UUID> findPermanentlyExhaustedIds(UUID topicId, UUID studentId) {
        return repository.findPermanentlyExhaustedIds(topicId, studentId);
    }

    @Override
    @Transactional
    public void incrementUsageCount(UUID id) {
        repository.incrementUsageCount(id);
    }

    @Override
    @Transactional
    public void decrementUsageCount(UUID id) {
        repository.decrementUsageCount(id);
    }
}
