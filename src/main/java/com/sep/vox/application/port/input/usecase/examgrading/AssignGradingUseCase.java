package com.sep.vox.application.port.input.usecase.examgrading;

import java.time.Instant;
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
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.GradingRoundPolicy;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamRepository;

/**
 * School admin gán tay giáo viên cho từng bài, ở một trong bốn vòng chấm. Nhận batch
 * để admin tick nhiều dòng rồi gán một lần.
 *
 * <p>Validate hết rồi mới ghi: một dòng sai không được để lại phân công nửa vời
 * của các dòng trước đó.
 *
 * <p>Mọi lookup đều batch để không N+1 theo số bài: kết quả và kỳ thi nạp theo lô,
 * kiểm tra đã-phân-công một query, và tính hợp lệ giáo viên gom theo <em>trường</em>
 * (không gọi {@code isTeacherOfSchool} lặp từng người). Phân quyền cũng chỉ chạy
 * một lần cho mỗi trường phân biệt — thường là một.
 *
 * <p>Vòng {@code APPEAL} KHÔNG giao qua đây: nó gắn với một đơn phúc khảo cụ thể nên
 * đi qua {@code AssignExamAppealReviewerUseCase}, chỗ duy nhất biết luật xung đột
 * lợi ích.
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
        var roundType = parseRoundType(command.roundType());
        var deadlineAt = validateDeadline(command.deadlineAt());

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

        // ---- đã có phân công ĐANG MỞ? một query cho cả lô ----
        var alreadyAssigned = examGradingAssignmentRepository
            .findOpenByCandidateResultIdIn(candidateResultIds).stream()
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

        var now = Instant.now();
        var assignments = new ArrayList<ExamGradingAssignment>();
        for (var item : items) {
            var result = resultsById.get(item.candidateResultId());
            if (!GradingRoundPolicy.isAssignable(roundType, result.getStatus())) {
                throw new IllegalStateException(
                    "Bài thi không ở trạng thái phù hợp với vòng chấm " + roundType.name() + ".");
            }
            // Pre-check để báo lỗi tiếng Việt thay vì để unique index ném
            // DataIntegrityViolation; index vẫn là chốt cuối khi có race.
            if (alreadyAssigned.contains(item.candidateResultId())) {
                throw new DuplicatedException("Bài thi này đang được một giáo viên chấm.");
            }
            var schoolId = schoolIdByResultId.get(item.candidateResultId());
            if (!validTeachersBySchool.getOrDefault(schoolId, Set.of()).contains(item.teacherId())) {
                throw new IllegalArgumentException("Người chấm phải là giáo viên thuộc cùng trường với bài thi.");
            }

            // scoreBefore chụp NGAY LÚC GIAO: đây là mốc để đo "AI/vòng trước lệch bao
            // nhiêu" sau khi giáo viên chấm xong. Lấy sau thì đã bị chính họ sửa mất.
            assignments.add(ExamGradingAssignment.open(
                item.candidateResultId(),
                item.teacherId(),
                roundType,
                null,
                result.getTotalScore(),
                now,
                currentUserId,
                deadlineAt
            ));
        }

        return examGradingAssignmentRepository.saveAll(assignments).stream()
            .map(assignment -> assignment.getId())
            .toList();
    }

    private GradingRoundType parseRoundType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Phải chọn vòng chấm.");
        }
        GradingRoundType roundType;
        try {
            roundType = GradingRoundType.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("Vòng chấm không hợp lệ.");
        }
        if (roundType == GradingRoundType.APPEAL) {
            throw new IllegalArgumentException(
                "Vòng phúc khảo được phân công từ màn đơn phúc khảo, không phân công ở đây.");
        }
        return roundType;
    }

    private Instant validateDeadline(Instant deadlineAt) {
        if (deadlineAt != null && deadlineAt.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Hạn chấm phải ở tương lai.");
        }
        return deadlineAt;
    }
}
