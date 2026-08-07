package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Id;

@Entity
@Table(
    name = "learner_profile",
    indexes = @Index(
        name = "idx_learner_profile_student_version",
        columnList = "student_id, version",
        unique = true
    )
)
public class LearnerProfileJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id",
        nullable = false,
        updatable = false,
        insertable = false,
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "version", nullable = false, updatable = false)
    private int version;

    @Column(name = "goal_type", length = 24)
    private String goalType;

    // GỠ 2026-08-07: targetExam / targetDate. Cột `target_exam` và `target_date` VẪN CÒN trong
    // DB (V13 đã chạy, không sửa được checksum) nhưng không ánh xạ nữa -- cả hai chưa từng được
    // ghi: appendProfile không có tham số cho chúng, không setter nào được gọi, và next() chỉ
    // chép giá trị cũ sang bản mới. Tức chúng vĩnh viễn NULL. Không mặt ở GraphQL lẫn Flutter.

    // GỠ 2026-08-07: flsaScore / flsaRawAnswersJson -- thang tự đánh giá lo lắng ngoại ngữ.
    // Cột `flsa_score` và `flsa_raw_answers_json` cũng giữ nguyên trong DB, chỉ thôi ánh xạ.
    // Chúng ĐƯỢC ghi (qua mutation submitFlsaSelfReport) nhưng chưa từng được ĐỌC để đổi hành
    // vi: không luồng sinh câu hỏi, chọn độ khó hay xếp hạng chủ đề nào tra tới. Client cũng đã
    // bỏ màn hỏi từ trước -- Flutter chỉ còn chú thích nhắc rằng mutation vẫn còn nếu cần quay lại.

    @Column(name = "auto_update_interest", nullable = false)
    private boolean autoUpdateInterest = true;

    @Column(name = "quiz_completed_at")
    private Instant quizCompletedAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected LearnerProfileJpaEntity() {
    }

    public LearnerProfileJpaEntity(
            UUID studentId,
            int version,
            String goalType,
            boolean autoUpdateInterest,
            Instant quizCompletedAt,
            Instant recordedAt) {
        this.studentId = studentId;
        this.version = version;
        this.goalType = goalType;
        this.autoUpdateInterest = autoUpdateInterest;
        this.quizCompletedAt = quizCompletedAt;
        this.recordedAt = recordedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public int getVersion() {
        return version;
    }

    public String getGoalType() {
        return goalType;
    }

    public boolean isAutoUpdateInterest() {
        return autoUpdateInterest;
    }

    public Instant getQuizCompletedAt() {
        return quizCompletedAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
