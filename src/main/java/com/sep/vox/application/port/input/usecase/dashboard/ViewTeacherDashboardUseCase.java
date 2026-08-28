package com.sep.vox.application.port.input.usecase.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.examevaluation.ResolveExamCandidateAttemptsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.ExamCandidateAttempts;
import com.sep.vox.application.response.input.dashboard.ExamStatusCountResponse;
import com.sep.vox.application.response.input.dashboard.GradingStatsResponse;
import com.sep.vox.application.response.input.dashboard.SchoolClassScoreStatsResponse;
import com.sep.vox.application.response.input.dashboard.ScoreStatsResponse;
import com.sep.vox.application.response.input.dashboard.TeacherDashboardSummaryResponse;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;

@Service
@Transactional(readOnly = true)
public class ViewTeacherDashboardUseCase implements IUseCase<Void, TeacherDashboardSummaryResponse> {

    private static final int MAX_CLASS_TESTS_FOR_DASHBOARD = 500;
    private static final String UNKNOWN_CLASS_NAME = "Không xác định";

    private final UserContextPort userContextPort;
    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final ResolveExamCandidateAttemptsUseCase resolveExamCandidateAttemptsUseCase;

    public ViewTeacherDashboardUseCase(UserContextPort userContextPort, ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamCandidateRepository examCandidateRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolClassUserRepository schoolClassUserRepository,
            ResolveExamCandidateAttemptsUseCase resolveExamCandidateAttemptsUseCase) {
        this.userContextPort = userContextPort;
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.resolveExamCandidateAttemptsUseCase = resolveExamCandidateAttemptsUseCase;
    }

    @Override
    public TeacherDashboardSummaryResponse execute(Void input) {
        var teacherId = userContextPort.getCurrentAuthenticatedUserId();
        var schoolId = userContextPort.getCurrentSchoolId();
        var classTests = fetchTeacherClassTests(teacherId, schoolId);

        var candidates = examCandidateRepository.findByExamIdIn(classTests.stream().map(exam -> exam.getId()).toList());
        var candidateIds = candidates.stream().map(candidate -> candidate.getId()).toList();
        Map<UUID, ExamCandidateAttempts> attemptsByCandidateId = candidateIds.isEmpty()
            ? Map.of()
            : resolveExamCandidateAttemptsUseCase.executeBatch(candidateIds);

        return new TeacherDashboardSummaryResponse(
            buildExamStatusCounts(classTests),
            buildGradingStats(teacherId),
            buildScoreStats(candidates, attemptsByCandidateId),
            buildClassScoreStats(classTests, candidates, attemptsByCandidateId)
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

        var examIds = accessible.stream().map(exam -> exam.getId()).toList();
        Set<UUID> memberExamIds = examMemberRepository.findByExamIdIn(examIds).stream()
            .filter(m -> m.getUserId().equals(teacherId))
            .map(member -> member.getExamId())
            .collect(Collectors.toSet());

        return accessible.stream().filter(e -> memberExamIds.contains(e.getId())).toList();
    }

    private ExamStatusCountResponse buildExamStatusCounts(List<Exam> classTests) {
        var draft = countByStatus(classTests, ExamStatus.DRAFT);
        var scheduled = countByStatus(classTests, ExamStatus.SCHEDULED);
        var inProgress = countByStatus(classTests, ExamStatus.IN_PROGRESS);
        var closed = countByStatus(classTests, ExamStatus.CLOSED);
        var resultsPublished = countByStatus(classTests, ExamStatus.RESULTS_PUBLISHED);
        var cancelled = countByStatus(classTests, ExamStatus.CANCELLED);
        var total = draft + scheduled + inProgress + closed + resultsPublished + cancelled;
        return new ExamStatusCountResponse(total, draft, scheduled, inProgress, closed, resultsPublished, cancelled);
    }

    private long countByStatus(List<Exam> classTests, ExamStatus status) {
        return classTests.stream().filter(e -> e.getStatus() == status).count();
    }

    private GradingStatsResponse buildGradingStats(UUID teacherId) {
        return new GradingStatsResponse(
            examGradingAssignmentRepository.countByTeacherIdAndStatus(teacherId, GradingAssignmentStatus.ASSIGNED),
            examGradingAssignmentRepository.countByTeacherIdAndStatus(teacherId, GradingAssignmentStatus.COMPLETED)
        );
    }

    private ScoreStatsResponse buildScoreStats(List<ExamCandidate> candidates,
            Map<UUID, ExamCandidateAttempts> attemptsByCandidateId) {
        if (candidates.isEmpty()) {
            return new ScoreStatsResponse(null, 0, 0);
        }

        var scores = officialScoresOf(candidates, attemptsByCandidateId);
        if (scores.isEmpty()) {
            return new ScoreStatsResponse(null, 0, candidates.size());
        }

        return new ScoreStatsResponse(average(scores), scores.size(), candidates.size());
    }

    private List<SchoolClassScoreStatsResponse> buildClassScoreStats(List<Exam> classTests, List<ExamCandidate> candidates,
            Map<UUID, ExamCandidateAttempts> attemptsByCandidateId) {
        if (classTests.isEmpty()) {
            return List.of();
        }

        var candidatesByExamId = candidates.stream().collect(Collectors.groupingBy(candidate -> candidate.getExamId()));
        var classIdByStudentId = resolveActiveClassIdByStudent(candidates);
        var classIdByExamId = resolveClassIdByExam(classTests, candidatesByExamId, classIdByStudentId);
        var classNameById = resolveClassNames(classIdByExamId.values());

        return classTests.stream()
            .map(exam -> {
                var examCandidates = candidatesByExamId.getOrDefault(exam.getId(), List.of());
                var scores = officialScoresOf(examCandidates, attemptsByCandidateId);
                var classId = classIdByExamId.get(exam.getId());
                var className = classId == null ? UNKNOWN_CLASS_NAME : classNameById.getOrDefault(classId, UNKNOWN_CLASS_NAME);
                return new SchoolClassScoreStatsResponse(
                    exam.getName(),
                    className,
                    scores.isEmpty() ? null : average(scores),
                    scores.isEmpty() ? null : scores.stream().max(Comparator.naturalOrder()).orElseThrow(),
                    scores.isEmpty() ? null : scores.stream().min(Comparator.naturalOrder()).orElseThrow(),
                    scores.size(),
                    examCandidates.size()
                );
            })
            .toList();
    }

    private Map<UUID, UUID> resolveActiveClassIdByStudent(List<ExamCandidate> candidates) {
        var studentIds = candidates.stream().map(candidate -> candidate.getStudentId()).distinct().toList();
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        return schoolClassUserRepository.findByUserIdIn(studentIds).stream()
            .filter(m -> m.isActive())
            .collect(Collectors.toMap(m -> m.getUserId(), m -> m.getSchoolClassId(), (a, b) -> a));
    }

    private Map<UUID, UUID> resolveClassIdByExam(List<Exam> classTests, Map<UUID, List<ExamCandidate>> candidatesByExamId,
            Map<UUID, UUID> classIdByStudentId) {
        Map<UUID, UUID> classIdByExamId = new HashMap<>();
        for (var exam : classTests) {
            var examCandidates = candidatesByExamId.getOrDefault(exam.getId(), List.of());
            var classId = examCandidates.stream()
                .map(c -> classIdByStudentId.get(c.getStudentId()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey())
                .orElse(null);
            classIdByExamId.put(exam.getId(), classId);
        }
        return classIdByExamId;
    }

    private Map<UUID, String> resolveClassNames(Collection<UUID> classIds) {
        var distinctIds = classIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return schoolClassRepository.findAllById(distinctIds).stream()
            .collect(Collectors.toMap(sc -> sc.getId(), sc -> sc.getName()));
    }

    private List<BigDecimal> officialScoresOf(List<ExamCandidate> candidates,
            Map<UUID, ExamCandidateAttempts> attemptsByCandidateId) {
        return candidates.stream()
            .map(c -> attemptsByCandidateId.get(c.getId()))
            .filter(Objects::nonNull)
            .map(attempt -> attempt.officialScore())
            .filter(Objects::nonNull)
            .toList();
    }

    private BigDecimal average(List<BigDecimal> scores) {
        return scores.stream()
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b))
            .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }
}
