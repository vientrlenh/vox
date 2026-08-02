package com.sep.vox.application.port.input.usecase.practicesession;

import java.time.OffsetDateTime;
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
import com.sep.vox.application.mapper.practicesession.PracticeSessionResponseMapper;
import com.sep.vox.application.port.input.command.ConsumeQuotaCommand;
import com.sep.vox.application.port.input.command.SubmitPracticeTurnCommand;
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

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final PracticeItemResponseRepository practiceItemResponseRepository;
    private final PracticeResponseTurnRepository practiceResponseTurnRepository;
    private final TurnCorrectionRepository turnCorrectionRepository;
    private final PracticeQuestionRepository practiceQuestionRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
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
        requireOwnedInProgress(turn.sessionId(), studentId);
        var responseId = practiceItemResponseRepository.upsertResponse(
            turn.sessionId(),
            turn.questionId(),
            turn.audioUrl(),
            turn.transcript()
        );
        var turnId = practiceResponseTurnRepository.save(
            responseId,
            turn.turnOrder(),
            turn.turnType(),
            turn.promptText(),
            turn.audioUrl(),
            turn.transcript(),
            Math.max(0, turn.durationSeconds()),
            turn.wordFeedbackJson(),
            turn.turnScore()
        );
        var corrections = storeCorrections(turnId, turn.corrections());
        if (turn.durationSeconds() > 0) {
            consumeQuotaUseCase.execute(new ConsumeQuotaCommand(
                activeSubscriptionId(studentId),
                turn.sessionId(),
                QuotaType.PRACTICE,
                turn.durationSeconds(),
                studentId
            ));
            var session = practiceSessionRepository.findById(turn.sessionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên luyện."));
            practiceSessionRepository.save(session.withGradedSecondsAndHeartbeat(
                session.gradedSeconds() + turn.durationSeconds(),
                OffsetDateTime.now()
            ));
        }
        var result = new SubmitTurnResultDto(
            responseId,
            turnId,
            practiceResponseTurnRepository.findRemainingQuestionSeconds(turn.sessionId(), turn.questionId()),
            turn.questionComplete(),
            corrections
        );
        if (result.evaluationQueued()) {
            eventPublisher.publish(buildEvaluationRequestEvent(
                turn.sessionId(),
                result.responseId(),
                turn.questionId()
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

    private List<TurnCorrectionDto> storeCorrections(UUID turnId, List<TurnCorrectionSubmission> inputs) {
        var result = new ArrayList<TurnCorrectionDto>();
        for (var correction : inputs == null ? List.<TurnCorrectionSubmission>of() : inputs) {
            if (correction.confidence() < 0.8 || result.size() >= 3) {
                continue;
            }
            turnCorrectionRepository.save(
                turnId,
                correction.category(),
                correction.originalText(),
                correction.correctedText(),
                correction.explanation(),
                correction.correctAudioUrl()
            );
            result.add(new TurnCorrectionDto(
                correction.category(),
                correction.originalText(),
                correction.correctedText(),
                correction.explanation(),
                correction.correctAudioUrl()
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
            "SPEAKING",
            null,
            turns.stream().mapToInt(
                ExamAttemptEvaluationRequestedExternalEvent.TurnInput::durationSeconds
            ).sum(),
            0,
            question.maxResponseSeconds(),
            null,
            question.topicName(),
            question.topicDescription(),
            evaluationGuide(question.evaluationGuideJson()),
            "practice",
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
