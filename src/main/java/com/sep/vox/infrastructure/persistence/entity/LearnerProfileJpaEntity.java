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
    // Phải khớp V16: chỉ mục nay chỉ trên student_id (bất biến 1-1). Khai còn cột `version` ở
    // đây là bảo Hibernate tạo chỉ mục trên một cột không tồn tại.
    indexes = @Index(
        name = "idx_learner_profile_student",
        columnList = "student_id",
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

    @Column(name = "goal_type", length = 24)
    private String goalType;

    // Không còn cột `version`: một học sinh một hồ sơ, cập nhật tại chỗ. V16 drop cột này khỏi
    // DB -- xem chú thích đầu migration đó.

    // GỠ 2026-08-07: flsaScore / flsaRawAnswersJson -- thang tự đánh giá lo lắng ngoại ngữ.
    // Hai cột `flsa_score` / `flsa_raw_answers_json` nay cũng không còn trong DB -- V14 drop
    // chúng (cùng target_exam / target_date) khỏi bảng do V13 tạo.
    // Chúng ĐƯỢC ghi (qua mutation submitFlsaSelfReport) nhưng chưa từng được ĐỌC để đổi hành
    // vi: không luồng sinh câu hỏi, chọn độ khó hay xếp hạng chủ đề nào tra tới. Client cũng đã
    // bỏ màn hỏi từ trước -- Flutter chỉ còn chú thích nhắc rằng mutation vẫn còn nếu cần quay lại.

    @Column(name = "auto_update_interest", nullable = false)
    private boolean autoUpdateInterest = true;

    @Column(name = "quiz_completed_at")
    private Instant quizCompletedAt;

    // updatable: hồ sơ nay cập nhật TẠI CHỖ nên mốc này phải đổi theo. Bản cũ khoá
    // updatable=false vì mỗi thay đổi sinh một dòng mới, mốc không bao giờ cần sửa.
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected LearnerProfileJpaEntity() {
    }

    public LearnerProfileJpaEntity(
            UUID studentId,
            String goalType,
            boolean autoUpdateInterest,
            Instant quizCompletedAt,
            Instant recordedAt) {
        this.studentId = studentId;
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

    public void setId(UUID id) {
        this.id = id;
    }

    public void setGoalType(String goalType) {
        this.goalType = goalType;
    }

    public void setAutoUpdateInterest(boolean autoUpdateInterest) {
        this.autoUpdateInterest = autoUpdateInterest;
    }

    public void setQuizCompletedAt(Instant quizCompletedAt) {
        this.quizCompletedAt = quizCompletedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
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
