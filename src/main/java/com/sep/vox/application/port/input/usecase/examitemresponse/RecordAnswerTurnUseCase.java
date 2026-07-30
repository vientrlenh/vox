package com.sep.vox.application.port.input.usecase.examitemresponse;

import java.time.Instant;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.RecordAnswerTurnCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.exam.ExamItemResponse;
import com.sep.vox.domain.model.exam.ExamItemResponseTurn;
import com.sep.vox.domain.model.exam.TurnType;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamItemResponseTurnRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class RecordAnswerTurnUseCase implements IUseCase<RecordAnswerTurnCommand, Void> {

    private final ExamSessionRepository examSessionRepository;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamItemResponseTurnRepository examItemResponseTurnRepository;

    public RecordAnswerTurnUseCase(
            ExamSessionRepository examSessionRepository,
            ExamItemResponseRepository examItemResponseRepository,
            ExamItemResponseTurnRepository examItemResponseTurnRepository) {
        this.examSessionRepository = examSessionRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.examItemResponseTurnRepository = examItemResponseTurnRepository;
    }

    @Override
    @Transactional
    public Void execute(RecordAnswerTurnCommand input) {
        if (input.sessionId() == null) {
            throw new IllegalArgumentException("Thiếu sessionId trong dữ liệu turn");
        }
        if (input.answerId() == null) {
            throw new IllegalArgumentException("Thiếu answerId trong dữ liệu turn");
        }
        if (input.turnOrder() <= 0) {
            throw new IllegalArgumentException("turnOrder phải lớn hơn 0");
        }

        if (!examSessionRepository.existsById(input.sessionId())) {
            throw new NotFoundException("Không tìm thấy phiên thi cho turn đã ghi nhận");
        }

        var answeredAt = input.answeredAt() == null ? Instant.now() : input.answeredAt();
        var response = examItemResponseRepository.findById(input.answerId())
            .orElseGet(() -> new ExamItemResponse(
                input.answerId(),
                input.sessionId(),
                input.paperItemId(),
                input.audioUrl(),
                input.durationSeconds(),
                input.transcript(),
                null,
                answeredAt
            ));

        response.setSessionId(input.sessionId());
        if (response.getPaperItemId() == null && input.paperItemId() != null) {
            response.setPaperItemId(input.paperItemId());
        }
        response.setAudioUrl(input.audioUrl());
        response.setDurationSeconds(input.durationSeconds());
        response.setTranscript(input.transcript());
        response.setSubmittedAt(answeredAt);
        examItemResponseRepository.save(response);

        var turn = new ExamItemResponseTurn(
            response.getId(),
            input.turnOrder(),
            normalizeTurnType(input.turnType()),
            input.promptText(),
            input.audioUrl(),
            input.transcript(),
            input.durationSeconds(),
            input.wordCount(),
            answeredAt,
            null
        );
        examItemResponseTurnRepository.upsert(turn);
        return null;
    }

    private TurnType normalizeTurnType(String turnType) {
        if (turnType == null || turnType.isBlank()) {
            return TurnType.MAIN;
        }

        var normalized = turnType.trim().toUpperCase(Locale.ROOT);
        try {
            return TurnType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("turnType không hợp lệ");
        }
    }
}
