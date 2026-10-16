package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.GradingAssignmentNotificationService;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.AutoAssignGradingCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.GradingSampleSelector;
import com.sep.vox.application.port.input.service.RoundRobinLoadBalancer;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentGradingRoundType;
import com.sep.vox.domain.model.assessment.AssessmentGradingSampleSelectionMode;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.service.assessment.GradingRoundPolicy;

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
        var currentUserId = assessmentGradingAccessService.requireActiveUserId();
        var schoolId = assessmentGradingAccessService.requireCurrentSchoolId(currentUserId);
        assessmentGradingAccessService.authorizeSchoolAdmin(schoolId, currentUserId);

        if (command.assessmentId() == null && command.scheduleId() == null) {
            throw new IllegalArgumentException("Phải chọn kỳ thi hoặc ca thi để phân công tự động.");
        }
        // Chặn ngay ở assessmentId chứ không đợi tới danh sách bài: bài trên lớp đã được gán
        // hết cho chủ bài nên danh sách sẽ RỖNG, và "rỗng" trả về thành công lặng lẽ —
        // admin tưởng đã phân công xong trong khi không có gì xảy ra.
        if (assessmentGradingAccessService.isClassTestAssessment(command.assessmentId())) {
            throw new ForbiddenException(AssessmentGradingAccessService.CLASS_TEST_COORDINATION_REJECTION);
        }
        var roundType = parseRoundType(command.roundType());
        var selectionMode = parseSelectionMode(command.selectionMode());
        var deadlineAt = validateDeadline(command.deadlineAt());

        var teacherIds = command.teacherIds() == null ? List.<UUID>of() : command.teacherIds();
        if (teacherIds.isEmpty()) {
            throw new IllegalArgumentException("Phải chọn ít nhất một giáo viên để phân công.");
        }
        if (new HashSet<>(teacherIds).size() != teacherIds.size()) {
            throw new ResourceDuplicatedException("Không được chọn trùng giáo viên.");
        }
        // Một query cho cả nhóm thay vì isTeacherOfSchool lặp từng người (N+1).
        var validTeachers = assessmentGradingQueryRepository.findTeacherIdsInSchool(schoolId, teacherIds);
        if (!validTeachers.containsAll(teacherIds)) {
            throw new IllegalArgumentException("Người chấm phải là giáo viên thuộc cùng trường với bài thi.");
        }

        var eligibleStatuses = GradingRoundPolicy.assignableStatuses(roundType).stream()
            .map(candidateResultStatus -> candidateResultStatus.name())
            .toList();
        var candidates = assessmentGradingQueryRepository.findAssignableResultIds(
            schoolId, command.assessmentId(), command.scheduleId(), eligibleStatuses);
        if (candidates.isEmpty()) {
            return List.of();
        }
        // Đường vào theo scheduleId không đi qua chốt assessmentId ở trên.
        assessmentGradingAccessService.rejectClassTestCoordination(candidates);

        // Chỉ nạp tín hiệu rủi ro khi chế độ thật sự cần — một query nặng chạy không
        // công cho ba chế độ còn lại là lãng phí thấy rõ.
        var riskInfos = selectionMode == AssessmentGradingSampleSelectionMode.RISK_BASED
            ? assessmentGradingQueryRepository.findRiskInfos(candidates)
            : List.<com.sep.vox.application.projection.dto.GradingRiskInfo>of();
        var selected = gradingSampleSelector.select(
            selectionMode, candidates, command.percent(), command.candidateResultIds(), riskInfos);
        if (selected.isEmpty()) {
            return List.of();
        }

        var loads = assessmentGradingQueryRepository.assignedLoadByTeacherIds(teacherIds);
        var picked = roundRobinLoadBalancer.distribute(selected, teacherIds, loads);

        // scoreBefore chụp lúc giao, batch một query — mốc đo độ lệch sau khi chấm.
        var scoreByResultId = assessmentCandidateResultRepository.findByIdIn(selected).stream()
            .collect(Collectors.toMap(result -> result.getId(), Function.identity(), (left, right) -> left));

        var now = Instant.now();
        var assignments = new ArrayList<AssessmentGradingAssignment>();
        for (var index = 0; index < selected.size(); index++) {
            var candidateResultId = selected.get(index);
            var result = scoreByResultId.get(candidateResultId);
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

        var saved = assessmentGradingAssignmentRepository.saveAll(assignments);

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

    private AssessmentGradingSampleSelectionMode parseSelectionMode(String value) {
        if (value == null || value.isBlank()) {
            return AssessmentGradingSampleSelectionMode.ALL;
        }
        try {
            return AssessmentGradingSampleSelectionMode.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("Cách chọn bài không hợp lệ.");
        }
    }

    private Instant validateDeadline(Instant deadlineAt) {
        if (deadlineAt != null && deadlineAt.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Hạn chấm phải ở tương lai.");
        }
        return deadlineAt;
    }
}
