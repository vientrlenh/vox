package com.sep.vox.application.port.input.usecase.practiceevaluation;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.practiceevaluation.RecordPracticeAttemptEvaluationCommand;
import com.sep.vox.application.port.input.service.ConfidenceReviewCalculator;
import com.sep.vox.application.port.input.service.ConfidenceReviewCalculator.ConfidenceMode;
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
    private final ConfidenceReviewCalculator confidenceReviewCalculator;
    private final PracticeSessionRepository practiceSessionRepository;

    public RecordPracticeAttemptEvaluationUseCase(
            PracticeItemEvaluationRepository evaluationRepository,
            PracticeCriterionScoreRepository criterionScoreRepository,
            PracticeItemResponseRepository responseRepository,
            ConfidenceReviewCalculator confidenceReviewCalculator,
            PracticeSessionRepository practiceSessionRepository) {
        this.evaluationRepository = evaluationRepository;
        this.criterionScoreRepository = criterionScoreRepository;
        this.responseRepository = responseRepository;
        this.confidenceReviewCalculator = confidenceReviewCalculator;
        this.practiceSessionRepository = practiceSessionRepository;
    }

    @Override
    @Transactional
    public Void execute(RecordPracticeAttemptEvaluationCommand input) {
        var confidenceDecision = confidenceReviewCalculator.compute(
            input.confidenceCase(),
            input.audioQuality(),
            ConfidenceMode.PRACTICE,
            input.codeSwitchingRatio(),
            input.wordCount() < 35
        );
        var markedInvalid = !input.validForScoring() || confidenceDecision.requiresHumanReview();
        var itemScore = input.criteria().stream()
            .mapToDouble(criterion -> criterion.score() == null ? 0 : criterion.score())
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

        // Ghi thẳng theo MÃ tiêu chí (V13). Bản cũ tra rubric_criterions rồi `continue` khi
        // không khớp -- điểm biến mất không dấu vết, đúng cách bug 2026-08-06 ở đường thi ẩn
        // được. Giờ chỉ bỏ khi mã rỗng, và có log để còn lần ra.
        //
        // Mã lạ (ví dụ "tc01" do Python gửi nhầm khoá) VẪN được ghi: thà có một dòng sai mã
        // nhìn thấy được còn hơn mất cả bản chấm trong im lặng.
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
            criterionScoreRepository.upsertByCode(
                evaluationId,
                code,
                criterion.score() == null ? 0 : criterion.score()
            );
        }

        refreshSessionScore(input.practiceResponseId());
        return null;
    }

    /**
     * Tính lại điểm phiên ngay khi một bản chấm vừa về.
     *
     * Trước đây điểm phiên chỉ được tính ĐÚNG MỘT LẦN, tại thời điểm bấm kết thúc
     * ({@code EndPracticeSessionUseCase}), rồi không bao giờ đụng lại. Nhưng chấm là bất đồng
     * bộ và về SAU đó vài chục giây -- đo trên dữ liệu thật: ended_at 11:03:25, evaluated_at
     * 11:04:10. Nên con số chốt lúc kết thúc gần như luôn được tính trên một tập rỗng, và học
     * sinh thấy 0 điểm dù đã làm bài đầy đủ. Comment cũ ở đó khẳng định "overall_score tính
     * lại từ đó" -- không có gì tính lại cả.
     *
     * Chạy cả khi phiên còn đang diễn ra cũng không sao: điểm chỉ đơn giản luôn phản ánh đúng
     * những câu đã chấm xong tại thời điểm đó.
     */
    /**
     * Chuỗi RỖNG phải thành NULL trước khi xuống DB.
     *
     * findEstimatedResultBandOrder đã có sẵn chốt {@code matched_band_code IS NOT NULL} -- người
     * viết truy vấn đã lường trước chuyện thiếu mã bậc. Nhưng chuỗi rỗng LỌT QUA chốt đó rồi mới
     * chết ở phép nối {@code band.code = matched_band_code}: bản ghi biến mất khỏi phép ước
     * lượng, không lỗi, không log, chỉ là mẫu nhỏ đi. Đo trên dữ liệu thật: 5 dòng có mã, chỉ 3
     * dòng qua được phép nối, mà ngưỡng {@code total >= 5} lại đếm SAU khi nối -- nên phép ước
     * lượng bậc chưa từng chạy một lần nào.
     *
     * NULL hoá ở đây để chốt đó làm đúng việc nó được viết ra để làm.
     */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
