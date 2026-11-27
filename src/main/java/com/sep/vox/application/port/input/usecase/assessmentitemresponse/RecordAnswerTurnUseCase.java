package com.sep.vox.application.port.input.usecase.assessmentitemresponse;

import java.time.Instant;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentitemresponse.RecordAnswerTurnCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.model.assessment.AssessmentItemResponseTurn;
import com.sep.vox.domain.model.assessment.TurnType;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseTurnRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

@Service
@RequiredArgsConstructor
public class RecordAnswerTurnUseCase implements IUseCase<RecordAnswerTurnCommand, Void> {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository;

    @Override
    @Transactional
    public Void execute(RecordAnswerTurnCommand input) {
        requireValidTurnInput(input);

        if (!assessmentSessionRepository.existsById(input.sessionId())) {
            throw new ResourceNotFoundException("Assessment session not found for the recorded turn");
        }

        Instant answeredAt = input.answeredAt() == null ? Instant.now() : input.answeredAt();
        AssessmentItemResponse response = findOrCreateResponse(input, answeredAt);
        mergeIntoResponse(response, input, answeredAt);
        assessmentItemResponseRepository.save(response);

        assessmentItemResponseTurnRepository.upsert(mergeTurn(response, input, answeredAt));
        return null;
    }

    private void requireValidTurnInput(RecordAnswerTurnCommand input) {
        if (input.sessionId() == null) {
            throw new IllegalArgumentException("sessionId is missing from the turn payload");
        }
        if (input.answerId() == null) {
            throw new IllegalArgumentException("answerId is missing from the turn payload");
        }
        if (input.turnOrder() <= 0) {
            throw new IllegalArgumentException("turnOrder must be greater than 0");
        }
    }

    private AssessmentItemResponse findOrCreateResponse(RecordAnswerTurnCommand input, Instant answeredAt) {
        return assessmentItemResponseRepository.findById(input.answerId())
            .orElseGet(() -> AssessmentItemResponse.answered(
                    input.answerId(),
                    input.sessionId(),
                    input.paperItemId(),
                    input.audioUrl(),
                    input.durationSeconds(),
                    input.transcript(),
                    answeredAt));
    }

    /**
     * Overwrites conditionally, not unconditionally as before.
     *
     * <p>A turn arrives here TWICE (see turn_publisher.publish_turn_if_new): phase 1 right after the follow-up
     * decision, carrying the Voice Live transcript but no recording yet; phase 2 when the recording arrives,
     * carrying audio_url while the Azure transcript may be empty -- measured on 2026-08-09: all 3 turns of a real
     * assessment session were empty.
     *
     * <p>With an unconditional assignment, whichever phase arrives later wipes the good part of the earlier one, and
     * the turn looks as if the candidate said nothing.
     */
    private void mergeIntoResponse(AssessmentItemResponse response, RecordAnswerTurnCommand input, Instant answeredAt) {
        response.setSessionId(input.sessionId());
        if (response.getPaperItemId() == null && input.paperItemId() != null) {
            response.setPaperItemId(input.paperItemId());
        }
        if (isMeaningful(input.audioUrl())) {
            response.setAudioUrl(input.audioUrl());
        }
        if (input.durationSeconds() != null) {
            response.setDurationSeconds(input.durationSeconds());
        }
        if (isMeaningful(input.transcript())) {
            response.setTranscript(input.transcript());
        }
        if (input.answeredAt() != null || response.getSubmittedAt() == null) {
            response.setSubmittedAt(answeredAt);
        }
    }

    /**
     * upsert() replaces the WHOLE row (see AssessmentItemResponseTurnRepositoryImpl: it keeps only the id and
     * createdAt of the old one), so the merge has to happen HERE before calling it. The semantics of
     * upsert are not changed because UpdateAssessmentItemResponseTurnUseCase is the path where a teacher edits by hand, where blanking out
     * a field is intentional.
     */
    private AssessmentItemResponseTurn mergeTurn(
        AssessmentItemResponse response, RecordAnswerTurnCommand input, Instant answeredAt) {
        AssessmentItemResponseTurn existingTurn = assessmentItemResponseTurnRepository
            .findByAssessmentItemResponseIdAndTurnOrder(response.getId(), input.turnOrder())
            .orElse(null);

        return AssessmentItemResponseTurn.answered(
                response.getId(),
                input.turnOrder(),
                mergedTurnType(input, existingTurn),
                keep(input.promptText(), existingTurn == null ? null : existingTurn.getPromptText()),
                keep(input.audioUrl(), existingTurn == null ? null : existingTurn.getAudioUrl()),
                keep(input.transcript(), existingTurn == null ? null : existingTurn.getTranscript()),
                input.durationSeconds() != null || existingTurn == null
                    ? input.durationSeconds()
                    : existingTurn.getDurationSeconds(),
                input.wordCount() != null || existingTurn == null
                    ? input.wordCount()
                    : existingTurn.getWordCount(),
                input.answeredAt() != null || existingTurn == null
                    ? answeredAt
                    : existingTurn.getAnsweredAt());
    }

    /**
     * normalizeTurnType returns MAIN when the type is missing, so a phase that sends no turnType would downgrade
     * a FOLLOWUP turn to MAIN -- skewing both the follow-up statistics and how the assessment is displayed.
     */
    private TurnType mergedTurnType(RecordAnswerTurnCommand input, AssessmentItemResponseTurn existingTurn) {
        return isMeaningful(input.turnType()) || existingTurn == null
            ? normalizeTurnType(input.turnType())
            : existingTurn.getTurnType();
    }

    /** Whether a new value is worth overwriting with: an empty or whitespace-only string is not. */
    private static boolean isMeaningful(String value) {
        return value != null && !value.isBlank();
    }

    /** Takes the new value if it is meaningful, otherwise keeps the existing one. */
    private static String keep(String incoming, String existing) {
        return isMeaningful(incoming) ? incoming : existing;
    }

    private TurnType normalizeTurnType(String turnType) {
        if (turnType == null || turnType.isBlank()) {
            return TurnType.MAIN;
        }

        String normalized = turnType.trim().toUpperCase(Locale.ROOT);
        try {
            return TurnType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("turnType is invalid");
        }
    }
}
