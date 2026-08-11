package com.sep.vox.application.port.input.usecase.practiceevaluation;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.practiceevaluation.RecordPracticeAttemptEvaluationCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.personalization.PracticeCriterionScoreRepository;
import com.sep.vox.domain.repository.personalization.PracticeItemEvaluationRepository;
import com.sep.vox.domain.repository.personalization.PracticeItemResponseRepository;
import com.sep.vox.domain.repository.personalization.PracticeSessionRepository;

@Service
public class RecordPracticeAttemptEvaluationUseCase implements IUseCase<RecordPracticeAttemptEvaluationCommand, Void> {

    private static final org.slf4j.Logger LOGGER =
        org.slf4j.LoggerFactory.getLogger(RecordPracticeAttemptEvaluationUseCase.class);

    private final PracticeItemEvaluationRepository evaluationRepository;
    private final PracticeCriterionScoreRepository criterionScoreRepository;
    private final PracticeItemResponseRepository responseRepository;
    private final PracticeSessionRepository practiceSessionRepository;

    public RecordPracticeAttemptEvaluationUseCase(
            PracticeItemEvaluationRepository evaluationRepository,
            PracticeCriterionScoreRepository criterionScoreRepository,
            PracticeItemResponseRepository responseRepository,
            PracticeSessionRepository practiceSessionRepository) {
        this.evaluationRepository = evaluationRepository;
        this.criterionScoreRepository = criterionScoreRepository;
        this.responseRepository = responseRepository;
        this.practiceSessionRepository = practiceSessionRepository;
    }

    @Override
    @Transactional
    public Void execute(RecordPracticeAttemptEvaluationCommand input) {
        var markedInvalid = !input.validForScoring();
        var itemScore = input.criteria().stream()
            .mapToDouble(criterion -> {
                var score = criterion.score();
                return score == null ? 0 : score;
            })
            .average()
            .orElse(0);
        var evaluatedAt = input.evaluatedAt() == null || input.evaluatedAt().isBlank()
            ? Instant.now()
            : Instant.parse(input.evaluatedAt());

        var evaluationId = evaluationRepository.upsert(
            input.practiceResponseId(),
            itemScore,
            markedInvalid,
            evaluatedAt
        );

        for (var criterion : input.criteria()) {
            var code = criterion.criterionCode() == null
                ? ""
                : criterion.criterionCode().trim().toUpperCase(Locale.ROOT);
            if (code.isBlank()) {
                LOGGER.warn(
                    "Bỏ qua một tiêu chí không có mã ở bản chấm luyện tập {}",
                    input.practiceResponseId()
                );
                continue;
            }
            var score = criterion.score();
            criterionScoreRepository.upsertByCode(
                evaluationId,
                code,
                score == null ? 0 : score
            );
        }

        refreshSessionScore(input.practiceResponseId());
        return null;
    }

    private void refreshSessionScore(UUID responseId) {
        try {
            var sessionId = responseRepository.findSessionIdByResponseId(responseId);
            if (sessionId == null) {
                return;
            }
            // MỘT câu UPDATE, không nạp entity rồi save lại -- xem chú thích dài ở
            // SpringDataPracticeSessionRepository.refreshOverallScore. Tóm tắt: save() ghi đè
            // cả dòng nên có thể xoá mất graded_seconds mà lượt nộp song song vừa cộng vào,
            // và nó giữ khoá dòng tới cuối transaction dài này khiến claim() phải xếp hàng.
            practiceSessionRepository.refreshOverallScore(sessionId);
        } catch (RuntimeException exception) {
            // Điểm đã chấm là dữ liệu chính; tổng hợp lại điểm phiên là phần phái sinh, lần
            // chấm sau bù được. Không để nó kéo đổ cả việc ghi bản chấm.
            LOGGER.warn("Không tính lại được điểm phiên từ response {}.", responseId, exception);
        }
    }

}
