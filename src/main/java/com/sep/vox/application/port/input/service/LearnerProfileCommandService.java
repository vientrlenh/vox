package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
        var saved = upsertProfile(studentId, null, null, Instant.now());
        dimensionScoreRepository.replaceScores(
            saved.getId(),
            interestQuizScorer.normalize(raw)
        );
    }

    // GỠ 2026-08-07: submitFlsa -- thang tự đánh giá lo lắng ngoại ngữ (FLSA). Không client nào
    // gọi mutation submitFlsaSelfReport, và không cơ chế nào ĐỌC flsaScore để đổi hành vi: nó chỉ
    // được ghi rồi dội ngược lại qua GraphQL. Xoá cả cột flsa_score/flsa_raw_answers_json khỏi
    // ánh xạ (cột trong DB giữ nguyên, xem chú thích ở LearnerProfileJpaEntity).

    public void setGoal(UUID studentId, String goalType) {
        if (!"EXAM_PREP".equals(goalType)
                && !"ABILITY_IMPROVEMENT".equals(goalType)) {
            throw new IllegalArgumentException(
                "Mục tiêu luyện tập không hợp lệ"
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
        var profile = repository.findCurrentForUpdate(studentId)
            .orElseGet(() -> LearnerProfile.forStudent(studentId));
        profile.applyChanges(goalType, autoUpdate, quizCompletedAt);
        return repository.save(profile);
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
