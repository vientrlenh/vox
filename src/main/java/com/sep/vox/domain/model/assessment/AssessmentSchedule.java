package com.sep.vox.domain.model.assessment;

import java.time.Instant;
import java.util.UUID;

public class AssessmentSchedule {
    private UUID id;
    private UUID assessmentId;
    private UUID schoolRoomId;
    private Instant startDate;
    private Instant endDate;
    private AssessmentScheduleStatus status;
    private UUID movedToScheduleId;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    protected AssessmentSchedule() {}

    public AssessmentSchedule(UUID id, UUID assessmentId, UUID schoolRoomId, Instant startDate, Instant endDate,
            AssessmentScheduleStatus status, UUID movedToScheduleId, Instant createdAt, Instant updatedAt,
            UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.assessmentId = assessmentId;
        this.schoolRoomId = schoolRoomId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.movedToScheduleId = movedToScheduleId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    /**
     * Tạo mới một ca thi ở trạng thái DRAFT (id do DB sinh).
     */
    public static AssessmentSchedule createFresh(UUID assessmentId, UUID schoolRoomId, Instant startDate,
            Instant endDate, UUID createdBy, Instant now) {
        return new AssessmentSchedule(assessmentId, schoolRoomId, startDate, endDate,
                AssessmentScheduleStatus.DRAFT, null, now, now, createdBy, createdBy);
    }

    /**
     * Ca thi chỉ được phép sửa (đổi phòng/giờ) khi còn ở trạng thái DRAFT.
     */
    public boolean isModifiable() {
        return this.status == AssessmentScheduleStatus.DRAFT;
    }

    /** Đã tới giờ bắt đầu. Đúng khoảnh khắc {@code startDate} tính là đã bắt đầu. */
    public boolean hasStartedAt(Instant now) {
        return this.startDate != null && !this.startDate.isAfter(now);
    }

    /**
     * Đã qua giờ kết thúc. Đúng khoảnh khắc {@code endDate} tính là đã hết giờ -- khớp với điều kiện
     * {@code endDate > :now} của các truy vấn vào phòng thi ({@code findByIdAndInSchedule}...), nếu
     * lệch biên thì có một giây mà ca vừa "chưa hết giờ" vừa "không vào thi được".
     */
    public boolean hasEndedAt(Instant now) {
        return this.endDate != null && !this.endDate.isAfter(now);
    }

    /**
     * Ca đang diễn ra: đã công bố, đã tới giờ và chưa hết giờ. Cùng vị từ với
     * {@code findByAssessmentIdAndInSchedule}, viết lại ở domain để service và worker khỏi phải chép biểu
     * thức so sánh {@code Instant}.
     */
    public boolean isOngoingAt(Instant now) {
        return this.status == AssessmentScheduleStatus.PUBLISHED && hasStartedAt(now) && !hasEndedAt(now);
    }

    public UUID getId() {
        return this.id;
    }

    public void setId(UUID id) {
        this.id = id;
    } 

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(UUID assessmentId) {
        this.assessmentId = assessmentId;
    }

    public UUID getSchoolRoomId() {
        return schoolRoomId;
    }

    public void setSchoolRoomId(UUID schoolRoomId) {
        this.schoolRoomId = schoolRoomId;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public AssessmentScheduleStatus getStatus() {
        return status;
    }

    public void setStatus(AssessmentScheduleStatus status) {
        this.status = status;
    }

    public UUID getMovedToScheduleId() {
        return movedToScheduleId;
    }

    public void setMovedToScheduleId(UUID movedToScheduleId) {
        this.movedToScheduleId = movedToScheduleId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    
}
