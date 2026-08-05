package com.sep.vox.application.port.input.usecase.practiceevaluation;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.practiceevaluation.RecordPracticeAttemptEvaluationCommand;
import com.sep.vox.application.port.input.service.ConfidenceReviewCalculator;
import com.sep.vox.application.port.input.service.WeaknessObservationDerivationService;
import com.sep.vox.application.port.input.service.ConfidenceReviewCalculator.ConfidenceMode;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.personalization.WeaknessObservationSourceType;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.WeaknessObservationRepository;
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
    private final RubricCriterionRepository rubricCriterionRepository;
    private final ConfidenceReviewCalculator confidenceReviewCalculator;
    private final WeaknessObservationDerivationService derivationService;
    private final WeaknessObservationRepository weaknessObservationRepository;
    private final PracticeSessionRepository practiceSessionRepository;

    public RecordPracticeAttemptEvaluationUseCase(
            PracticeItemEvaluationRepository evaluationRepository,
            PracticeCriterionScoreRepository criterionScoreRepository,
            PracticeItemResponseRepository responseRepository,
            RubricCriterionRepository rubricCriterionRepository,
            ConfidenceReviewCalculator confidenceReviewCalculator,
            WeaknessObservationDerivationService derivationService,
            WeaknessObservationRepository weaknessObservationRepository,
            PracticeSessionRepository practiceSessionRepository) {
        this.evaluationRepository = evaluationRepository;
        this.criterionScoreRepository = criterionScoreRepository;
        this.responseRepository = responseRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.confidenceReviewCalculator = confidenceReviewCalculator;
        this.derivationService = derivationService;
        this.weaknessObservationRepository = weaknessObservationRepository;
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

        var rubricVersionId = responseRepository.findRubricVersionIdByResponseId(input.practiceResponseId());
        var rubricCriteriaByCode = rubricCriterionRepository.findByRubricVersionId(rubricVersionId).stream()
            .collect(Collectors.toMap(
                item -> item.getCode() == null ? "" : item.getCode().trim().toLowerCase(Locale.ROOT),
                Function.identity(),
                (left, right) -> left
            ));
        for (var criterion : input.criteria()) {
            var rubricCriterion = rubricCriteriaByCode.get(
                criterion.criterionCode() == null
                    ? ""
                    : criterion.criterionCode().trim().toLowerCase(Locale.ROOT)
            );
            if (rubricCriterion == null) {
                continue;
            }
            criterionScoreRepository.upsert(
                evaluationId,
                rubricCriterion.getId(),
                criterion.score() == null ? 0 : criterion.score(),
                blankToNull(criterion.matchedBandCode())
            );
        }

        storeWeaknessObservations(input, evaluationId, markedInvalid, evaluatedAt, rubricCriteriaByCode);
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

    /**
     * Biến kết quả chấm thành các quan sát điểm yếu -- mắt xích đã thiếu khiến hồ sơ điểm yếu
     * luôn trống dù học sinh luyện đều.
     *
     * Trước đây {@code WeaknessObservationDerivationService.derive} không có một call site nào
     * trong cả repo: nó viết cho nhánh thi nhưng nhánh thi cũng chưa gọi. Kết quả là chuỗi dừng
     * ngay sau khi ghi điểm -- có {@code practice_criterion_score} nhưng không ai đọc để suy ra
     * nhãn điểm yếu, nên {@code weakness_observation} rỗng, kéo theo snapshot rỗng và cả ba ô
     * đếm trên màn hồ sơ đều bằng 0.
     *
     * Không để hỏng cả việc ghi điểm nếu bước này lỗi: điểm đã chấm là dữ liệu chính, còn suy
     * điểm yếu là phần làm giàu thêm -- mất nó thì lần chấm sau bù lại được.
     */
    private void storeWeaknessObservations(
            RecordPracticeAttemptEvaluationCommand input,
            UUID evaluationId,
            boolean markedInvalid,
            Instant evaluatedAt,
            Map<String, RubricCriterion> rubricCriteriaByCode) {
        try {
            var studentId = responseRepository.findStudentIdByResponseId(input.practiceResponseId());
            if (studentId == null) {
                return;
            }
            var observations = derivationService.derive(
                studentId,
                evaluationId,
                WeaknessObservationSourceType.PRACTICE,
                evaluatedAt,
                markedInvalid,
                false,
                input.rawCriteria(),
                rubricCriteriaByCode,
                input.turns(),
                input.signals()
            );
            for (var observation : observations) {
                // Chấm lại cùng một câu không được đẻ thêm bản trùng -- khoá tự nhiên là
                // (evaluation, tiêu chí, nhãn, bằng chứng).
                if (weaknessObservationRepository.existsForKey(
                        observation.getSourceEvaluationId(),
                        observation.getFrameworkCriterionId(),
                        observation.getSubAttribute(),
                        observation.getEvidenceSpan())) {
                    continue;
                }
                weaknessObservationRepository.save(observation);
            }
            LOGGER.debug(
                "Đã ghi {} quan sát điểm yếu từ phiên luyện, evaluation {}.",
                observations.size(),
                evaluationId
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Không suy được quan sát điểm yếu cho evaluation {}.", evaluationId, exception);
        }
    }
}
