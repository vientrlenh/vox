package com.sep.vox.application.port.input.usecase.practicesession;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.ExamAttemptEvaluationRequestedExternalEvent;
import com.sep.vox.application.event.PracticeAttemptEvaluationRequestedExternalEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.QuotaExceededException;
import com.sep.vox.application.mapper.practicesession.PracticeSessionResponseMapper;
import com.sep.vox.application.port.input.command.ConsumeQuotaCommand;
import com.sep.vox.application.port.input.command.SubmitPracticeTurnCommand;
import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ConsumeQuotaUseCase;
import com.sep.vox.application.port.output.ExternalEventPublisherPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.CriterionFrameworkInfo;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.SubmitTurnResult;
import com.sep.vox.domain.dto.personalization.SubmitTurnResultDto;
import com.sep.vox.domain.dto.personalization.TurnCorrectionDto;
import com.sep.vox.domain.model.personalization.SubmitPracticeTurn;
import com.sep.vox.domain.model.personalization.TurnCorrectionSubmission;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.personalization.PracticeItemResponseRepository;
import com.sep.vox.domain.repository.personalization.PracticeQuestionRepository;
import com.sep.vox.domain.repository.personalization.PracticeResponseTurnRepository;
import com.sep.vox.domain.repository.personalization.PracticeSessionRepository;
import com.sep.vox.domain.repository.personalization.TurnCorrectionRepository;

@Service
public class SubmitPracticeTurnUseCase implements IUseCase<SubmitPracticeTurnCommand, SubmitTurnResult> {

    private static final org.slf4j.Logger LOGGER =
        org.slf4j.LoggerFactory.getLogger(SubmitPracticeTurnUseCase.class);

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final PracticeItemResponseRepository practiceItemResponseRepository;
    private final PracticeResponseTurnRepository practiceResponseTurnRepository;
    private final TurnCorrectionRepository turnCorrectionRepository;
    private final PracticeQuestionRepository practiceQuestionRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final PracticeTopicOfferEnrichmentService enrichmentService;
    private final ConsumeQuotaUseCase consumeQuotaUseCase;
    private final ExternalEventPublisherPort eventPublisher;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserContextPort userContextPort;

    public SubmitPracticeTurnUseCase(
            PracticeSessionRepository practiceSessionRepository,
            PracticeSessionQueryRepository practiceSessionQueryRepository,
            PracticeItemResponseRepository practiceItemResponseRepository,
            PracticeResponseTurnRepository practiceResponseTurnRepository,
            TurnCorrectionRepository turnCorrectionRepository,
            PracticeQuestionRepository practiceQuestionRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            PracticeTopicOfferEnrichmentService enrichmentService,
            ConsumeQuotaUseCase consumeQuotaUseCase,
            ExternalEventPublisherPort eventPublisher,
            JsonSerializationPort jsonSerializationPort,
            UserContextPort userContextPort) {
        this.practiceSessionRepository = practiceSessionRepository;
        this.practiceSessionQueryRepository = practiceSessionQueryRepository;
        this.practiceItemResponseRepository = practiceItemResponseRepository;
        this.practiceResponseTurnRepository = practiceResponseTurnRepository;
        this.turnCorrectionRepository = turnCorrectionRepository;
        this.practiceQuestionRepository = practiceQuestionRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.enrichmentService = enrichmentService;
        this.consumeQuotaUseCase = consumeQuotaUseCase;
        this.eventPublisher = eventPublisher;
        this.jsonSerializationPort = jsonSerializationPort;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SubmitTurnResult execute(SubmitPracticeTurnCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        return execute(studentId, input.turn());
    }

    /**
     * Cho phép gọi với studentId biết trước (không qua UserContextPort) -- dùng bởi endpoint nội
     * bộ /internal/practice-sessions/{id}/turns (Python gọi, không có SecurityContext đăng nhập).
     */
    @Transactional
    public SubmitTurnResult execute(UUID studentId, SubmitPracticeTurn turn) {
        requireOwnedInProgress(turn.getSessionId(), studentId);
        var responseId = practiceItemResponseRepository.upsertResponse(
            turn.getSessionId(),
            turn.getQuestionId(),
            turn.getAudioUrl(),
            turn.getTranscript()
        );
        var turnId = practiceResponseTurnRepository.save(
            responseId,
            turn.getTurnOrder(),
            turn.getTurnType(),
            turn.getPromptText(),
            turn.getAudioUrl(),
            turn.getTranscript(),
            Math.max(0, turn.getDurationSeconds()),
            turn.getWordFeedbackJson(),
            turn.getTurnScore()
        );
        var corrections = storeCorrections(turnId, turn.getCorrections());
        // Nạp phiên KỂ CẢ khi lượt này im lặng: client vẫn phải thấy đúng tổng "đã nói" trên
        // thanh tiến độ, chứ không phải một ô trống mỗi lần học sinh bấm mic rồi không nói gì.
        var session = practiceSessionRepository.findById(turn.getSessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên luyện."));
        var spokenSeconds = session.getGradedSeconds();
        var quotaExhausted = false;
        if (turn.getDurationSeconds() > 0) {
            try {
                consumeQuotaUseCase.execute(new ConsumeQuotaCommand(
                    activeSubscriptionId(studentId),
                    turn.getSessionId(),
                    QuotaType.PRACTICE,
                    turn.getDurationSeconds(),
                    studentId
                ));
            } catch (QuotaExceededException exception) {
                // KHÔNG để lỗi thoát ra: method này @Transactional, nên ném lên là rollback
                // luôn cả response/turn/corrections vừa lưu ở trên -- học sinh nói xong mà
                // lượt biến mất, không được chấm, không vào hồ sơ điểm yếu.
                //
                // Đã nói thì phải được ghi. Hết hạn mức chỉ có nghĩa là phiên dừng SAU lượt
                // này, nên báo bằng cờ để tầng gọi đóng phiên một cách tử tế.
                LOGGER.info(
                    "Hết hạn mức PRACTICE ở phiên {} -- vẫn ghi lượt {} rồi đóng phiên.",
                    turn.getSessionId(),
                    turn.getTurnOrder()
                );
                quotaExhausted = true;
            }
            spokenSeconds += turn.getDurationSeconds();
            practiceSessionRepository.save(
                session.withGradedSecondsAndHeartbeat(spokenSeconds, Instant.now())
            );
        }
        var result = new SubmitTurnResultDto(
            responseId,
            turnId,
            practiceResponseTurnRepository.findRemainingQuestionSeconds(turn.getSessionId(), turn.getQuestionId()),
            turn.isQuestionComplete(),
            corrections,
            quotaExhausted,
            spokenSeconds,
            enrichmentService.sessionBudgetSecondsForStudent(studentId)
        );
        if (result.evaluationQueued()) {
            eventPublisher.publish(buildEvaluationRequestEvent(
                turn.getSessionId(),
                result.responseId(),
                turn.getQuestionId()
            ));
        }
        return PracticeSessionResponseMapper.toResponse(result);
    }

    private void requireOwnedInProgress(UUID sessionId, UUID studentId) {
        if (!practiceSessionRepository.existsByIdAndStudentIdAndStatus(sessionId, studentId, "IN_PROGRESS")) {
            throw new NotFoundException("Phiên luyện không còn hoạt động.");
        }
    }

    private UUID activeSubscriptionId(UUID studentId) {
        return schoolSubscriptionRepository.findActiveSubscriptionIdForUser(studentId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đang hoạt động."));
    }

    /**
     * Ngưỡng tin cậy chỉ áp cho những dòng sửa do LLM PHÁN ĐOÁN (ngữ pháp, dùng từ) -- ở đó
     * điểm tin cậy nói lên model chắc tới đâu. Dòng phát âm thì khác hẳn: nó là SỐ ĐO của
     * Azure (điểm chính xác từng âm vị), không tự xưng độ tin cậy nào cả nên luôn về 0.0 và bị
     * ngưỡng này loại sạch. Kết quả là thẻ sửa lỗi lúc đang nói thì hiện lỗi phát âm (đi thẳng
     * qua WebSocket), nhưng mở lại sau buổi thì chúng biến mất -- chưa từng được ghi xuống.
     */
    private static final String MEASURED_CATEGORY = "pronunciation";
    private static final double MIN_JUDGED_CONFIDENCE = 0.8;

    /**
     * Trần số dòng lưu cho MỘT lượt. Rộng hơn con số 3 cũ vì trần này chỉ chi phối bản LƯU
     * (để xem lại sau buổi), không chi phối thẻ hiện lúc đang nói -- thẻ đó do Python đẩy
     * thẳng qua WebSocket, không đi qua đây. Xem lại thì càng đủ càng tốt; giữ trần chỉ để
     * chặn ghi vô hạn.
     */
    private static final int MAX_STORED_CORRECTIONS = 8;

    private List<TurnCorrectionDto> storeCorrections(UUID turnId, List<TurnCorrectionSubmission> inputs) {
        var result = new ArrayList<TurnCorrectionDto>();
        for (var correction : inputs == null ? List.<TurnCorrectionSubmission>of() : inputs) {
            var measured = MEASURED_CATEGORY.equalsIgnoreCase(correction.getCategory());
            if (result.size() >= MAX_STORED_CORRECTIONS
                    || (!measured && correction.getConfidence() < MIN_JUDGED_CONFIDENCE)) {
                continue;
            }
            turnCorrectionRepository.save(
                turnId,
                correction.getCategory(),
                correction.getOriginalText(),
                correction.getCorrectedText(),
                correction.getExplanation(),
                correction.getCorrectAudioUrl()
            );
            result.add(new TurnCorrectionDto(
                correction.getCategory(),
                correction.getOriginalText(),
                correction.getCorrectedText(),
                correction.getExplanation(),
                correction.getCorrectAudioUrl()
            ));
        }
        return result;
    }

    private PracticeAttemptEvaluationRequestedExternalEvent buildEvaluationRequestEvent(
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
            grouped.computeIfAbsent(row.getRubricCriterionId(), ignored -> new ArrayList<>()).add(row);
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
            var criterionKey = first.getRubricCode().trim().toLowerCase(Locale.ROOT);
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
                true,
                first.getWeight(),
                first.getMinScore(),
                first.getMaxScore(),
                bands
            );
        }).toList();
    }
}
