package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.application.query.dto.GradingRiskInfo;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.GradingAssignmentNotificationService;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.AutoAssignGradingCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.GradingSampleSelector;
import com.sep.vox.application.port.input.service.RoundRobinLoadBalancer;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.GradingRoundPolicy;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.assessment.GradingSampleSelectionMode;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;

/**
 * Chọn một tập bài rồi chia đều cho nhóm giáo viên được chọn.
 *
 * <p>Hai phần tách bạch, mỗi phần một service test được riêng: {@link GradingSampleSelector}
 * quyết <em>chấm bài nào</em> (4 chế độ), {@link RoundRobinLoadBalancer} quyết
 * <em>ai chấm</em> (cân theo tải thật, không chia vòng tròn từ số 0 nên chạy lần hai
 * không dồn vào người đầu danh sách).
 *
 * <p>Bài đã có phân công đang mở bị loại ở tầng query, nên gọi lại nhiều lần là an toàn.
 */
@Service
public class AutoAssignGradingUseCase implements IUseCase<AutoAssignGradingCommand, List<UUID>> {

    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;
    private final GradingSampleSelector gradingSampleSelector;
    private final RoundRobinLoadBalancer roundRobinLoadBalancer;
    private final GradingAssignmentNotificationService gradingAssignmentNotificationService;

    public AutoAssignGradingUseCase(
            AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentGradingQueryRepository assessmentGradingQueryRepository,
            AssessmentGradingAccessService assessmentGradingAccessService,
            GradingSampleSelector gradingSampleSelector,
            RoundRobinLoadBalancer roundRobinLoadBalancer,
            GradingAssignmentNotificationService gradingAssignmentNotificationService) {
        this.assessmentGradingAssignmentRepository = assessmentGradingAssignmentRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentGradingQueryRepository = assessmentGradingQueryRepository;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
        this.gradingSampleSelector = gradingSampleSelector;
        this.roundRobinLoadBalancer = roundRobinLoadBalancer;
        this.gradingAssignmentNotificationService = gradingAssignmentNotificationService;
    }

    @Override
    @Transactional
    public List<UUID> execute(AutoAssignGradingCommand command) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        UUID schoolId = assessmentGradingAccessService.requireCurrentSchoolId(currentUserId);
        assessmentGradingAccessService.authorizeSchoolAdmin(schoolId, currentUserId);

        if (command.assessmentId() == null && command.scheduleId() == null) {
            throw new IllegalArgumentException("Phải chọn kỳ thi hoặc ca thi để phân công tự động.");
        }
        // Blocked right at assessmentId rather than waiting for the attempt list: a class test has already been assigned
        // entirely to its owner so the list would be EMPTY, and "empty" returns success silently --
        // the admin thinks the assignment is done while nothing happened.
        if (assessmentGradingAccessService.isClassTestAssessment(command.assessmentId())) {
            throw new ForbiddenException(AssessmentGradingAccessService.CLASS_TEST_COORDINATION_REJECTION);
        }
        GradingRoundType roundType = requireNotAppealRound(command.roundType());
        GradingSampleSelectionMode selectionMode = command.selectionMode() == null
            ? GradingSampleSelectionMode.ALL
            : command.selectionMode();
        Instant deadlineAt = validateDeadline(command.deadlineAt());

        List<UUID> teacherIds = command.teacherIds() == null ? List.<UUID>of() : command.teacherIds();
        if (teacherIds.isEmpty()) {
            throw new IllegalArgumentException("Phải chọn ít nhất một giáo viên để phân công.");
        }
        if (new HashSet<>(teacherIds).size() != teacherIds.size()) {
            throw new DuplicatedException("Không được chọn trùng giáo viên.");
        }
        // Một query cho cả nhóm thay vì isTeacherOfSchool lặp từng người (N+1).
        Set<UUID> validTeachers = assessmentGradingQueryRepository.findTeacherIdsInSchool(schoolId, teacherIds);
        if (!validTeachers.containsAll(teacherIds)) {
            throw new IllegalArgumentException("Người chấm phải là giáo viên thuộc cùng trường với bài thi.");
        }

        List<String> eligibleStatuses = GradingRoundPolicy.assignableStatuses(roundType).stream()
            .map(candidateResultStatus -> candidateResultStatus.name())
            .toList();
        List<UUID> candidates = assessmentGradingQueryRepository.findAssignableResultIds(
            schoolId, command.assessmentId(), command.scheduleId(), eligibleStatuses);
        if (candidates.isEmpty()) {
            return List.of();
        }
        // The entry by scheduleId does not go through the assessmentId guard above.
        assessmentGradingAccessService.rejectClassTestCoordination(candidates);

        // Chỉ nạp tín hiệu rủi ro khi chế độ thật sự cần — một query nặng chạy không
        // công cho ba chế độ còn lại là lãng phí thấy rõ.
        List<GradingRiskInfo> riskInfos = selectionMode == GradingSampleSelectionMode.RISK_BASED
            ? assessmentGradingQueryRepository.findRiskInfos(candidates)
            : List.<com.sep.vox.application.query.dto.GradingRiskInfo>of();
        List<UUID> selected = gradingSampleSelector.select(
            selectionMode, candidates, command.percent(), command.candidateResultIds(), riskInfos);
        if (selected.isEmpty()) {
            return List.of();
        }

        Map<UUID, Long> loads = assessmentGradingQueryRepository.assignedLoadByTeacherIds(teacherIds);
        List<UUID> picked = roundRobinLoadBalancer.distribute(selected, teacherIds, loads);

        // scoreBefore chụp lúc giao, batch một query — mốc đo độ lệch sau khi chấm.
        Map<UUID, AssessmentCandidateResult> scoreByResultId = assessmentCandidateResultRepository.findByIdIn(selected).stream()
            .collect(Collectors.toMap(result -> result.getId(), Function.identity(), (left, right) -> left));

        Instant now = Instant.now();
        List<AssessmentGradingAssignment> assignments = new ArrayList<AssessmentGradingAssignment>();
        for (int index = 0; index < selected.size(); index++) {
            UUID candidateResultId = selected.get(index);
            AssessmentCandidateResult result = scoreByResultId.get(candidateResultId);
            assignments.add(AssessmentGradingAssignment.open(
                candidateResultId,
                picked.get(index),
                roundType,
                null,
                result == null ? null : result.getTotalScore(),
                now,
                currentUserId,
                deadlineAt
            ));
        }

        List<AssessmentGradingAssignment> saved = assessmentGradingAssignmentRepository.saveAll(assignments);

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
