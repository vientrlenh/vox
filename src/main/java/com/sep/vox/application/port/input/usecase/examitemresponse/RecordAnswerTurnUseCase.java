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
        // GHI ĐÈ CÓ ĐIỀU KIỆN, không vô điều kiện như trước.
        //
        // Một lượt nói tới đây HAI LẦN (xem turn_publisher.publish_turn_if_new): pha 1 ngay khi
        // quyết định follow-up xong, mang transcript của Voice Live nhưng chưa có bản ghi âm;
        // pha 2 khi bản ghi âm về, mang audio_url nhưng transcript Azure thì có thể rỗng --
        // đo được 2026-08-09: cả 3 lượt của một phiên thi thật đều rỗng.
        //
        // Gán vô điều kiện thì pha nào tới sau cũng xoá mất phần tốt của pha trước, và lượt nói
        // trông như thí sinh chưa nói gì.
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
        examItemResponseRepository.save(response);

        // upsert() thay TOÀN BỘ dòng (xem ExamItemResponseTurnRepositoryImpl: chỉ giữ lại id và
        // createdAt của bản cũ), nên phải trộn Ở ĐÂY trước khi gọi. Không sửa ngữ nghĩa của
        // upsert vì UpdateExamItemResponseTurnUseCase là đường giáo viên sửa tay, nơi xoá trắng
        // một trường là chủ ý.
        var existingTurn = examItemResponseTurnRepository
            .findByExamItemResponseIdAndTurnOrder(response.getId(), input.turnOrder())
            .orElse(null);

        var turn = new ExamItemResponseTurn(
            response.getId(),
            input.turnOrder(),
            // normalizeTurnType trả MAIN khi thiếu, nên một pha không gửi turnType sẽ hạ cấp
            // lượt FOLLOWUP thành MAIN -- lệch cả thống kê follow-up lẫn cách hiển thị bài.
            isMeaningful(input.turnType()) || existingTurn == null
                ? normalizeTurnType(input.turnType())
                : existingTurn.getTurnType(),
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
                : existingTurn.getAnsweredAt(),
            null
        );
        examItemResponseTurnRepository.upsert(turn);
        return null;
    }

    /** Giá trị mới có đáng ghi đè không -- chuỗi rỗng/toàn khoảng trắng thì không. */
    private static boolean isMeaningful(String value) {
        return value != null && !value.isBlank();
    }

    /** Lấy giá trị mới nếu nó có nghĩa, ngược lại giữ nguyên giá trị đang có. */
    private static String keep(String incoming, String existing) {
        return isMeaningful(incoming) ? incoming : existing;
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
