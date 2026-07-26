package com.sep.vox.application.port.input.usecase.examgrading;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.AutoAssignGradingCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.service.GradingSampleSelector;
import com.sep.vox.application.port.input.service.RoundRobinLoadBalancer;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.GradingRoundPolicy;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.model.exam.GradingSampleSelectionMode;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;

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

    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;
    private final GradingSampleSelector gradingSampleSelector;
    private final RoundRobinLoadBalancer roundRobinLoadBalancer;

    public AutoAssignGradingUseCase(
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamGradingAccessService examGradingAccessService,
            GradingSampleSelector gradingSampleSelector,
            RoundRobinLoadBalancer roundRobinLoadBalancer) {
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
        this.gradingSampleSelector = gradingSampleSelector;
        this.roundRobinLoadBalancer = roundRobinLoadBalancer;
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
        var roundType = parseRoundType(command.roundType());
        var selectionMode = parseSelectionMode(command.selectionMode());
        var deadlineAt = validateDeadline(command.deadlineAt());

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

        var eligibleStatuses = GradingRoundPolicy.assignableStatuses(roundType).stream()
            .map(ExamCandidateResultStatus::name)
            .toList();
        var candidates = examGradingQueryRepository.findAssignableResultIds(
            schoolId, command.examId(), command.scheduleId(), eligibleStatuses);
        if (candidates.isEmpty()) {
            return List.of();
        }

        // Chỉ nạp tín hiệu rủi ro khi chế độ thật sự cần — một query nặng chạy không
        // công cho ba chế độ còn lại là lãng phí thấy rõ.
        var riskInfos = selectionMode == GradingSampleSelectionMode.RISK_BASED
            ? examGradingQueryRepository.findRiskInfos(candidates)
            : List.<com.sep.vox.application.query.dto.GradingRiskInfo>of();
        var selected = gradingSampleSelector.select(
            selectionMode, candidates, command.percent(), command.candidateResultIds(), riskInfos);
        if (selected.isEmpty()) {
            return List.of();
        }

        var loads = examGradingQueryRepository.assignedLoadByTeacherIds(teacherIds);
        var picked = roundRobinLoadBalancer.distribute(selected, teacherIds, loads);

        // scoreBefore chụp lúc giao, batch một query — mốc đo độ lệch sau khi chấm.
        var scoreByResultId = examCandidateResultRepository.findByIdIn(selected).stream()
            .collect(Collectors.toMap(result -> result.getId(), Function.identity(), (left, right) -> left));

        var now = OffsetDateTime.now();
        var assignments = new ArrayList<ExamGradingAssignment>();
        for (var index = 0; index < selected.size(); index++) {
            var candidateResultId = selected.get(index);
            var result = scoreByResultId.get(candidateResultId);
            assignments.add(ExamGradingAssignment.open(
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

    private GradingSampleSelectionMode parseSelectionMode(String value) {
        if (value == null || value.isBlank()) {
            return GradingSampleSelectionMode.ALL;
        }
        try {
            return GradingSampleSelectionMode.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("Cách chọn bài không hợp lệ.");
        }
    }

    private OffsetDateTime validateDeadline(OffsetDateTime deadlineAt) {
        if (deadlineAt != null && deadlineAt.isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Hạn chấm phải ở tương lai.");
        }
        return deadlineAt;
    }
}
