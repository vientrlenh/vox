package com.sep.vox.application.port.input.usecase.examgrading;

import java.time.Instant;
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

    /**
     * Trần một lượt thu hồi. Nhánh "không chỉ định" chạy trong một transaction GHI, nên
     * không có trần thì một trường tồn đọng lớn giữ connection và khoá rất lâu. Lấy dư
     * đúng một dòng để biết còn nữa hay không mà không cần thêm câu COUNT.
     */
    private static final int MAX_RECLAIM_BATCH = 500;

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

        var now = Instant.now();
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

        var batch = selectOverdue(command, schoolId, currentUserId, now);
        var overdue = batch.assignments();
        if (overdue.isEmpty()) {
            return ReclaimOverdueResponse.empty();
        }

        rejectConflictedReplacements(overdue, teacherIds);

        for (var assignment : overdue) {
            assignment.complete(GradingOutcome.DECLINED, RECLAIM_REASON, now);
            examGradingAssignmentRepository.save(assignment);
        }
        var reclaimedIds = overdue.stream().map(assignment -> assignment.getId()).toList();
        if (teacherIds.isEmpty()) {
            // Không có người thay: đơn phúc khảo phải được nhả ra, nếu không nó kẹt ở
            // GRADING mà chẳng còn phân công nào đang mở để chấm tiếp.
            overdue.forEach(this::releaseAppeal);
            return new ReclaimOverdueResponse(reclaimedIds, List.of(), batch.hasMore());
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
        return new ReclaimOverdueResponse(reclaimedIds, reassignedIds, batch.hasMore());
    }

    /**
     * Một lượt thu hồi cùng với câu trả lời "còn nữa không".
     *
     * @param hasMore đã chạm trần {@link #MAX_RECLAIM_BATCH}, còn dòng chưa xử lý
     */
    private record OverdueBatch(List<ExamGradingAssignment> assignments, boolean hasMore) {}

    /**
     * Vòng {@code APPEAL} chịu chung luật xung đột lợi ích với
     * {@code ReassignGradingUseCase}: người đã từng ghi phán quyết điểm cho bài không
     * được ngồi soi lại chính mình. Giao lại HÀNG LOẠT không được là cửa sau để lách
     * thứ mà giao lại từng dòng đã chặn — và sau khi
     * {@code AutoAssignGradingUseCase} cấm hẳn vòng phúc khảo, đây là cửa cuối cùng.
     *
     * <p>Chặn cả lô thay vì lọc lẻ từng người là lựa chọn cố ý: round-robin chia bài
     * sau khi danh sách đã chốt, nên lọc lẻ sẽ làm kết quả chia phụ thuộc vào dữ liệu
     * ẩn mà admin không nhìn thấy lúc bấm.
     *
     * <p>Chỉ hỏi khi trong lô thật sự có vòng phúc khảo — ba vòng còn lại không có luật
     * này, hỏi thừa là thêm query cho mọi lượt thu hồi.
     */
    private void rejectConflictedReplacements(List<ExamGradingAssignment> overdue, List<UUID> teacherIds) {
        if (teacherIds.isEmpty()) {
            return;
        }
        var appealResultIds = overdue.stream()
            .filter(assignment -> assignment.getRoundType() == GradingRoundType.APPEAL)
            .map(assignment -> assignment.getCandidateResultId())
            .distinct()
            .toList();
        for (var candidateResultId : appealResultIds) {
            var conflicted = examGradingQueryRepository.findTeacherIdsWithHumanEvaluation(candidateResultId);
            if (conflicted.stream().anyMatch(teacherIds::contains)) {
                throw new IllegalArgumentException(
                    "Danh sách giáo viên thay thế có người đã từng chấm một bài đang phúc khảo "
                        + "nên không được chấm phúc khảo bài đó. Bỏ người này ra, hoặc giao lại "
                        + "ở màn đơn phúc khảo nếu cần ghi lý do ngoại lệ.");
            }
        }
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
     *
     * <p>Nhánh đó lấy dư đúng MỘT dòng so với trần: nếu về đủ {@code MAX + 1} thì biết
     * chắc còn việc, mà không phải chạy thêm câu COUNT trên cùng vị ngữ. Danh sách chỉ
     * định thì admin đã tự chọn nên không áp trần.
     */
    private List<ExamGradingAssignment> excludeClassTest(List<ExamGradingAssignment> assignments) {
        if (assignments.isEmpty()) {
            return assignments;
        }
        var classTestResultIds = examGradingAccessService.classTestResultIds(
            assignments.stream().map(assignment -> assignment.getCandidateResultId()).toList());
        if (classTestResultIds.isEmpty()) {
            return assignments;
        }
        return assignments.stream()
            .filter(assignment -> !classTestResultIds.contains(assignment.getCandidateResultId()))
            .toList();
    }

    private OverdueBatch selectOverdue(
            ReclaimOverdueAssignmentsCommand command, UUID schoolId, UUID currentUserId, Instant now) {
        var assignmentIds = command.assignmentIds() == null ? List.<UUID>of() : command.assignmentIds();
        if (assignmentIds.isEmpty()) {
            var found = examGradingAssignmentRepository.findOverdueInSchool(
                now, schoolId, command.examId(), MAX_RECLAIM_BATCH + 1);
            // Nhánh "thu hồi cả trường": LỌC BỎ bài trên lớp chứ không ném. Ném ở đây là
            // một bài trên lớp quá hạn chặn đứng nút thu hồi của cả trường.
            found = excludeClassTest(found);
            return found.size() > MAX_RECLAIM_BATCH
                ? new OverdueBatch(found.subList(0, MAX_RECLAIM_BATCH), true)
                : new OverdueBatch(found, false);
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
        // Ngược lại với nhánh trên: admin tick đích danh từng dòng, nên im lặng bỏ qua
        // là để họ tưởng đã thu hồi. Ở đây phải nói thẳng.
        examGradingAccessService.rejectClassTestCoordination(
            selected.stream().map(assignment -> assignment.getCandidateResultId()).toList());
        return new OverdueBatch(selected, false);
    }
}
