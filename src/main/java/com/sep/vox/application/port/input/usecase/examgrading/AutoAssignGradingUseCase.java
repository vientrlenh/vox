package com.sep.vox.application.port.input.usecase.examgrading;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.AutoAssignGradingCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;

/**
 * Chia đều bài chưa gán cho nhóm giáo viên được chọn.
 *
 * <p>Round-robin ở đây là <em>cân bằng theo tải thật</em>, không phải chia vòng
 * tròn từ số 0: điểm xuất phát là số bài ASSIGNED mỗi người đang giữ, nên chạy lần
 * hai không dồn hết vào người đầu danh sách. Bài đã có phân công bị bỏ qua ở tầng
 * query, nên gọi lại nhiều lần là an toàn.
 */
@Service
public class AutoAssignGradingUseCase implements IUseCase<AutoAssignGradingCommand, List<UUID>> {

    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public AutoAssignGradingUseCase(
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional
    public List<UUID> execute(AutoAssignGradingCommand command) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var schoolId = examGradingAccessService.requireCurrentSchoolId(currentUserId);
        examGradingAccessService.authorizeSchoolAdmin(schoolId, currentUserId);

        if (command.examId() == null && command.scheduleId() == null) {
            throw new IllegalArgumentException("Phải chọn kỳ thi hoặc ca thi để phân công tự động.");
        }
        var teacherIds = command.teacherIds() == null ? List.<UUID>of() : command.teacherIds();
        if (teacherIds.isEmpty()) {
            throw new IllegalArgumentException("Phải chọn ít nhất một giáo viên để phân công.");
        }
        if (new HashSet<>(teacherIds).size() != teacherIds.size()) {
            throw new DuplicatedException("Không được chọn trùng giáo viên.");
        }
        // Một query cho cả nhóm thay vì isTeacherOfSchool lặp từng người (N+1).
        var validTeachers = examGradingQueryRepository.findTeacherIdsInSchool(schoolId, teacherIds);
        if (!validTeachers.containsAll(teacherIds)) {
            throw new IllegalArgumentException("Người chấm phải là giáo viên thuộc cùng trường với bài thi.");
        }

        var candidateResultIds = examGradingQueryRepository.findUnassignedPendingReviewResultIds(
            schoolId, command.examId(), command.scheduleId());
        if (candidateResultIds.isEmpty()) {
            return List.of();
        }

        var loads = currentLoads(teacherIds);
        var now = OffsetDateTime.now();
        var assignments = new ArrayList<ExamGradingAssignment>();
        for (var candidateResultId : candidateResultIds) {
            var target = leastLoadedIndex(loads);
            loads[target]++;
            assignments.add(new ExamGradingAssignment(
                candidateResultId,
                teacherIds.get(target),
                GradingAssignmentStatus.ASSIGNED,
                now,
                currentUserId,
                null
            ));
        }

        return examGradingAssignmentRepository.saveAll(assignments).stream()
            .map(assignment -> assignment.getId())
            .toList();
    }

    /** Tải hiện tại của đúng nhóm được chọn, lấy trong 1 query (tránh N+1). */
    private long[] currentLoads(List<UUID> teacherIds) {
        var loadByTeacher = examGradingQueryRepository.assignedLoadByTeacherIds(teacherIds);
        var loads = new long[teacherIds.size()];
        for (var index = 0; index < teacherIds.size(); index++) {
            loads[index] = loadByTeacher.getOrDefault(teacherIds.get(index), 0L);
        }
        return loads;
    }

    /** Hoà thì người đứng trước thắng — số bài dư rải tuần tự theo thứ tự đã chọn. */
    private int leastLoadedIndex(long[] loads) {
        var best = 0;
        for (var index = 1; index < loads.length; index++) {
            if (loads[index] < loads[best]) {
                best = index;
            }
        }
        return best;
    }
}
