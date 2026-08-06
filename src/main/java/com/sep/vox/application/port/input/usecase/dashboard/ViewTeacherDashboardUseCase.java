package com.sep.vox.application.port.input.usecase.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.examevaluation.ResolveExamCandidateAttemptsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.ExamCandidateAttempts;
import com.sep.vox.domain.dto.SchoolAdminDashboardSummaryDto.ExamStatusCountsDto;
import com.sep.vox.domain.dto.TeacherDashboardSummaryDto;
import com.sep.vox.domain.dto.TeacherDashboardSummaryDto.GradingStatsDto;
import com.sep.vox.domain.dto.TeacherDashboardSummaryDto.ScoreStatsDto;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMember;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;

@Service
@Transactional(readOnly = true)
public class ViewTeacherDashboardUseCase implements IUseCase<Void, TeacherDashboardSummaryDto> {

    private static final int MAX_CLASS_TESTS_FOR_DASHBOARD = 500;

    private final UserContextPort userContextPort;
    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ResolveExamCandidateAttemptsUseCase resolveExamCandidateAttemptsUseCase;

    public ViewTeacherDashboardUseCase(UserContextPort userContextPort, ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamCandidateRepository examCandidateRepository,
            ResolveExamCandidateAttemptsUseCase resolveExamCandidateAttemptsUseCase) {
        this.userContextPort = userContextPort;
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.resolveExamCandidateAttemptsUseCase = resolveExamCandidateAttemptsUseCase;
    }

    @Override
    public TeacherDashboardSummaryDto execute(Void input) {
        var teacherId = userContextPort.getCurrentAuthenticatedUserId();
        var schoolId = userContextPort.getCurrentSchoolId();
        var classTests = fetchTeacherClassTests(teacherId, schoolId);

        return new TeacherDashboardSummaryDto(
            buildExamStatusCounts(classTests),
            buildGradingStats(teacherId),
            buildScoreStats(classTests)
        );
    }

    private List<Exam> fetchTeacherClassTests(UUID teacherId, UUID schoolId) {
        var accessible = examRepository.findAccessible(
            teacherId, schoolId, false, false, schoolId, null, ExamKind.CLASS_TEST, null, null,
            0, MAX_CLASS_TESTS_FOR_DASHBOARD
        ).content();
        if (accessible.isEmpty()) {
            return List.of();
        }

        var examIds = accessible.stream().map(Exam::getId).toList();
        Set<UUID> memberExamIds = examMemberRepository.findByExamIdIn(examIds).stream()
            .filter(m -> m.getUserId().equals(teacherId))
            .map(ExamMember::getExamId)
            .collect(Collectors.toSet());

        return accessible.stream().filter(e -> memberExamIds.contains(e.getId())).toList();
    }

    private ExamStatusCountsDto buildExamStatusCounts(List<Exam> classTests) {
        var draft = countByStatus(classTests, ExamStatus.DRAFT);
        var scheduled = countByStatus(classTests, ExamStatus.SCHEDULED);
        var inProgress = countByStatus(classTests, ExamStatus.IN_PROGRESS);
        var closed = countByStatus(classTests, ExamStatus.CLOSED);
        var resultsPublished = countByStatus(classTests, ExamStatus.RESULTS_PUBLISHED);
        var cancelled = countByStatus(classTests, ExamStatus.CANCELLED);
        var total = draft + scheduled + inProgress + closed + resultsPublished + cancelled;
        return new ExamStatusCountsDto(total, draft, scheduled, inProgress, closed, resultsPublished, cancelled);
    }

    private long countByStatus(List<Exam> classTests, ExamStatus status) {
        return classTests.stream().filter(e -> e.getStatus() == status).count();
    }

    private GradingStatsDto buildGradingStats(UUID teacherId) {
        return new GradingStatsDto(
            examGradingAssignmentRepository.countByTeacherIdAndStatus(teacherId, GradingAssignmentStatus.ASSIGNED),
            examGradingAssignmentRepository.countByTeacherIdAndStatus(teacherId, GradingAssignmentStatus.COMPLETED)
        );
    }

    private ScoreStatsDto buildScoreStats(List<Exam> classTests) {
        if (classTests.isEmpty()) {
            return new ScoreStatsDto(null, 0, 0);
        }

        var examIds = classTests.stream().map(Exam::getId).toList();
        var candidateIds = examCandidateRepository.findByExamIdIn(examIds).stream()
            .map(ExamCandidate::getId)
            .toList();
        if (candidateIds.isEmpty()) {
            return new ScoreStatsDto(null, 0, 0);
        }

        var attempts = resolveExamCandidateAttemptsUseCase.executeBatch(candidateIds).values().stream()
            .map(ExamCandidateAttempts::officialScore)
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
