package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "practice_item_response",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_practice_response_session_question",
        columnNames = {"practice_session_id", "practice_question_id"}
    ),
    indexes = @Index(name = "idx_practice_response_session", columnList = "practice_session_id")
)
public class PracticeItemResponseJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "practice_session_id", nullable = false, updatable = false)
    private UUID practiceSessionId;
    @Column(name = "practice_question_id", nullable = false, updatable = false)
    private UUID practiceQuestionId;
    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl;
    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;
    /** Chuỗi follow-up của câu này đã kết thúc chưa -- xem V14 để biết vì sao phải lưu. */
    @Column(name = "question_complete", nullable = false)
    private boolean questionComplete;

    /**
     * Vòng đời chấm -- giá trị theo {@link com.sep.vox.domain.model.personalization.PracticeGradingStatus}.
     *
     * <p>Để String kèm {@code @CheckConstraint} chứ không {@code @Enumerated}, bám đúng khuôn
     * {@code ExamSessionJpaEntity.status}: ràng buộc nằm ở DB nên dữ liệu rác không vào được kể
     * cả khi ai đó sửa tay, còn enum lo phần an toàn kiểu ở tầng ứng dụng.
     */
    @Column(name = "grading_status", nullable = false, length = 16, check = {
        @CheckConstraint(
            name = "chk_practice_item_response_grading_status",
            constraint = "grading_status IN ('PENDING', 'GRADING', 'GRADED', 'GRADING_FAILED')"
        )
    })
    private String gradingStatus;

    /**
     * Lúc gửi yêu cầu chấm gần nhất. Đi CẶP với {@link #gradingStatus}: trạng thái nói đang ở
     * đâu, mốc này nói đã ở đó bao lâu -- thiếu nó thì GRADING treo vĩnh viễn không ai cứu.
     */
    @Column(name = "grading_requested_at")
    private Instant gradingRequestedAt;

    /** Số lần đã gửi yêu cầu chấm. Trần ở {@code PracticeGradingFlushService.MAX_GRADING_ATTEMPTS}. */
    @Column(name = "grading_attempts", nullable = false)
    private int gradingAttempts;

    protected PracticeItemResponseJpaEntity() {
    }

    public PracticeItemResponseJpaEntity(
            UUID id,
            UUID practiceSessionId,
            UUID practiceQuestionId,
            String audioUrl,
            String transcript,
            boolean questionComplete) {
        this.id = id;
        this.practiceSessionId = practiceSessionId;
        this.practiceQuestionId = practiceQuestionId;
        this.audioUrl = audioUrl;
        this.transcript = transcript;
        this.questionComplete = questionComplete;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPracticeSessionId() {
        return practiceSessionId;
    }

    public UUID getPracticeQuestionId() {
        return practiceQuestionId;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public boolean isQuestionComplete() {
        return questionComplete;
    }

    public void setQuestionComplete(boolean questionComplete) {
        this.questionComplete = questionComplete;
    }

    public String getGradingStatus() {
        return gradingStatus;
    }

    public Instant getGradingRequestedAt() {
        return gradingRequestedAt;
    }

    public int getGradingAttempts() {
        return gradingAttempts;
    }
}
