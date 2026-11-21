package com.sep.vox.application.port.input.usecase.assessmentitemresponse;

import java.time.Instant;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.RecordAnswerTurnCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.model.assessment.AssessmentItemResponseTurn;
import com.sep.vox.domain.model.assessment.TurnType;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseTurnRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

@Service
public class RecordAnswerTurnUseCase implements IUseCase<RecordAnswerTurnCommand, Void> {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository;

    public RecordAnswerTurnUseCase(
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentItemResponseRepository assessmentItemResponseRepository,
            AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository) {
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentItemResponseRepository = assessmentItemResponseRepository;
        this.assessmentItemResponseTurnRepository = assessmentItemResponseTurnRepository;
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

        if (!assessmentSessionRepository.existsById(input.sessionId())) {
            throw new NotFoundException("Không tìm thấy phiên thi cho turn đã ghi nhận");
        }

        Instant answeredAt = input.answeredAt() == null ? Instant.now() : input.answeredAt();
        AssessmentItemResponse response = assessmentItemResponseRepository.findById(input.answerId())
            .orElseGet(() -> AssessmentItemResponse.builder()
                    .id(input.answerId())
                    .sessionId(input.sessionId())
                    .paperItemId(input.paperItemId())
                    .audioUrl(input.audioUrl())
                    .durationSeconds(input.durationSeconds())
                    .transcript(input.transcript())
                    .terminationReason(null)
                    .submittedAt(answeredAt)
                    .build());

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
        assessmentItemResponseRepository.save(response);

        // upsert() replaces the WHOLE row (see AssessmentItemResponseTurnRepositoryImpl: it keeps only the id and
        // createdAt of the old one), so the merge has to happen HERE before calling it. The semantics of
        // upsert are not changed because UpdateAssessmentItemResponseTurnUseCase is the path where a teacher edits by hand, where blanking out
        // a field is intentional.
        AssessmentItemResponseTurn existingTurn = assessmentItemResponseTurnRepository
            .findByAssessmentItemResponseIdAndTurnOrder(response.getId(), input.turnOrder())
            .orElse(null);

        // normalizeTurnType returns MAIN when the type is missing, so a phase that sends no turnType would downgrade
        // a FOLLOWUP turn to MAIN -- skewing both the follow-up statistics and how the assessment is displayed.
        AssessmentItemResponseTurn turn = AssessmentItemResponseTurn.builder()
                .assessmentItemResponseId(response.getId())
                .turnOrder(input.turnOrder())
                .turnType(isMeaningful(input.turnType()) || existingTurn == null
                    ? normalizeTurnType(input.turnType())
                    : existingTurn.getTurnType())
                .promptText(keep(input.promptText(), existingTurn == null ? null : existingTurn.getPromptText()))
                .audioUrl(keep(input.audioUrl(), existingTurn == null ? null : existingTurn.getAudioUrl()))
                .transcript(keep(input.transcript(), existingTurn == null ? null : existingTurn.getTranscript()))
                .durationSeconds(input.durationSeconds() != null || existingTurn == null
                ? input.durationSeconds()
                : existingTurn.getDurationSeconds())
                .wordCount(input.wordCount() != null || existingTurn == null
                ? input.wordCount()
                : existingTurn.getWordCount())
                .answeredAt(input.answeredAt() != null || existingTurn == null
                ? answeredAt
                : existingTurn.getAnsweredAt())
                .build();
        assessmentItemResponseTurnRepository.upsert(turn);
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

        String normalized = turnType.trim().toUpperCase(Locale.ROOT);
        try {
            return TurnType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("turnType không hợp lệ");
        }
    }
}
