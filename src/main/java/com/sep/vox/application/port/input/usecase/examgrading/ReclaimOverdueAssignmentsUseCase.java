package com.sep.vox.application.port.input.usecase.examgrading;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.ReclaimOverdueAssignmentsCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.service.RoundRobinLoadBalancer;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.application.response.input.examgrading.ReclaimOverdueResponse;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;

/**
 * Thu hồi phân công quá hạn, và giao lại ngay nếu admin đã chọn người thay.
 *
 * <p>Thu hồi = đóng dòng cũ với {@code outcome = DECLINED} chứ không xoá: cần giữ vết
 * "người này đã được giao và không làm kịp", cả để giao lại cho đúng người lẫn để
 * nhìn ra ai đang quá tải.
 *
 * <p>Đóng dòng cũ TRƯỚC rồi mới mở dòng mới, và {@code save} của adapter flush ngay
 * — nếu không, unique index trên {@code active_result_id} sẽ chặn dòng mới vì dòng cũ
 * chưa kịp nhả chỗ.
 */
@Service
public class ReclaimOverdueAssignmentsUseCase
        implements IUseCase<ReclaimOverdueAssignmentsCommand, ReclaimOverdueResponse> {

    private static final String RECLAIM_REASON = "Thu hồi do quá hạn chấm.";

    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamResultAppealRepository examResultAppealRepository;
    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;
    private final RoundRobinLoadBalancer roundRobinLoadBalancer;

    public ReclaimOverdueAssignmentsUseCase(
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamResultAppealRepository examResultAppealRepository,
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamGradingAccessService examGradingAccessService,
            RoundRobinLoadBalancer roundRobinLoadBalancer) {
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examResultAppealRepository = examResultAppealRepository;
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
        this.roundRobinLoadBalancer = roundRobinLoadBalancer;
    }

    @Override
    @Transactional
    public ReclaimOverdueResponse execute(ReclaimOverdueAssignmentsCommand command) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var schoolId = examGradingAccessService.requireCurrentSchoolId(currentUserId);
        examGradingAccessService.authorizeSchoolAdmin(schoolId, currentUserId);

        var now = OffsetDateTime.now();
        if (command.newDeadlineAt() != null && command.newDeadlineAt().isBefore(now)) {
            throw new IllegalArgumentException("Hạn chấm mới phải ở tương lai.");
        }

        var teacherIds = command.reassignToTeacherIds() == null
            ? List.<UUID>of() : command.reassignToTeacherIds();
        if (!teacherIds.isEmpty()) {
            if (new HashSet<>(teacherIds).size() != teacherIds.size()) {
                throw new DuplicatedException("Không được chọn trùng giáo viên.");
            }
            var validTeachers = examGradingQueryRepository.findTeacherIdsInSchool(schoolId, teacherIds);
            if (!validTeachers.containsAll(teacherIds)) {
                throw new IllegalArgumentException("Người chấm phải là giáo viên thuộc cùng trường với bài thi.");
            }
        }

        var overdue = selectOverdue(command, schoolId, currentUserId, now);
        if (overdue.isEmpty()) {
            return ReclaimOverdueResponse.empty();
        }

        for (var assignment : overdue) {
            assignment.complete(GradingOutcome.DECLINED, RECLAIM_REASON, now);
            examGradingAssignmentRepository.save(assignment);
        }
        var reclaimedIds = overdue.stream().map(assignment -> assignment.getId()).toList();
        if (teacherIds.isEmpty()) {
            // Không có người thay: đơn phúc khảo phải được nhả ra, nếu không nó kẹt ở
            // GRADING mà chẳng còn phân công nào đang mở để chấm tiếp.
            overdue.forEach(this::releaseAppeal);
            return new ReclaimOverdueResponse(reclaimedIds, List.of());
        }

        // Tải được đọc SAU khi đóng dòng cũ, nên người vừa bị thu hồi không còn bị tính
        // là đang giữ những bài đó — chia lại mới công bằng.
        var loads = examGradingQueryRepository.assignedLoadByTeacherIds(teacherIds);
        var workItems = overdue.stream().map(assignment -> assignment.getCandidateResultId()).toList();
        var picked = roundRobinLoadBalancer.distribute(workItems, teacherIds, loads);

        var reopened = new ArrayList<ExamGradingAssignment>();
        for (var index = 0; index < overdue.size(); index++) {
            var previous = overdue.get(index);
            reopened.add(ExamGradingAssignment.open(
                previous.getCandidateResultId(),
                picked.get(index),
                previous.getRoundType(),
                previous.getAppealId(),
                previous.getScoreBefore(),
                now,
                currentUserId,
                command.newDeadlineAt()
            ));
        }
        var reassignedIds = examGradingAssignmentRepository.saveAll(reopened).stream()
            .map(assignment -> assignment.getId())
            .toList();
        return new ReclaimOverdueResponse(reclaimedIds, reassignedIds);
    }

    /**
     * Nhả đơn phúc khảo về {@code APPROVED} để admin giao lại được. Chỉ gọi ở nhánh
     * thu hồi suông: khi có người thay, dòng mới mở ngay với cùng {@code appealId}
     * trong cùng transaction nên đơn ở {@code GRADING} vẫn đúng.
     *
     * <p>Ba vòng còn lại không gắn đơn nên là no-op.
     */
    private void releaseAppeal(ExamGradingAssignment assignment) {
        if (assignment.getRoundType() != GradingRoundType.APPEAL || assignment.getAppealId() == null) {
            return;
        }
        examResultAppealRepository.findById(assignment.getAppealId())
            .filter(appeal -> appeal.getStatus() == ExamAppealStatus.GRADING)
            .ifPresent(appeal -> {
                appeal.setStatus(ExamAppealStatus.APPROVED);
                examResultAppealRepository.save(appeal);
            });
    }

    /**
     * Danh sách chỉ định thì phân quyền + kiểm quá hạn từng dòng; không chỉ định thì
     * để SQL lọc sẵn theo trường (và kỳ thi nếu có).
     *
     * <p>Nhánh "không chỉ định" trước đây quét {@code findOverdue} toàn hệ thống rồi
     * gọi {@code load()} từng dòng chỉ để đọc {@code schoolId} — 4 query mỗi dòng, và
     * chạm cả dữ liệu trường khác. Bộ lọc đã xuống SQL nên ở đây là MỘT query.
     */
    private List<ExamGradingAssignment> selectOverdue(
            ReclaimOverdueAssignmentsCommand command, UUID schoolId, UUID currentUserId, OffsetDateTime now) {
        var assignmentIds = command.assignmentIds() == null ? List.<UUID>of() : command.assignmentIds();
        if (assignmentIds.isEmpty()) {
            return examGradingAssignmentRepository.findOverdueInSchool(now, schoolId, command.examId());
        }

        var selected = new ArrayList<ExamGradingAssignment>();
        for (var assignmentId : assignmentIds) {
            var context = examGradingAccessService.load(assignmentId);
            examGradingAccessService.authorizeSchoolAdmin(context.schoolId(), currentUserId);
            if (!context.assignment().isOverdue(now)) {
                throw new IllegalStateException("Chỉ thu hồi được phân công đang mở và đã quá hạn.");
            }
            selected.add(context.assignment());
        }
        return selected;
    }
}
