package com.sep.vox.application.port.input.usecase.assessmentgrading;

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

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.AssignGradingCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.GradingAssignmentNotificationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentGradingRoundType;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.service.assessment.GradingRoundPolicy;

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
 * đi qua {@code AssignAssessmentAppealReviewerUseCase}, chỗ duy nhất biết luật xung đột
 * lợi ích.
 */
@Service
public class AssignGradingUseCase implements IUseCase<AssignGradingCommand, List<UUID>> {

    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;
    private final GradingAssignmentNotificationService gradingAssignmentNotificationService;

    public AssignGradingUseCase(
            AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentRepository assessmentRepository,
            AssessmentGradingQueryRepository assessmentGradingQueryRepository,
            AssessmentGradingAccessService assessmentGradingAccessService,
            GradingAssignmentNotificationService gradingAssignmentNotificationService) {
        this.assessmentGradingAssignmentRepository = assessmentGradingAssignmentRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentGradingQueryRepository = assessmentGradingQueryRepository;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
        this.gradingAssignmentNotificationService = gradingAssignmentNotificationService;
    }

    @Override
    @Transactional
    public List<UUID> execute(AssignGradingCommand command) {
        var currentUserId = assessmentGradingAccessService.requireActiveUserId();
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
            throw new ResourceDuplicatedException("Không được phân công trùng bài thi.");
        }

        // ---- nạp theo lô: kết quả -> kỳ thi -> trường ----
        var resultsById = assessmentCandidateResultRepository.findByIdIn(candidateResultIds).stream()
            .collect(Collectors.toMap(result -> result.getId(), Function.identity(), (left, right) -> left));
        for (var candidateResultId : candidateResultIds) {
            if (!resultsById.containsKey(candidateResultId)) {
                throw new ResourceNotFoundException("Không tìm thấy kết quả bài thi.");
            }
        }
        var schoolIdByAssessmentId = assessmentRepository.findByIdIn(
                resultsById.values().stream().map(result -> result.getAssessmentId()).distinct().toList()).stream()
            .collect(Collectors.toMap(assessment -> assessment.getId(), assessment -> assessment.getSchoolId(), (left, right) -> left));

        // Trường của mỗi bài; đồng thời là tập trường phân biệt để phân quyền + lọc giáo viên.
        var schoolIdByResultId = new HashMap<UUID, UUID>();
        for (var result : resultsById.values()) {
            var schoolId = schoolIdByAssessmentId.get(result.getAssessmentId());
            if (schoolId == null) {
                throw new ResourceNotFoundException("Không tìm thấy bài kiểm tra của kết quả này.");
            }
            schoolIdByResultId.put(result.getId(), schoolId);
        }
        var distinctSchoolIds = new HashSet<>(schoolIdByResultId.values());

        // ---- phân quyền: một lần / trường phân biệt (thường là một) ----
        for (var schoolId : distinctSchoolIds) {
            assessmentGradingAccessService.authorizeSchoolAdmin(schoolId, currentUserId);
        }
        // Bài trên lớp đi đường riêng: chủ bài tự nhận qua /class-tests/{id}/grading/claim.
        assessmentGradingAccessService.rejectClassTestCoordination(candidateResultIds);

        // ---- đã có phân công ĐANG MỞ? một query cho cả lô ----
        var alreadyAssigned = assessmentGradingAssignmentRepository
            .findOpenByCandidateResultIdIn(candidateResultIds).stream()
            .map(assignment -> assignment.getCandidateResultId())
            .collect(Collectors.toSet());

        // ---- giáo viên hợp lệ theo từng trường, một query / trường ----
        var teacherIds = items.stream()
            .map(item -> item.teacherId()).distinct().toList();
        var validTeachersBySchool = new HashMap<UUID, Set<UUID>>();
        for (var schoolId : distinctSchoolIds) {
            validTeachersBySchool.put(schoolId,
                assessmentGradingQueryRepository.findTeacherIdsInSchool(schoolId, teacherIds));
        }

        var now = Instant.now();
        var assignments = new ArrayList<AssessmentGradingAssignment>();
        for (var item : items) {
            var result = resultsById.get(item.candidateResultId());
            if (!GradingRoundPolicy.isAssignable(roundType, result.getStatus())) {
                throw new IllegalStateException(
                    "Bài thi không ở trạng thái phù hợp với vòng chấm " + roundType.name() + ".");
            }
            // Pre-check để báo lỗi tiếng Việt thay vì để unique index ném
            // DataIntegrityViolation; index vẫn là chốt cuối khi có race.
            if (alreadyAssigned.contains(item.candidateResultId())) {
                throw new ResourceDuplicatedException("Bài thi này đang được một giáo viên chấm.");
            }
            var schoolId = schoolIdByResultId.get(item.candidateResultId());
            if (!validTeachersBySchool.getOrDefault(schoolId, Set.of()).contains(item.teacherId())) {
                throw new IllegalArgumentException("Người chấm phải là giáo viên thuộc cùng trường với bài thi.");
            }

            // scoreBefore chụp NGAY LÚC GIAO: đây là mốc để đo "AI/vòng trước lệch bao
            // nhiêu" sau khi giáo viên chấm xong. Lấy sau thì đã bị chính họ sửa mất.
            assignments.add(AssessmentGradingAssignment.open(
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

        var saved = assessmentGradingAssignmentRepository.saveAll(assignments);

        // Sau saveAll: thông báo cần id của dòng phân công để dẫn giáo viên vào đúng bài.
        gradingAssignmentNotificationService.publishAssigned(saved, now);

        return saved.stream()
            .map(assignment -> assignment.getId())
            .toList();
    }

    private AssessmentGradingRoundType parseRoundType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Phải chọn vòng chấm.");
        }
        AssessmentGradingRoundType roundType;
        try {
            roundType = AssessmentGradingRoundType.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("Vòng chấm không hợp lệ.");
        }
        if (roundType == AssessmentGradingRoundType.APPEAL) {
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
