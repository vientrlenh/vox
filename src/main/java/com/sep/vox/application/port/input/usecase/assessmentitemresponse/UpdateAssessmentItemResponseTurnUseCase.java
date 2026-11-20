package com.sep.vox.application.port.input.usecase.assessmentitemresponse;

import com.sep.vox.domain.model.assessment.AssessmentItemResponseTurn;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.assessmentitemresponse.AssessmentItemResponseResponseMapper;
import com.sep.vox.application.port.input.command.UpdateAssessmentItemResponseTurnCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemResponseTurnResponse;
import com.sep.vox.domain.model.assessment.TurnType;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseTurnRepository;

@Service
public class UpdateAssessmentItemResponseTurnUseCase
        implements IUseCase<UpdateAssessmentItemResponseTurnCommand, AssessmentItemResponseTurnResponse> {

    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository;

    public UpdateAssessmentItemResponseTurnUseCase(
            AssessmentItemResponseRepository assessmentItemResponseRepository,
            AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository) {
        this.assessmentItemResponseRepository = assessmentItemResponseRepository;
        this.assessmentItemResponseTurnRepository = assessmentItemResponseTurnRepository;
    }

    @Override
    @Transactional
    public AssessmentItemResponseTurnResponse execute(UpdateAssessmentItemResponseTurnCommand input) {
        if (!assessmentItemResponseRepository.existsById(input.answerId())) {
            throw new NotFoundException("Không tìm thấy câu trả lời của thí sinh");
        }

        AssessmentItemResponseTurn turn = assessmentItemResponseTurnRepository.findByAssessmentItemResponseIdAndTurnOrder(input.answerId(), input.turnOrder())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy turn cần cập nhật"));

        if (input.turnType() != null) {
            turn.setTurnType(normalizeTurnType(input.turnType()));
        }
        if (input.promptText() != null) {
            turn.setPromptText(input.promptText());
        }
        if (input.audioUrl() != null) {
            turn.setAudioUrl(input.audioUrl());
        }
        if (input.transcript() != null) {
            turn.setTranscript(input.transcript());
        }
        if (input.durationSeconds() != null) {
            turn.setDurationSeconds(input.durationSeconds());
        }
        if (input.wordCount() != null) {
            turn.setWordCount(input.wordCount());
        }
        if (input.answeredAt() != null) {
            turn.setAnsweredAt(input.answeredAt());
        }

        AssessmentItemResponseTurn saved = assessmentItemResponseTurnRepository.upsert(turn);
        return AssessmentItemResponseResponseMapper.toTurnResponse(saved);
    }

    public static Instant parseAnsweredAt(String answeredAt) {
        if (answeredAt == null) {
            return null;
        }
        if (answeredAt.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(answeredAt.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("answeredAt không đúng định dạng ISO-8601");
        }
    }

    private TurnType normalizeTurnType(String turnType) {
        if (turnType == null || turnType.isBlank()) {
            throw new IllegalArgumentException("turnType không được để trống");
        }

        String normalized = turnType.trim().toUpperCase(Locale.ROOT);
        try {
            return TurnType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("turnType không hợp lệ");
        }
    }
}
