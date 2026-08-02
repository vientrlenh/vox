package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.personalization.LearnerProfile;
import com.sep.vox.domain.model.personalization.QuizAnswer;
import com.sep.vox.domain.repository.personalization.DimensionInterestScoreRepository;
import com.sep.vox.domain.repository.personalization.InterestQuizItemRepository;
import com.sep.vox.domain.repository.personalization.LearnerProfileRepository;

@Service
public class LearnerProfileCommandService {

    private final LearnerProfileRepository repository;
    private final InterestQuizItemRepository quizItemRepository;
    private final DimensionInterestScoreRepository dimensionScoreRepository;
    private final JsonSerializationPort jsonSerialization;
    private final InterestQuizScorer interestQuizScorer;

    public LearnerProfileCommandService(
            LearnerProfileRepository repository,
            InterestQuizItemRepository quizItemRepository,
            DimensionInterestScoreRepository dimensionScoreRepository,
            JsonSerializationPort jsonSerialization,
            InterestQuizScorer interestQuizScorer) {
        this.repository = repository;
        this.quizItemRepository = quizItemRepository;
        this.dimensionScoreRepository = dimensionScoreRepository;
        this.jsonSerialization = jsonSerialization;
        this.interestQuizScorer = interestQuizScorer;
    }

    public void submitQuiz(UUID studentId, List<QuizAnswer> answers) {
        if (answers == null || answers.size() < 5 || answers.size() > 7) {
            throw new IllegalArgumentException(
                "Quiz sở thích phải có từ 5 đến 7 câu trả lời"
            );
        }
        var raw = new HashMap<String, Integer>();
        for (var answer : answers) {
            if (answer.getMostStatementIndex()
                    == answer.getLeastStatementIndex()) {
                throw new IllegalArgumentException(
                    "Lựa chọn giống nhất và ít giống nhất phải khác nhau"
                );
            }
            var item = quizItemRepository.findActiveQuizItem(answer.getItemId())
                .orElseThrow(() -> new NotFoundException(
                    "Không tìm thấy item quiz sở thích"
                ));
            var dimensions = item.getDimensionPerStatement();
            if (dimensions.size() != 3
                    || dimensions.stream().distinct().count() != 3) {
                throw new IllegalStateException(
                    "Item quiz không có đúng ba chiều khác nhau"
                );
            }
            raw.merge(
                dimensionAt(dimensions, answer.getMostStatementIndex()),
                1,
                Integer::sum
            );
            raw.merge(
                dimensionAt(dimensions, answer.getLeastStatementIndex()),
                -1,
                Integer::sum
            );
        }
        var saved = appendProfile(
            studentId,
            null,
            null,
            null,
            null,
            OffsetDateTime.now()
        );
        dimensionScoreRepository.replaceScores(
            saved.getId(),
            interestQuizScorer.normalize(raw)
        );
    }

    public void submitFlsa(UUID studentId, List<Integer> answers) {
        if (answers == null || answers.size() < 3 || answers.size() > 4
                || answers.stream().anyMatch(
                    value -> value == null || value < 1 || value > 5
                )) {
            throw new IllegalArgumentException(
                "FLAS phải có 3 đến 4 câu trả lời Likert từ 1 đến 5"
            );
        }
        var adjusted = new ArrayList<>(answers);
        adjusted.set(2, 6 - adjusted.get(2));
        var score = adjusted.stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElseThrow();
        appendProfile(
            studentId,
            null,
            BigDecimal.valueOf((score - 1.0) * 25.0)
                .setScale(2, RoundingMode.HALF_UP),
            jsonSerialization.toJson(answers),
            null,
            null
        );
    }

    public void setGoal(UUID studentId, String goalType) {
        if (!"EXAM_PREP".equals(goalType)
                && !"ABILITY_IMPROVEMENT".equals(goalType)) {
            throw new IllegalArgumentException(
                "Mục tiêu luyện tập không hợp lệ"
            );
        }
        appendProfile(studentId, goalType, null, null, null, null);
    }

    public void setAutoUpdate(UUID studentId, boolean enabled) {
        appendProfile(studentId, null, null, null, enabled, null);
    }

    private LearnerProfile appendProfile(
            UUID studentId,
            String goalType,
            BigDecimal flsaScore,
            String flsaRawAnswersJson,
            Boolean autoUpdate,
            OffsetDateTime quizCompletedAt) {
        var previous = repository.findCurrentForUpdate(studentId).orElse(null);
        var next = previous == null
            ? LearnerProfile.first(studentId).next(
                goalType,
                flsaScore,
                flsaRawAnswersJson,
                autoUpdate,
                quizCompletedAt
            )
            : previous.next(
                goalType,
                flsaScore,
                flsaRawAnswersJson,
                autoUpdate,
                quizCompletedAt
            );
        if (previous == null) {
            next = new LearnerProfile(
                null,
                next.getStudentId(),
                1,
                next.getGoalType(),
                next.getTargetExam(),
                next.getTargetDate(),
                next.getFlsaScore(),
                next.getFlsaRawAnswersJson(),
                next.isAutoUpdateInterest(),
                next.getQuizCompletedAt(),
                next.getRecordedAt()
            );
        }
        var saved = repository.save(next);
        if (previous != null) {
            dimensionScoreRepository.copyScores(previous.getId(), saved.getId());
        }
        return saved;
    }

    private String dimensionAt(List<String> dimensions, int index) {
        if (index < 0 || index >= dimensions.size()) {
            throw new IllegalArgumentException(
                "Chỉ số lựa chọn quiz không hợp lệ"
            );
        }
        return dimensions.get(index);
    }
}
