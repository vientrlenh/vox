package com.sep.vox.application.port.input.usecase.examitemresponse;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.examitemresponse.ExamItemResponseResponseMapper;
import com.sep.vox.application.port.input.command.UpdateExamItemResponseTurnCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examitemresponse.ExamItemResponseTurnResponse;
import com.sep.vox.domain.model.exam.TurnType;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamItemResponseTurnRepository;

@Service
public class UpdateExamItemResponseTurnUseCase
        implements IUseCase<UpdateExamItemResponseTurnCommand, ExamItemResponseTurnResponse> {

    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamItemResponseTurnRepository examItemResponseTurnRepository;

    public UpdateExamItemResponseTurnUseCase(
            ExamItemResponseRepository examItemResponseRepository,
            ExamItemResponseTurnRepository examItemResponseTurnRepository) {
        this.examItemResponseRepository = examItemResponseRepository;
        this.examItemResponseTurnRepository = examItemResponseTurnRepository;
    }

    @Override
    @Transactional
    public ExamItemResponseTurnResponse execute(UpdateExamItemResponseTurnCommand input) {
        if (!examItemResponseRepository.existsById(input.answerId())) {
            throw new NotFoundException("Không tìm thấy câu trả lời của thí sinh");
        }

        var turn = examItemResponseTurnRepository.findByExamItemResponseIdAndTurnOrder(input.answerId(), input.turnOrder())
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

        var saved = examItemResponseTurnRepository.upsert(turn);
        return ExamItemResponseResponseMapper.toTurnResponse(saved);
    }

    public static OffsetDateTime parseAnsweredAt(String answeredAt) {
        if (answeredAt == null) {
            return null;
        }
        if (answeredAt.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(answeredAt.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("answeredAt không đúng định dạng ISO-8601");
        }
    }

    private TurnType normalizeTurnType(String turnType) {
        if (turnType == null || turnType.isBlank()) {
            throw new IllegalArgumentException("turnType không được để trống");
        }

        var normalized = turnType.trim().toUpperCase(Locale.ROOT);
        try {
            return TurnType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("turnType không hợp lệ");
        }
    }
}
