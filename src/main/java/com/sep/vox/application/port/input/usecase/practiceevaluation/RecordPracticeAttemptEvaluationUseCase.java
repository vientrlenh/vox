package com.sep.vox.application.port.input.usecase.practiceevaluation;

import com.sep.vox.application.port.input.command.practiceevaluation.PracticeCriterionScoreInput;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.practiceevaluation.RecordPracticeAttemptEvaluationCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.PracticeCriterionScoreRepository;
import com.sep.vox.domain.repository.PracticeItemEvaluationRepository;
import com.sep.vox.domain.repository.PracticeItemResponseRepository;
import com.sep.vox.domain.repository.PracticeSessionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecordPracticeAttemptEvaluationUseCase implements IUseCase<RecordPracticeAttemptEvaluationCommand, Void> {

    private final PracticeItemEvaluationRepository evaluationRepository;
    private final PracticeCriterionScoreRepository criterionScoreRepository;
    private final PracticeItemResponseRepository responseRepository;
    private final PracticeSessionRepository practiceSessionRepository;

    @Override
    @Transactional
    public Void execute(RecordPracticeAttemptEvaluationCommand input) {
        UUID evaluationId = upsertItemEvaluation(input);
        upsertCriterionScores(evaluationId, input);

        // Same transaction as the evaluation upsert above, so the status and the evaluation can NEVER
        // disagree. With two separate writes there would be moments when a graded row still carries
        // the GRADING label, i.e. the data would lie to anyone reading the table.
        responseRepository.markGraded(input.practiceResponseId());
        refreshSessionScore(input.practiceResponseId());
        return null;
    }

    private UUID upsertItemEvaluation(RecordPracticeAttemptEvaluationCommand input) {
        boolean markedInvalid = !input.validForScoring();
        double itemScore = input.criteria().stream()
            .mapToDouble(criterion -> {
                Double score = criterion.score();
                return score == null ? 0 : score;
            })
            .average()
            .orElse(0);
        Instant evaluatedAt = input.evaluatedAt() == null || input.evaluatedAt().isBlank()
            ? Instant.now()
            : Instant.parse(input.evaluatedAt());

        return evaluationRepository.upsert(
            input.practiceResponseId(),
            itemScore,
            markedInvalid,
            evaluatedAt
        );
    }

    private void upsertCriterionScores(UUID evaluationId, RecordPracticeAttemptEvaluationCommand input) {
        for (PracticeCriterionScoreInput criterion : input.criteria()) {
            String code = criterion.criterionCode() == null
                ? ""
                : criterion.criterionCode().trim().toUpperCase(Locale.ROOT);
            if (code.isBlank()) {
                log.warn(
                    "Bỏ qua một tiêu chí không có mã ở bản chấm luyện tập {}",
                    input.practiceResponseId()
                );
                continue;
            }
            Double score = criterion.score();
            criterionScoreRepository.upsertByCode(
                evaluationId,
                code,
                score == null ? 0 : score
            );
        }
    }

    private void refreshSessionScore(UUID responseId) {
        try {
            UUID sessionId = responseRepository.findSessionIdByResponseId(responseId);
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
            log.warn("Không tính lại được điểm phiên từ response {}.", responseId, exception);
        }
    }

}
