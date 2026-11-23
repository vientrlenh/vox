package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentgrading.AssignGradingCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.GradingAssignmentNotificationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.GradingRoundPolicy;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

/**
 * The school admin manually assigns teachers to each attempt, in one of the four grading rounds. Accepts a batch
 * so the admin can tick several rows and assign them at once.
 *
 * <p>Validates everything before writing: a wrong row must not leave a half-done assignment
 * from the rows before it.
 *
 * <p>Every lookup is batched so there is no N+1 by number of attempts: results and assessments are loaded in batches,
 * the already-assigned check is one query, and teacher validity is grouped by <em>school</em>
 * (not calling {@code isTeacherOfSchool} repeatedly per person). Permission checking also runs
 * only once per distinct school -- usually one.
 *
 * <p>The {@code APPEAL} round is NOT assigned through here: it is tied to a specific appeal so it
 * goes through {@code AssignAssessmentAppealReviewerUseCase}, the only place that knows the
 * conflict-of-interest rule.
 */
@Service
@RequiredArgsConstructor
public class AssignGradingUseCase implements IUseCase<AssignGradingCommand, List<UUID>> {

    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;
    private final GradingAssignmentNotificationService gradingAssignmentNotificationService;

    @Override
    @Transactional
    public List<UUID> execute(AssignGradingCommand command) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        GradingRoundType roundType = requireNotAppealRound(command.roundType());
        Instant deadlineAt = validateDeadline(command.deadlineAt());

        List<AssignGradingCommand.AssignmentItem> items = command.assignments() == null
            ? List.<AssignGradingCommand.AssignmentItem>of() : command.assignments();
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Phải chọn ít nhất một bài thi để phân công.");
        }
        for (AssignGradingCommand.AssignmentItem item : items) {
            if (item.candidateResultId() == null || item.teacherId() == null) {
                throw new IllegalArgumentException("Thiếu bài thi hoặc giáo viên cần phân công.");
            }
        }
        List<UUID> candidateResultIds = items.stream()
            .map(item -> item.candidateResultId()).toList();
        if (new HashSet<>(candidateResultIds).size() != candidateResultIds.size()) {
            throw new ResourceDuplicatedException("Không được phân công trùng bài thi.");
        }

        // ---- nạp theo lô: kết quả -> kỳ thi -> trường ----
        Map<UUID, AssessmentCandidateResult> resultsById = assessmentCandidateResultRepository.findByIdIn(candidateResultIds).stream()
            .collect(Collectors.toMap(result -> result.getId(), Function.identity(), (left, right) -> left));
        for (UUID candidateResultId : candidateResultIds) {
            if (!resultsById.containsKey(candidateResultId)) {
                throw new ResourceNotFoundException("Không tìm thấy kết quả bài thi.");
            }
        }
        Map<UUID, UUID> schoolIdByAssessmentId = assessmentRepository.findByIdIn(
                resultsById.values().stream().map(result -> result.getAssessmentId()).distinct().toList()).stream()
            .collect(Collectors.toMap(assessment -> assessment.getId(), assessment -> assessment.getSchoolId(), (left, right) -> left));

        // Trường của mỗi bài; đồng thời là tập trường phân biệt để phân quyền + lọc giáo viên.
        Map<UUID, UUID> schoolIdByResultId = new HashMap<UUID, UUID>();
        for (AssessmentCandidateResult result : resultsById.values()) {
            UUID schoolId = schoolIdByAssessmentId.get(result.getAssessmentId());
            if (schoolId == null) {
                throw new ResourceNotFoundException("Không tìm thấy bài kiểm tra của kết quả này.");
            }
            schoolIdByResultId.put(result.getId(), schoolId);
        }
        Set<UUID> distinctSchoolIds = new HashSet<>(schoolIdByResultId.values());

        // ---- phân quyền: một lần / trường phân biệt (thường là một) ----
        for (UUID schoolId : distinctSchoolIds) {
            assessmentGradingAccessService.authorizeSchoolAdmin(schoolId, currentUserId);
        }
        // Bài trên lớp đi đường riêng: chủ bài tự nhận qua /class-tests/{id}/grading/claim.
        assessmentGradingAccessService.rejectClassTestCoordination(candidateResultIds);

        // ---- đã có phân công ĐANG MỞ? một query cho cả lô ----
        Set<UUID> alreadyAssigned = assessmentGradingAssignmentRepository
            .findOpenByCandidateResultIdIn(candidateResultIds).stream()
            .map(assignment -> assignment.getCandidateResultId())
            .collect(Collectors.toSet());

        // ---- giáo viên hợp lệ theo từng trường, một query / trường ----
        List<UUID> teacherIds = items.stream()
            .map(item -> item.teacherId()).distinct().toList();
        Map<UUID, Set<UUID>> validTeachersBySchool = new HashMap<UUID, Set<UUID>>();
        for (UUID schoolId : distinctSchoolIds) {
            validTeachersBySchool.put(schoolId,
                assessmentGradingQueryRepository.findTeacherIdsInSchool(schoolId, teacherIds));
        }

        Instant now = Instant.now();
        List<AssessmentGradingAssignment> assignments = new ArrayList<AssessmentGradingAssignment>();
        for (AssignGradingCommand.AssignmentItem item : items) {
            AssessmentCandidateResult result = resultsById.get(item.candidateResultId());
            if (!GradingRoundPolicy.isAssignable(roundType, result.getStatus())) {
                throw new IllegalArgumentException(
                    "Bài thi không ở trạng thái phù hợp với vòng chấm " + roundType.name() + ".");
            }
            // Pre-check để báo lỗi tiếng Việt thay vì để unique index ném
            // DataIntegrityViolation; index vẫn là chốt cuối khi có race.
            if (alreadyAssigned.contains(item.candidateResultId())) {
                throw new ResourceDuplicatedException("Bài thi này đang được một giáo viên chấm.");
            }
            UUID schoolId = schoolIdByResultId.get(item.candidateResultId());
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

        List<AssessmentGradingAssignment> saved = assessmentGradingAssignmentRepository.saveAll(assignments);

        // Sau saveAll: thông báo cần id của dòng phân công để dẫn giáo viên vào đúng bài.
        gradingAssignmentNotificationService.publishAssigned(saved, now);

        return saved.stream()
            .map(assignment -> assignment.getId())
            .toList();
    }

    private GradingRoundType requireNotAppealRound(GradingRoundType roundType) {
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
