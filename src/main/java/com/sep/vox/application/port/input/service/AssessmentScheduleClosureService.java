package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;

/**
 * Chỗ DUY NHẤT phát biểu luật "đóng ca thi theo bài kiểm tra".
 *
 * <p>Trước đây đóng bài ({@code CLOSE}) không đụng gì tới ca thi, nên bài đã đóng vẫn để lại ca ở
 * PUBLISHED vĩnh viễn: ca đó lọt qua {@code isVisibleToStudent()} lẫn {@code allowsAttendance()},
 * và COMPLETED thì chỉ tới được khi có người bấm tay từng ca một. Đường đóng bài phổ biến nhất lại
 * là {@code AssessmentStatusAutoTransitionJob} — nơi không có ai để bấm.
 *
 * <p>Ba đường gọi vào đây: đóng bài ({@code UpdateAssessmentStatusUseCase} + job), huỷ/xoá bài
 * ({@code UpdateAssessmentStatusUseCase} + {@code DeleteAssessmentUseCase} — trước đây là hai bản chép tay của
 * cùng một hàm, kèm ghi chú "sửa thì sửa cả hai"), và job quét ca hết giờ.
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
     * Chặn đóng bài khi ca đang diễn ra vẫn còn người làm bài dở.
     *
     * <p>Cố ý KHÔNG chặn cứng theo khung giờ: bài kiểm tra trên lớp có đúng một ca trùng khít
     * openAt–closeAt, nên chặn theo giờ là giáo viên không bao giờ đóng sớm được, và lối thoát duy
     * nhất còn lại (huỷ ca) làm {@code allowsAttendance()} trả false ⇒ mất quyền sửa điểm danh của
     * một ca đã thi thật.
     *
     * <p>Đóng bài giữa chừng không làm mất dữ liệu — {@code DeferredAssessmentSessionGradingJob} vẫn ép
     * nộp và chấm phần đã làm. Guard này để chống bấm nhầm cắt ngang buổi thi, nên khi bị chặn thì
     * lối thoát đúng nghĩa là {@code forceEndAssessmentSession} cho đúng phiên còn treo.
     */
    public void requireNoActiveSessionInOngoingSchedule(UUID assessmentId, Instant now) {
        if (assessmentScheduleRepository.findByAssessmentIdAndInSchedule(assessmentId, now).isEmpty()) {
            return;
        }
        var activeCount = assessmentSessionRepository.countActiveByAssessmentId(assessmentId);
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
        var changed = 0;
        for (var schedule : assessmentScheduleRepository.findByAssessmentId(assessmentId)) {
            if (isTerminal(schedule.getStatus())) {
                continue;
            }
            // Chỉ ca ĐÃ CÔNG BỐ mới có thể đã thi thật: ca còn DRAFT thì học sinh chưa từng nhìn
            // thấy nó, dù khung giờ đã trôi qua -- gọi là COMPLETED là bịa ra một buổi thi không có.
            // Ca đang diễn ra không tới được đây vì guard chạy trước; nếu có thì nó đã bắt đầu thật
            // nên COMPLETED vẫn là nhãn đúng hơn CANCELLED.
            var alreadyRan = schedule.getStatus() == AssessmentScheduleStatus.PUBLISHED
                && (schedule.hasEndedAt(now) || schedule.isOngoingAt(now));
            var target = alreadyRan ? AssessmentScheduleStatus.COMPLETED : AssessmentScheduleStatus.CANCELLED;
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
        var changed = 0;
        for (var schedule : assessmentScheduleRepository.findByAssessmentId(assessmentId)) {
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
        var ended = assessmentScheduleRepository.findPublishedEndedBefore(now, limit);
        for (var schedule : ended) {
            apply(schedule, AssessmentScheduleStatus.COMPLETED, null, now);
        }
        return ended.size();
    }

    /**
     * COMPLETED/MOVED/CANCELLED là trạng thái kết thúc: ghi đè sẽ xoá dấu vết ca đã thi xong và làm
     * lệch {@code movedToScheduleId}. DELETED đã bị {@code findByAssessmentId} lọc sẵn. Cascade thì bỏ
     * qua chứ không ném lỗi.
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
