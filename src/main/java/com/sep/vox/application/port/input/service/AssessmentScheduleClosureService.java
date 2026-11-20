package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * The ONLY place that states the rule "close the sessions according to the assessment".
 *
 * <p>Previously closing an assessment ({@code CLOSE}) did not touch its sessions, so a closed assessment left its sessions in
 * PUBLISHED forever: such a session passed both {@code isVisibleToStudent()} and {@code allowsAttendance()},
 * and COMPLETED could only be reached when someone clicked each session by hand. The most common way to close an assessment
 * is {@code AssessmentStatusAutoTransitionJob} -- where there is nobody to click.
 *
 * <p>Three paths call in here: closing an assessment ({@code UpdateAssessmentStatusUseCase} + the job), cancelling/deleting one
 * ({@code UpdateAssessmentStatusUseCase} + {@code DeleteAssessmentUseCase} -- previously two hand-copied versions of
 * the same function, with a note "change one, change both"), and the job that sweeps sessions whose time has run out.
 */
@Service
public class AssessmentScheduleClosureService {

    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;

    public AssessmentScheduleClosureService(
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentSessionRepository assessmentSessionRepository) {
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
    }

    /**
     * Blocks closing an assessment while a running session still has people in the middle of an attempt.
     *
     * <p>Deliberately does NOT block strictly by time window: a class test has exactly one session that coincides exactly with
     * openAt-closeAt, so blocking by time would mean the teacher can never close early, and the only remaining
     * way out (cancelling the session) makes {@code allowsAttendance()} return false => the right to fix the attendance of
     * a session that really took place is lost.
     *
     * <p>Closing an assessment midway does not lose data -- {@code DeferredAssessmentSessionGradingJob} still forces
     * submission and grades the part that was done. This guard exists to prevent an accidental click that cuts a session short, so when it blocks,
     * the right way out is {@code forceEndAssessmentSession} for exactly the session that is still hanging.
     */
    public void requireNoActiveSessionInOngoingSchedule(UUID assessmentId, Instant now) {
        if (assessmentScheduleRepository.findByAssessmentIdAndInSchedule(assessmentId, now).isEmpty()) {
            return;
        }
        long activeCount = assessmentSessionRepository.countActiveByAssessmentId(assessmentId);
        if (activeCount > 0) {
            throw new IllegalStateException("Còn " + activeCount
                + " học sinh đang làm bài ở ca thi đang diễn ra, không thể đóng bài kiểm tra");
        }
    }

    /**
     * Cascade khi ĐÓNG bài: ca đã qua giờ kết thúc → COMPLETED (đã thi thật), ca chưa từng chạy
     * (còn DRAFT, hoặc đã công bố nhưng chưa tới giờ) → CANCELLED (sẽ không bao giờ diễn ra).
     *
     * <p>Phân biệt theo giờ chứ không gộp một trạng thái, vì gộp về COMPLETED thì ca chưa ai vào
     * cũng mang nhãn "đã hoàn thành" (sai với điểm danh và báo cáo), còn gộp về CANCELLED thì ca đã
     * thi xong bị ghi là "bị huỷ" (sai lịch sử).
     *
     * @return số ca đã đổi trạng thái
     */
    public int closeSchedulesForAssessment(UUID assessmentId, UUID actorId, Instant now) {
        int changed = 0;
        for (AssessmentSchedule schedule : assessmentScheduleRepository.findByAssessmentId(assessmentId)) {
            if (isTerminal(schedule.getStatus())) {
                continue;
            }
            // Chỉ ca ĐÃ CÔNG BỐ mới có thể đã thi thật: ca còn DRAFT thì học sinh chưa từng nhìn
            // thấy nó, dù khung giờ đã trôi qua -- gọi là COMPLETED là bịa ra một buổi thi không có.
            // Ca đang diễn ra không tới được đây vì guard chạy trước; nếu có thì nó đã bắt đầu thật
            // nên COMPLETED vẫn là nhãn đúng hơn CANCELLED.
            boolean alreadyRan = schedule.getStatus() == AssessmentScheduleStatus.PUBLISHED
                && (schedule.hasEndedAt(now) || schedule.isOngoingAt(now));
            AssessmentScheduleStatus target = alreadyRan ? AssessmentScheduleStatus.COMPLETED : AssessmentScheduleStatus.CANCELLED;
            apply(schedule, target, actorId, now);
            changed++;
        }
        return changed;
    }

    /**
     * Cascade khi HUỶ hoặc XOÁ bài: mọi ca chưa kết thúc → CANCELLED, không phân biệt đã chạy hay
     * chưa. Khác đóng bài ở chỗ đó: bài bị huỷ thì ca cũng bị huỷ.
     *
     * @return số ca đã đổi trạng thái
     */
    public int cancelSchedulesForAssessment(UUID assessmentId, UUID actorId, Instant now) {
        int changed = 0;
        for (AssessmentSchedule schedule : assessmentScheduleRepository.findByAssessmentId(assessmentId)) {
            if (isTerminal(schedule.getStatus())) {
                continue;
            }
            apply(schedule, AssessmentScheduleStatus.CANCELLED, actorId, now);
            changed++;
        }
        return changed;
    }

    /**
     * Ca đã công bố mà đã qua giờ kết thúc → COMPLETED, tối đa {@code limit} ca mỗi lượt. Đây là
     * đường bình thường: ca tự đóng khi hết giờ, không phải chờ tới lúc bài kiểm tra đóng.
     *
     * @return số ca đã đổi trạng thái
     */
    public int completeEndedSchedules(Instant now, int limit) {
        List<AssessmentSchedule> ended = assessmentScheduleRepository.findPublishedEndedBefore(now, limit);
        for (AssessmentSchedule schedule : ended) {
            apply(schedule, AssessmentScheduleStatus.COMPLETED, null, now);
        }
        return ended.size();
    }

    /**
     * COMPLETED/MOVED/CANCELLED are final states: overwriting them would erase the trace of a session that has already been held and would
     * skew {@code movedToScheduleId}. DELETED is already filtered out by {@code findByAssessmentId}. The cascade skips them
     * rather than throwing an error.
     */
    private boolean isTerminal(AssessmentScheduleStatus status) {
        return status != AssessmentScheduleStatus.DRAFT && status != AssessmentScheduleStatus.PUBLISHED;
    }

    /**
     * {@code actorId} null là lời gọi từ worker: ghi null đè lên sẽ xoá mất người sửa cuối, nên chỉ
     * ghi khi thật sự có người thao tác.
     */
    private void apply(AssessmentSchedule schedule, AssessmentScheduleStatus target, UUID actorId, Instant now) {
        schedule.setStatus(target);
        schedule.setUpdatedAt(now);
        if (actorId != null) {
            schedule.setUpdatedBy(actorId);
        }
        assessmentScheduleRepository.save(schedule);
    }
}
