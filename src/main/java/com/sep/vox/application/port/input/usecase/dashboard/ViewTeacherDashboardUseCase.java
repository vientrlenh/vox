package com.sep.vox.application.port.input.usecase.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.examevaluation.ResolveExamCandidateAttemptsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolAdminDashboardSummaryDto.ExamStatusCountsDto;
import com.sep.vox.domain.dto.TeacherDashboardSummaryDto;
import com.sep.vox.domain.dto.TeacherDashboardSummaryDto.GradingStatsDto;
import com.sep.vox.domain.dto.TeacherDashboardSummaryDto.ScoreStatsDto;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamRepository;

@Service
@Transactional(readOnly = true)
public class ViewTeacherDashboardUseCase implements IUseCase<Void, TeacherDashboardSummaryDto> {

    private static final int MAX_CLASS_TESTS_FOR_SCORE_STATS = 500;

    private final UserContextPort userContextPort;
    private final ExamRepository examRepository;
    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ResolveExamCandidateAttemptsUseCase resolveExamCandidateAttemptsUseCase;

    public ViewTeacherDashboardUseCase(UserContextPort userContextPort, ExamRepository examRepository,
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamCandidateRepository examCandidateRepository,
            ResolveExamCandidateAttemptsUseCase resolveExamCandidateAttemptsUseCase) {
        this.userContextPort = userContextPort;
        this.examRepository = examRepository;
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.resolveExamCandidateAttemptsUseCase = resolveExamCandidateAttemptsUseCase;
    }

    @Override
    public TeacherDashboardSummaryDto execute(Void input) {
        var teacherId = userContextPort.getCurrentAuthenticatedUserId();
        var schoolId = userContextPort.getCurrentSchoolId();

        return new TeacherDashboardSummaryDto(
            buildExamStatusCounts(teacherId, schoolId),
            buildGradingStats(teacherId),
            buildScoreStats(teacherId, schoolId)
        );
    }

    private ExamStatusCountsDto buildExamStatusCounts(UUID teacherId, UUID schoolId) {
        var draft = countExamsByStatus(teacherId, schoolId, ExamStatus.DRAFT);
        var scheduled = countExamsByStatus(teacherId, schoolId, ExamStatus.SCHEDULED);
        var inProgress = countExamsByStatus(teacherId, schoolId, ExamStatus.IN_PROGRESS);
        var closed = countExamsByStatus(teacherId, schoolId, ExamStatus.CLOSED);
        var resultsPublished = countExamsByStatus(teacherId, schoolId, ExamStatus.RESULTS_PUBLISHED);
        var cancelled = countExamsByStatus(teacherId, schoolId, ExamStatus.CANCELLED);
        var total = draft + scheduled + inProgress + closed + resultsPublished + cancelled;
        return new ExamStatusCountsDto(total, draft, scheduled, inProgress, closed, resultsPublished, cancelled);
    }

    private long countExamsByStatus(UUID teacherId, UUID schoolId, ExamStatus status) {
        return examRepository.findAccessible(
            teacherId, schoolId, false, false, schoolId, null, ExamKind.CLASS_TEST, status, null, 0, 1
        ).totalElements();
    }

    private GradingStatsDto buildGradingStats(UUID teacherId) {
        return new GradingStatsDto(
            examGradingAssignmentRepository.countByTeacherIdAndStatus(teacherId, GradingAssignmentStatus.ASSIGNED),
            examGradingAssignmentRepository.countByTeacherIdAndStatus(teacherId, GradingAssignmentStatus.COMPLETED)
        );
    }

    private ScoreStatsDto buildScoreStats(UUID teacherId, UUID schoolId) {
        var classTests = examRepository.findAccessible(
            teacherId, schoolId, false, false, schoolId, null, ExamKind.CLASS_TEST, null, null,
            0, MAX_CLASS_TESTS_FOR_SCORE_STATS
        ).content();
        if (classTests.isEmpty()) {
            return new ScoreStatsDto(null, 0, 0);
        }

        var examIds = classTests.stream().map(e -> e.getId()).toList();
        var candidateIds = examCandidateRepository.findByExamIdIn(examIds).stream()
            .map(ec -> ec.getId())
            .toList();
        if (candidateIds.isEmpty()) {
            return new ScoreStatsDto(null, 0, 0);
        }

        var attempts = resolveExamCandidateAttemptsUseCase.executeBatch(candidateIds).values().stream()
            .map(eca -> eca.officialScore())
            .filter(Objects::nonNull)
            .toList();

        if (attempts.isEmpty()) {
            return new ScoreStatsDto(null, 0, candidateIds.size());
        }

        var average = attempts.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(attempts.size()), 2, RoundingMode.HALF_UP);

        return new ScoreStatsDto(average, attempts.size(), candidateIds.size());
    }
}
