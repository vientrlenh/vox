package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.domain.model.personalization.LearnerProfile;
import com.sep.vox.domain.model.personalization.QuizAnswer;
import com.sep.vox.domain.repository.DimensionInterestScoreRepository;
import com.sep.vox.domain.repository.InterestQuizItemRepository;
import com.sep.vox.domain.repository.LearnerProfileRepository;

@Service
@RequiredArgsConstructor
public class LearnerProfileCommandService {

    private final LearnerProfileRepository repository;
    private final InterestQuizItemRepository quizItemRepository;
    private final DimensionInterestScoreRepository dimensionScoreRepository;
    private final InterestQuizScorer interestQuizScorer;

    public void submitQuiz(UUID studentId, List<QuizAnswer> answers) {
        if (answers == null || answers.size() < 5 || answers.size() > 7) {
            throw new IllegalArgumentException(
                "The interest quiz must have 5 to 7 answers"
            );
        }
        Map<String, Integer> raw = new HashMap<String, Integer>();
        for (QuizAnswer answer : answers) {
            addAnswerVotes(raw, answer);
        }
        LearnerProfile saved = upsertProfile(studentId, null, null, Instant.now());
        dimensionScoreRepository.replaceScores(
            saved.getId(),
            interestQuizScorer.normalize(raw)
        );
    }

    private void addAnswerVotes(Map<String, Integer> raw, QuizAnswer answer) {
        if (answer.getMostStatementIndex()
                == answer.getLeastStatementIndex()) {
            throw new IllegalArgumentException(
                "The most-like and least-like choices must differ"
            );
        }
        List<String> dimensions = requireThreeDistinctDimensions(answer);
        raw.merge(
            dimensionAt(dimensions, answer.getMostStatementIndex()),
            1,
            (current, delta) -> current + delta
        );
        raw.merge(
            dimensionAt(dimensions, answer.getLeastStatementIndex()),
            -1,
            (current, delta) -> current + delta
        );
    }

    private List<String> requireThreeDistinctDimensions(QuizAnswer answer) {
        InterestQuizSeedItem item = quizItemRepository.findActiveQuizItem(answer.getItemId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Interest quiz item not found"
            ));
        List<String> dimensions = item.getDimensionPerStatement();
        if (dimensions.size() != 3
                || dimensions.stream().distinct().count() != 3) {
            throw new IllegalStateException(
                "The quiz item does not have exactly three distinct dimensions"
            );
        }
        return dimensions;
    }

    // REMOVED 2026-08-07: submitFlsa -- the foreign-language anxiety self-assessment scale (FLSA). No client
    // calls the mutation submitFlsaSelfReport, and no mechanism READS flsaScore to change behavior: it is only
    // written and then echoed back through GraphQL. Dropped the flsa_score/flsa_raw_answers_json columns from the
    // mapping (the DB columns stay, see the note in LearnerProfileJpaEntity).

    public void setGoal(UUID studentId, String goalType) {
        if (!"ASSESSMENT_PREP".equals(goalType)
                && !"ABILITY_IMPROVEMENT".equals(goalType)) {
            throw new IllegalArgumentException(
                "The practice goal is invalid"
            );
        }
        upsertProfile(studentId, goalType, null, null);
    }

    public void setAutoUpdate(UUID studentId, boolean enabled) {
        upsertProfile(studentId, null, enabled, null);
    }

   
    private LearnerProfile upsertProfile(
            UUID studentId,
            String goalType,
            Boolean autoUpdate,
            Instant quizCompletedAt) {
        LearnerProfile profile = repository.findCurrentForUpdate(studentId)
            .orElseGet(() -> LearnerProfile.forStudent(studentId));
        profile.applyChanges(goalType, autoUpdate, quizCompletedAt);
        return repository.save(profile);
    }

    private String dimensionAt(List<String> dimensions, int index) {
        if (index < 0 || index >= dimensions.size()) {
            throw new IllegalArgumentException(
                "The quiz choice index is invalid"
            );
        }
        return dimensions.get(index);
    }
}
