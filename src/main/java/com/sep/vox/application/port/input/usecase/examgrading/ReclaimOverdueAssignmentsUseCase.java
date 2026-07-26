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
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;

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
        implements IUseCase<ReclaimOverdueAssignmentsCommand, List<UUID>> {

    private static final String RECLAIM_REASON = "Thu hồi do quá hạn chấm.";

    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;
    private final RoundRobinLoadBalancer roundRobinLoadBalancer;

    public ReclaimOverdueAssignmentsUseCase(
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamGradingAccessService examGradingAccessService,
            RoundRobinLoadBalancer roundRobinLoadBalancer) {
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
        this.roundRobinLoadBalancer = roundRobinLoadBalancer;
    }

    @Override
    @Transactional
    public List<UUID> execute(ReclaimOverdueAssignmentsCommand command) {
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
            return List.of();
        }

        for (var assignment : overdue) {
            assignment.complete(GradingOutcome.DECLINED, RECLAIM_REASON, now);
            examGradingAssignmentRepository.save(assignment);
        }
        if (teacherIds.isEmpty()) {
            return overdue.stream().map(assignment -> assignment.getId()).toList();
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
        return examGradingAssignmentRepository.saveAll(reopened).stream()
            .map(assignment -> assignment.getId())
            .toList();
    }

    /**
     * Danh sách chỉ định thì phân quyền + kiểm quá hạn từng dòng; không chỉ định thì
     * quét toàn bộ dòng quá hạn rồi lọc theo kỳ thi của phạm vi được phép.
     */
    private List<ExamGradingAssignment> selectOverdue(
            ReclaimOverdueAssignmentsCommand command, UUID schoolId, UUID currentUserId, OffsetDateTime now) {
        var assignmentIds = command.assignmentIds() == null ? List.<UUID>of() : command.assignmentIds();
        if (!assignmentIds.isEmpty()) {
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

        var overdue = examGradingAssignmentRepository.findOverdue(now);
        if (overdue.isEmpty()) {
            return List.of();
        }
        // Lọc lại theo trường (và kỳ thi nếu có) qua chính chuỗi phân quyền, để không
        // thu hồi nhầm bài của trường khác khi quét toàn hệ thống.
        var filtered = new ArrayList<ExamGradingAssignment>();
        for (var assignment : overdue) {
            var context = examGradingAccessService.load(assignment.getId());
            if (!schoolId.equals(context.schoolId())) {
                continue;
            }
            if (command.examId() != null && !command.examId().equals(context.candidateResult().getExamId())) {
                continue;
            }
            filtered.add(context.assignment());
        }
        return filtered;
    }
}
