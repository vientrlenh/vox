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
import com.sep.vox.application.exception.ResourceNotFoundException;

/**
 * Builds the grading request for ONE question of a practice session.
 *
 * Split out of {@code SubmitPracticeTurnUseCase} because there are now TWO places that need to fire a grading request, no
 * longer one: the last turn of a question (the normal path) and the closing of a session with the questions the student
 * left unfinished (see {@link PracticeGradingFlushService}). Leaving it in the use case and calling across
 * would force both paths through the same @Transactional -- yet the flush path runs inside the
 * session-closing transaction, a completely different context.
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
            .orElseThrow(() -> new ResourceNotFoundException("Practice question not found."));
        List<AssessmentAttemptEvaluationRequestedExternalEvent.TurnInput> turns = loadTurns(responseId);
        return new PracticeAttemptEvaluationRequestedExternalEvent(
            sessionId.toString(),
            responseId.toString(),
            questionId.toString(),
            buildPayload(sessionId, question, turns)
        );
    }

    private List<AssessmentAttemptEvaluationRequestedExternalEvent.TurnInput> loadTurns(UUID responseId) {
        return practiceResponseTurnRepository
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
    }

    /**
     * The question type is the REAL one (V13). It used to send "SPEAKING" -- not part of the QuestionType enum
     * on the Python side, so the validator silently swallowed it into null, which made
     * get_expected_min_words fall into the default branch and expect EXACTLY 10 WORDS for every
     * question, even a 45-second one. With the real type it runs the right branch: DESCRIPTION 45s
     * expects 35 words instead of 10.
     *
     * <p>mode: "unscripted" exactly like the assessment path. It used to send "practice" -- the wrong AXIS:
     * SpeakingMode on the Python side is scripted/unscripted (speaking from a given text or speaking
     * freely), not assessment/practice. The enum has no 'practice' so
     * SpeakingMode(request_payload.mode) threw ValueError, failed all 3 retries and then
     * ended with AssessmentAttemptEvaluationFailedEvent -- the practice grading chain never ran.
     */
    private AssessmentAttemptEvaluationRequestedExternalEvent.Payload buildPayload(
            UUID sessionId,
            QuestionEvaluationInfo question,
            List<AssessmentAttemptEvaluationRequestedExternalEvent.TurnInput> turns) {
        return new AssessmentAttemptEvaluationRequestedExternalEvent.Payload(
            question.questionText(),
            question.questionType(),
            null,
            turns.stream().mapToInt(turn -> turn.durationSeconds()).sum(),
            question.minResponseSeconds(),
            question.maxResponseSeconds(),
            null,
            question.topicName(),
            question.topicDescription(),
            evaluationGuide(question.evaluationGuideJson()),
            "unscripted",
            null,
            "en-US",
            criteriaFrameworks(sessionId),
            turns
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
        return grouped.values().stream().map(group -> toCriterionFramework(group)).toList();
    }

    /**
     * targetBandOnly = true: practice sends EXACTLY the band the student chose, and Python
     * already has a branch for it -- "assign a score within {range} for how fully this
     * answer satisfies the TARGET band's descriptor".
     *
     * <p>Weight: null. Python does not read this field (grep agents/src finds 0 places), and
     * the question score is the arithmetic mean of the 5 criteria in RecordPracticeAttemptEvaluationUseCase
     * -- i.e. 20% per criterion, already right without any weight.
     */
    private AssessmentAttemptEvaluationRequestedExternalEvent.CriterionFramework toCriterionFramework(
            List<CriterionFrameworkInfo> group) {
        CriterionFrameworkInfo first = group.get(0);
        return new AssessmentAttemptEvaluationRequestedExternalEvent.CriterionFramework(
            criterionKeyOf(first),
            first.getFrameworkCode(),
            first.getFrameworkName(),
            first.getFrameworkDescription(),
            first.getTargetBandId(),
            first.getTargetBandCode(),
            first.getTargetBandLabel(),
            true,
            null,
            first.getMinScore(),
            first.getMaxScore(),
            toFrameworkBands(group)
        );
    }

    private String criterionKeyOf(CriterionFrameworkInfo first) {
        String criterionKey = first.getCriterionCode().trim().toLowerCase(Locale.ROOT);
        if ("discourse".equals(criterionKey)) {
            criterionKey = "coherence";
        }
        return criterionKey;
    }

    private List<AssessmentAttemptEvaluationRequestedExternalEvent.FrameworkBand> toFrameworkBands(
            List<CriterionFrameworkInfo> group) {
        return group.stream()
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
    }
}
