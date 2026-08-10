package com.sep.vox.domain.model.personalization;

import java.time.Instant;
import java.util.UUID;

/**
 * Hồ sơ học tập -- MỘT dòng cho MỘT học sinh, cập nhật tại chỗ.
 *
 * <p>Bản trước là append-only có cột {@code version}: mỗi lần đổi mục tiêu / nộp lại quiz / bật
 * tắt tự cập nhật là sinh một bản mới. Bỏ vì lịch sử đó được ghi mà không ai đọc -- chi tiết ở
 * chú thích {@code CREATE TABLE learner_profile} trong V13.
 *
 * <p>Đi kèm là {@code first()} và {@code next()} cũng bị gỡ: chúng chỉ tồn tại để dựng bản kế
 * tiếp. Giờ dùng setter thường, và {@code applyChanges} gom phần "chỉ ghi đè khi có giá trị".
 */
public class LearnerProfile {

    private UUID id;
    private UUID studentId;
    private String goalType;
    private boolean autoUpdateInterest;
    private Instant quizCompletedAt;
    private Instant recordedAt;

    public LearnerProfile() {
    }

    public LearnerProfile(
            UUID id,
            UUID studentId,
            String goalType,
            boolean autoUpdateInterest,
            Instant quizCompletedAt,
            Instant recordedAt) {
        this.id = id;
        this.studentId = studentId;
        this.goalType = goalType;
        this.autoUpdateInterest = autoUpdateInterest;
        this.quizCompletedAt = quizCompletedAt;
        this.recordedAt = recordedAt;
    }

    /** Hồ sơ mặc định cho học sinh chưa có gì -- bật tự cập nhật sở thích. */
    public static LearnerProfile forStudent(UUID studentId) {
        return new LearnerProfile(null, studentId, null, true, null, Instant.now());
    }

    /**
     * Ghi đè các trường có giá trị, giữ nguyên phần còn lại.
     *
     * <p>{@code null} nghĩa là "không đụng tới", đúng như ngữ nghĩa của {@code next()} cũ: mỗi
     * hành động (đặt mục tiêu / nộp quiz / bật tắt) chỉ truyền đúng phần nó phụ trách.
     */
    public void applyChanges(
            String nextGoalType,
            Boolean nextAutoUpdate,
            Instant nextQuizCompletedAt) {
        if (nextGoalType != null) {
            this.goalType = nextGoalType;
        }
        if (nextAutoUpdate != null) {
            this.autoUpdateInterest = nextAutoUpdate;
        }
        if (nextQuizCompletedAt != null) {
            this.quizCompletedAt = nextQuizCompletedAt;
        }
        this.recordedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public String getGoalType() {
        return goalType;
    }

    public void setGoalType(String goalType) {
        this.goalType = goalType;
    }

    public boolean isAutoUpdateInterest() {
        return autoUpdateInterest;
    }

    public void setAutoUpdateInterest(boolean autoUpdateInterest) {
        this.autoUpdateInterest = autoUpdateInterest;
    }

    public Instant getQuizCompletedAt() {
        return quizCompletedAt;
    }

    public void setQuizCompletedAt(Instant quizCompletedAt) {
        this.quizCompletedAt = quizCompletedAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
