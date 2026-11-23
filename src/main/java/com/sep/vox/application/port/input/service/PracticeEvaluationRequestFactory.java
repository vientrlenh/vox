package com.sep.vox.application.port.input.service;

import com.sep.vox.application.query.dto.QuestionEvaluationInfo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.event.AssessmentAttemptEvaluationRequestedExternalEvent;
import com.sep.vox.application.event.PracticeAttemptEvaluationRequestedExternalEvent;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.query.dto.CriterionFrameworkInfo;
import com.sep.vox.application.query.repository.PracticeQuestionQueryRepository;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.domain.repository.PracticeResponseTurnRepository;
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
@RequiredArgsConstructor
public class PracticeEvaluationRequestFactory {

    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final PracticeResponseTurnRepository practiceResponseTurnRepository;
    private final PracticeQuestionQueryRepository practiceQuestionQueryRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public PracticeAttemptEvaluationRequestedExternalEvent build(
            UUID sessionId,
            UUID responseId,
            UUID questionId) {
        QuestionEvaluationInfo question = practiceQuestionQueryRepository.findQuestionWithTopic(questionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi luyện."));
        List<AssessmentAttemptEvaluationRequestedExternalEvent.TurnInput> turns = practiceResponseTurnRepository
            .findByPracticeResponseIdOrderByTurnOrder(responseId).stream()
            .map(record -> new AssessmentAttemptEvaluationRequestedExternalEvent.TurnInput(
                record.turnOrder(),
                record.turnType(),
                record.promptText(),
                record.audioUrl(),
                record.transcript(),
                record.durationSeconds()
            ))
            .toList();
        AssessmentAttemptEvaluationRequestedExternalEvent.Payload payload = new AssessmentAttemptEvaluationRequestedExternalEvent.Payload(
            question.questionText(),
            // Dạng bài THẬT (V13). Trước đây gửi "SPEAKING" -- không thuộc enum QuestionType
            // bên Python nên bị validator nuốt về null trong im lặng, khiến
            // get_expected_min_words rơi xuống nhánh mặc định và kỳ vọng ĐÚNG 10 TỪ cho mọi
            // câu, kể cả câu 45 giây. Có dạng bài rồi thì nó chạy đúng nhánh: DESCRIPTION 45s
            // kỳ vọng 35 từ chứ không phải 10.
            question.questionType(),
            null,
            turns.stream().mapToInt(turn -> turn.durationSeconds()).sum(),
            question.minResponseSeconds(),
            question.maxResponseSeconds(),
            null,
            question.topicName(),
            question.topicDescription(),
            evaluationGuide(question.evaluationGuideJson()),
            // mode: "unscripted" exactly like the assessment path. It used to send "practice" -- the wrong AXIS:
            // SpeakingMode on the Python side is scripted/unscripted (speaking from a given text or speaking
            // freely), not assessment/practice. The enum has no 'practice' so
            // SpeakingMode(request_payload.mode) threw ValueError, failed all 3 retries and then
            // ended with AssessmentAttemptEvaluationFailedEvent -- the practice grading chain never ran.
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

    private AssessmentAttemptEvaluationRequestedExternalEvent.EvaluationGuide evaluationGuide(String json) {
        Map<String, String> fields = jsonSerializationPort.toStringMap(json);
        return new AssessmentAttemptEvaluationRequestedExternalEvent.EvaluationGuide(
            fields.get("expected_content"),
            fields.get("key_points"),
            fields.get("acceptable_responses"),
            fields.get("off_topic_examples"),
            fields.get("scoring_hints"),
            fields.get("common_mistakes")
        );
    }

    private List<AssessmentAttemptEvaluationRequestedExternalEvent.CriterionFramework> criteriaFrameworks(UUID sessionId) {
        List<CriterionFrameworkInfo> rows = practiceSessionQueryRepository.findCriteriaFrameworks(sessionId);
        Map<UUID, List<CriterionFrameworkInfo>> grouped = new LinkedHashMap<UUID, List<CriterionFrameworkInfo>>();
        for (CriterionFrameworkInfo row : rows) {
            grouped.computeIfAbsent(row.getCriterionId(), ignored -> new ArrayList<>()).add(row);
        }
        return grouped.values().stream().map(group -> {
            CriterionFrameworkInfo first = group.get(0);
            List<AssessmentAttemptEvaluationRequestedExternalEvent.FrameworkBand> bands = group.stream()
                .map(row -> new AssessmentAttemptEvaluationRequestedExternalEvent.FrameworkBand(
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
            String criterionKey = first.getCriterionCode().trim().toLowerCase(Locale.ROOT);
            if ("discourse".equals(criterionKey)) {
                criterionKey = "coherence";
            }
            return new AssessmentAttemptEvaluationRequestedExternalEvent.CriterionFramework(
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
