package com.sep.vox.application.port.input.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.event.ExamAttemptEvaluationRequestedExternalEvent;
import com.sep.vox.application.event.PracticeAttemptEvaluationRequestedExternalEvent;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.query.dto.CriterionFrameworkInfo;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.domain.repository.personalization.PracticeQuestionRepository;
import com.sep.vox.domain.repository.personalization.PracticeResponseTurnRepository;
import com.sep.vox.application.exception.NotFoundException;

/**
 * Dựng yêu cầu chấm cho MỘT câu của phiên luyện.
 *
 * Tách khỏi {@code SubmitPracticeTurnUseCase} vì giờ có HAI nơi cần bắn yêu cầu chấm, không
 * còn một: lượt cuối của một câu (đường bình thường) và lúc đóng phiên với những câu học sinh
 * bỏ dở (xem {@link PracticeGradingFlushService}). Để nguyên trong use case rồi gọi chéo sang
 * là buộc hai đường phải đi qua cùng một @Transactional -- mà đường xả chấm chạy trong
 * transaction đóng phiên, hoàn toàn khác ngữ cảnh.
 */
@Service
public class PracticeEvaluationRequestFactory {

    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final PracticeResponseTurnRepository practiceResponseTurnRepository;
    private final PracticeQuestionRepository practiceQuestionRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public PracticeEvaluationRequestFactory(
            PracticeSessionQueryRepository practiceSessionQueryRepository,
            PracticeResponseTurnRepository practiceResponseTurnRepository,
            PracticeQuestionRepository practiceQuestionRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.practiceSessionQueryRepository = practiceSessionQueryRepository;
        this.practiceResponseTurnRepository = practiceResponseTurnRepository;
        this.practiceQuestionRepository = practiceQuestionRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    public PracticeAttemptEvaluationRequestedExternalEvent build(
            UUID sessionId,
            UUID responseId,
            UUID questionId) {
        var question = practiceQuestionRepository.findQuestionWithTopic(questionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi luyện."));
        var turns = practiceResponseTurnRepository
            .findByPracticeResponseIdOrderByTurnOrder(responseId).stream()
            .map(record -> new ExamAttemptEvaluationRequestedExternalEvent.TurnInput(
                record.turnOrder(),
                record.turnType(),
                record.promptText(),
                record.audioUrl(),
                record.transcript(),
                record.durationSeconds()
            ))
            .toList();
        var payload = new ExamAttemptEvaluationRequestedExternalEvent.Payload(
            question.questionText(),
            // Dạng bài THẬT (V13). Trước đây gửi "SPEAKING" -- không thuộc enum QuestionType
            // bên Python nên bị validator nuốt về null trong im lặng, khiến
            // get_expected_min_words rơi xuống nhánh mặc định và kỳ vọng ĐÚNG 10 TỪ cho mọi
            // câu, kể cả câu 45 giây. Có dạng bài rồi thì nó chạy đúng nhánh: DESCRIPTION 45s
            // kỳ vọng 35 từ chứ không phải 10.
            question.questionType(),
            null,
            turns.stream().mapToInt(
                ExamAttemptEvaluationRequestedExternalEvent.TurnInput::durationSeconds
            ).sum(),
            question.minResponseSeconds(),
            question.maxResponseSeconds(),
            null,
            question.topicName(),
            question.topicDescription(),
            evaluationGuide(question.evaluationGuideJson()),
            // mode: "unscripted" y như đường đề thi. Trước đây gửi "practice" -- nhầm TRỤC:
            // SpeakingMode bên Python là scripted/unscripted (nói theo văn bản có sẵn hay nói
            // tự do), không phải thi/luyện. Enum không có 'practice' nên
            // SpeakingMode(request_payload.mode) ném ValueError, hỏng cả 3 lần retry rồi kết
            // thúc bằng ExamAttemptEvaluationFailedEvent -- chuỗi chấm bài luyện chưa từng chạy.
            "unscripted",
            null,
            "en-US",
            criteriaFrameworks(sessionId),
            turns
        );
        return new PracticeAttemptEvaluationRequestedExternalEvent(
            sessionId.toString(),
            responseId.toString(),
            questionId.toString(),
            payload
        );
    }

    private ExamAttemptEvaluationRequestedExternalEvent.EvaluationGuide evaluationGuide(String json) {
        var fields = jsonSerializationPort.toStringMap(json);
        return new ExamAttemptEvaluationRequestedExternalEvent.EvaluationGuide(
            fields.get("expected_content"),
            fields.get("key_points"),
            fields.get("acceptable_responses"),
            fields.get("off_topic_examples"),
            fields.get("scoring_hints"),
            fields.get("common_mistakes")
        );
    }

    private List<ExamAttemptEvaluationRequestedExternalEvent.CriterionFramework> criteriaFrameworks(UUID sessionId) {
        var rows = practiceSessionQueryRepository.findCriteriaFrameworks(sessionId);
        var grouped = new LinkedHashMap<UUID, List<CriterionFrameworkInfo>>();
        for (var row : rows) {
            grouped.computeIfAbsent(row.getCriterionId(), ignored -> new ArrayList<>()).add(row);
        }
        return grouped.values().stream().map(group -> {
            var first = group.get(0);
            var bands = group.stream()
                .map(row -> new ExamAttemptEvaluationRequestedExternalEvent.FrameworkBand(
                    row.getBandCode(),
                    row.getBandLabel(),
                    row.getMinScore(),
                    row.getMaxScore(),
                    row.getDescriptor(),
                    List.of(),
                    List.of(),
                    row.getBandOrder()
                ))
                .toList();
            var criterionKey = first.getCriterionCode().trim().toLowerCase(Locale.ROOT);
            if ("discourse".equals(criterionKey)) {
                criterionKey = "coherence";
            }
            return new ExamAttemptEvaluationRequestedExternalEvent.CriterionFramework(
                criterionKey,
                first.getFrameworkCode(),
                first.getFrameworkName(),
                first.getFrameworkDescription(),
                first.getTargetBandId(),
                first.getTargetBandCode(),
                first.getTargetBandLabel(),
                // targetBandOnly = true: luyện tập chỉ gửi ĐÚNG bậc học sinh chọn, và Python
                // đã có sẵn nhánh cho nó -- "assign a score within {range} for how fully this
                // answer satisfies the TARGET band's descriptor".
                true,
                // Trọng số: null. Python không đọc trường này (grep agents/src ra 0 chỗ), và
                // điểm câu là trung bình cộng 5 tiêu chí ở RecordPracticeAttemptEvaluationUseCase
                // -- tức 20% mỗi tiêu chí, đã đúng sẵn không cần trọng số nào.
                null,
                first.getMinScore(),
                first.getMaxScore(),
                bands
            );
        }).toList();
    }
}
