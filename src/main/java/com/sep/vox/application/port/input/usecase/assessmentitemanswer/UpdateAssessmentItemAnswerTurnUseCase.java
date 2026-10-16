package com.sep.vox.application.port.input.usecase.assessmentitemanswer;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.mapper.assessmentitemanswer.AssessmentItemAnswerResponseMapper;
import com.sep.vox.application.port.input.command.UpdateAssessmentItemAnswerTurnCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemAnswerTurnResponse;
import com.sep.vox.domain.model.assessment.AssessmentTurnType;
import com.sep.vox.domain.repository.assessment.AssessmentItemAnswerRepository;
import com.sep.vox.domain.repository.assessment.AssessmentItemResponseTurnRepository;

@Service
public class UpdateAssessmentItemAnswerTurnUseCase
        implements IUseCase<UpdateAssessmentItemAnswerTurnCommand, AssessmentItemAnswerTurnResponse> {

    private final AssessmentItemAnswerRepository assessmentItemAnswerRepository;
    private final AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository;

    public UpdateAssessmentItemAnswerTurnUseCase(
            AssessmentItemAnswerRepository assessmentItemAnswerRepository,
            AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository) {
        this.assessmentItemAnswerRepository = assessmentItemAnswerRepository;
        this.assessmentItemResponseTurnRepository = assessmentItemResponseTurnRepository;
    }

    @Override
    @Transactional
    public AssessmentItemAnswerTurnResponse execute(UpdateAssessmentItemAnswerTurnCommand input) {
        if (!assessmentItemAnswerRepository.existsById(input.answerId())) {
            throw new ResourceNotFoundException("Không tìm thấy câu trả lời của thí sinh");
        }

        var turn = assessmentItemResponseTurnRepository.findByAssessmentItemAnswerIdAndTurnOrder(input.answerId(), input.turnOrder())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy turn cần cập nhật"));

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

        var saved = assessmentItemResponseTurnRepository.upsert(turn);
        return AssessmentItemAnswerResponseMapper.toTurnResponse(saved);
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

    private AssessmentTurnType normalizeTurnType(String turnType) {
        if (turnType == null || turnType.isBlank()) {
            throw new IllegalArgumentException("turnType không được để trống");
        }

        var normalized = turnType.trim().toUpperCase(Locale.ROOT);
        try {
            return AssessmentTurnType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("turnType không hợp lệ");
        }
    }
}
