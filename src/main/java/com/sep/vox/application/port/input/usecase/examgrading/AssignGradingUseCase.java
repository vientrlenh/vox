package com.sep.vox.application.port.input.usecase.examgrading;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AssignGradingCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamRepository;

/**
 * School admin gán tay giáo viên cho từng bài. Nhận batch để admin tick nhiều dòng
 * rồi gán một lần.
 *
 * <p>Validate hết rồi mới ghi: một dòng sai không được để lại phân công nửa vời
 * của các dòng trước đó.
 *
 * <p>Mọi lookup đều batch để không N+1 theo số bài: kết quả và kỳ thi nạp theo lô,
 * kiểm tra đã-phân-công một query, và tính hợp lệ giáo viên gom theo <em>trường</em>
 * (không gọi {@code isTeacherOfSchool} lặp từng người). Phân quyền cũng chỉ chạy
 * một lần cho mỗi trường phân biệt — thường là một.
 */
@Service
public class AssignGradingUseCase implements IUseCase<AssignGradingCommand, List<UUID>> {

    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamRepository examRepository;
    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public AssignGradingUseCase(
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamRepository examRepository,
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examRepository = examRepository;
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional
    public List<UUID> execute(AssignGradingCommand command) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var items = command.assignments() == null
            ? List.<AssignGradingCommand.AssignmentItem>of() : command.assignments();
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Phải chọn ít nhất một bài thi để phân công.");
        }
        for (var item : items) {
            if (item.candidateResultId() == null || item.teacherId() == null) {
                throw new IllegalArgumentException("Thiếu bài thi hoặc giáo viên cần phân công.");
            }
        }
        var candidateResultIds = items.stream()
            .map(item -> item.candidateResultId()).toList();
        if (new HashSet<>(candidateResultIds).size() != candidateResultIds.size()) {
            throw new DuplicatedException("Không được phân công trùng bài thi.");
        }

        // ---- nạp theo lô: kết quả -> kỳ thi -> trường ----
        var resultsById = examCandidateResultRepository.findByIdIn(candidateResultIds).stream()
            .collect(Collectors.toMap(result -> result.getId(), Function.identity(), (left, right) -> left));
        for (var candidateResultId : candidateResultIds) {
            if (!resultsById.containsKey(candidateResultId)) {
                throw new NotFoundException("Không tìm thấy kết quả bài thi.");
            }
        }
        var schoolIdByExamId = examRepository.findByIdIn(
                resultsById.values().stream().map(result -> result.getExamId()).distinct().toList()).stream()
            .collect(Collectors.toMap(exam -> exam.getId(), exam -> exam.getSchoolId(), (left, right) -> left));

        // Trường của mỗi bài; đồng thời là tập trường phân biệt để phân quyền + lọc giáo viên.
        var schoolIdByResultId = new HashMap<UUID, UUID>();
        for (var result : resultsById.values()) {
            var schoolId = schoolIdByExamId.get(result.getExamId());
            if (schoolId == null) {
                throw new NotFoundException("Không tìm thấy bài kiểm tra của kết quả này.");
            }
            schoolIdByResultId.put(result.getId(), schoolId);
        }
        var distinctSchoolIds = new HashSet<>(schoolIdByResultId.values());

        // ---- phân quyền: một lần / trường phân biệt (thường là một) ----
        for (var schoolId : distinctSchoolIds) {
            examGradingAccessService.authorizeSchoolAdmin(schoolId, currentUserId);
        }

        // ---- đã có phân công? một query cho cả lô ----
        var alreadyAssigned = examGradingAssignmentRepository.findByCandidateResultIdIn(candidateResultIds).stream()
            .map(assignment -> assignment.getCandidateResultId())
            .collect(Collectors.toSet());

        // ---- giáo viên hợp lệ theo từng trường, một query / trường ----
        var teacherIds = items.stream()
            .map(item -> item.teacherId()).distinct().toList();
        var validTeachersBySchool = new HashMap<UUID, Set<UUID>>();
        for (var schoolId : distinctSchoolIds) {
            validTeachersBySchool.put(schoolId,
                examGradingQueryRepository.findTeacherIdsInSchool(schoolId, teacherIds));
        }

        var now = OffsetDateTime.now();
        var assignments = new ArrayList<ExamGradingAssignment>();
        for (var item : items) {
            var result = resultsById.get(item.candidateResultId());
            if (result.getStatus() != ExamCandidateResultStatus.PENDING_REVIEW) {
                throw new IllegalStateException("Chỉ phân công được bài thi đang chờ chấm.");
            }
            // Pre-check để báo lỗi tiếng Việt thay vì để unique index ném
            // DataIntegrityViolation; index vẫn là chốt cuối khi có race.
            if (alreadyAssigned.contains(item.candidateResultId())) {
                throw new DuplicatedException("Bài thi này đã được phân công cho một giáo viên.");
            }
            var schoolId = schoolIdByResultId.get(item.candidateResultId());
            if (!validTeachersBySchool.getOrDefault(schoolId, Set.of()).contains(item.teacherId())) {
                throw new IllegalArgumentException("Người chấm phải là giáo viên thuộc cùng trường với bài thi.");
            }

            assignments.add(new ExamGradingAssignment(
                item.candidateResultId(),
                item.teacherId(),
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
}
