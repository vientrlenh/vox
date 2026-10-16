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
import com.sep.vox.application.port.input.usecase.assessmentevaluation.ResolveAssessmentCandidateAttemptsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.dto.AssessmentCandidateAttempts;
import com.sep.vox.application.response.input.dashboard.AssessmentStatusCountResponse;
import com.sep.vox.application.response.input.dashboard.GradingStatsResponse;
import com.sep.vox.application.response.input.dashboard.SchoolClassScoreStatsResponse;
import com.sep.vox.application.response.input.dashboard.ScoreStatsResponse;
import com.sep.vox.application.response.input.dashboard.TeacherDashboardSummaryResponse;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignmentStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;

@Service
@Transactional(readOnly = true)
public class ViewTeacherDashboardUseCase implements IUseCase<Void, TeacherDashboardSummaryResponse> {

    private static final int MAX_CLASS_TESTS_FOR_DASHBOARD = 500;
    private static final String UNKNOWN_CLASS_NAME = "Không xác định";

    private final UserContextPort userContextPort;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final ResolveAssessmentCandidateAttemptsUseCase resolveAssessmentCandidateAttemptsUseCase;

    public ViewTeacherDashboardUseCase(UserContextPort userContextPort, AssessmentRepository assessmentRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolClassUserRepository schoolClassUserRepository,
            ResolveAssessmentCandidateAttemptsUseCase resolveAssessmentCandidateAttemptsUseCase) {
        this.userContextPort = userContextPort;
        this.assessmentRepository = assessmentRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.assessmentGradingAssignmentRepository = assessmentGradingAssignmentRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.resolveAssessmentCandidateAttemptsUseCase = resolveAssessmentCandidateAttemptsUseCase;
    }

    @Override
    public TeacherDashboardSummaryResponse execute(Void input) {
        var teacherId = userContextPort.getCurrentAuthenticatedUserId();
        var schoolId = userContextPort.getCurrentSchoolId();
        var classTests = fetchTeacherClassTests(teacherId, schoolId);

        var candidates = assessmentCandidateRepository.findByAssessmentIdIn(classTests.stream().map(assessment -> assessment.getId()).toList());
        var candidateIds = candidates.stream().map(candidate -> candidate.getId()).toList();
        Map<UUID, AssessmentCandidateAttempts> attemptsByCandidateId = candidateIds.isEmpty()
            ? Map.of()
            : resolveAssessmentCandidateAttemptsUseCase.executeBatch(candidateIds);

        return new TeacherDashboardSummaryResponse(
            buildAssessmentStatusCounts(classTests),
            buildGradingStats(teacherId),
            buildScoreStats(candidates, attemptsByCandidateId),
            buildClassScoreStats(classTests, candidates, attemptsByCandidateId)
        );
    }

    /**
     * findAccessible là 1-based: AssessmentRepositoryImpl trừ 1 trước khi dựng PageRequest, nên truyền 0
     * vào là ra -1 và Spring Data ném "Page index must not be less than zero" -- hỏng nguyên
     * request dashboard, không có đường vòng.
     *
     * <p>Đúng lỗi mà ViewSchoolAdminDashboardUseCase.countAssessmentsByStatus đã dính và đã sửa; bản của
     * giáo viên bị bỏ sót lúc đó. Đo thật 2026-08-28: dashboard giáo viên 500 mọi lần mở.
     */
    private List<Assessment> fetchTeacherClassTests(UUID teacherId, UUID schoolId) {
        var accessible = assessmentRepository.findAccessible(
            teacherId, schoolId, false, false, schoolId, null, AssessmentKind.CLASS_TEST, null, null,
            1, MAX_CLASS_TESTS_FOR_DASHBOARD
        ).content();
        if (accessible.isEmpty()) {
            return List.of();
        }

        var assessmentIds = accessible.stream().map(assessment -> assessment.getId()).toList();
        Set<UUID> memberAssessmentIds = assessmentMemberRepository.findByAssessmentIdIn(assessmentIds).stream()
            .filter(m -> m.getUserId().equals(teacherId))
            .map(member -> member.getAssessmentId())
            .collect(Collectors.toSet());

        return accessible.stream().filter(e -> memberAssessmentIds.contains(e.getId())).toList();
    }

    private AssessmentStatusCountResponse buildAssessmentStatusCounts(List<Assessment> classTests) {
        var draft = countByStatus(classTests, AssessmentStatus.DRAFT);
        var scheduled = countByStatus(classTests, AssessmentStatus.SCHEDULED);
        var inProgress = countByStatus(classTests, AssessmentStatus.IN_PROGRESS);
        var closed = countByStatus(classTests, AssessmentStatus.CLOSED);
        var resultsPublished = countByStatus(classTests, AssessmentStatus.RESULTS_PUBLISHED);
        var cancelled = countByStatus(classTests, AssessmentStatus.CANCELLED);
        var total = draft + scheduled + inProgress + closed + resultsPublished + cancelled;
        return new AssessmentStatusCountResponse(total, draft, scheduled, inProgress, closed, resultsPublished, cancelled);
    }

    private long countByStatus(List<Assessment> classTests, AssessmentStatus status) {
        return classTests.stream().filter(e -> e.getStatus() == status).count();
    }

    private GradingStatsResponse buildGradingStats(UUID teacherId) {
        return new GradingStatsResponse(
            assessmentGradingAssignmentRepository.countByTeacherIdAndStatus(teacherId, AssessmentGradingAssignmentStatus.ASSIGNED),
            assessmentGradingAssignmentRepository.countByTeacherIdAndStatus(teacherId, AssessmentGradingAssignmentStatus.COMPLETED)
        );
    }

    private ScoreStatsResponse buildScoreStats(List<AssessmentCandidate> candidates,
            Map<UUID, AssessmentCandidateAttempts> attemptsByCandidateId) {
        if (candidates.isEmpty()) {
            return new ScoreStatsResponse(null, 0, 0);
        }

        var scores = officialScoresOf(candidates, attemptsByCandidateId);
        if (scores.isEmpty()) {
            return new ScoreStatsResponse(null, 0, candidates.size());
        }

        return new ScoreStatsResponse(average(scores), scores.size(), candidates.size());
    }

    private List<SchoolClassScoreStatsResponse> buildClassScoreStats(List<Assessment> classTests, List<AssessmentCandidate> candidates,
            Map<UUID, AssessmentCandidateAttempts> attemptsByCandidateId) {
        if (classTests.isEmpty()) {
            return List.of();
        }

        var candidatesByAssessmentId = candidates.stream().collect(Collectors.groupingBy(candidate -> candidate.getAssessmentId()));
        var classIdByStudentId = resolveActiveClassIdByStudent(candidates);
        var classIdByAssessmentId = resolveClassIdByAssessment(classTests, candidatesByAssessmentId, classIdByStudentId);
        var classNameById = resolveClassNames(classIdByAssessmentId.values());

        return classTests.stream()
            .map(assessment -> {
                var assessmentCandidates = candidatesByAssessmentId.getOrDefault(assessment.getId(), List.of());
                var scores = officialScoresOf(assessmentCandidates, attemptsByCandidateId);
                var classId = classIdByAssessmentId.get(assessment.getId());
                var className = classId == null ? UNKNOWN_CLASS_NAME : classNameById.getOrDefault(classId, UNKNOWN_CLASS_NAME);
                return new SchoolClassScoreStatsResponse(
                    assessment.getName(),
                    className,
                    scores.isEmpty() ? null : average(scores),
                    scores.isEmpty() ? null : scores.stream().max(Comparator.naturalOrder()).orElseThrow(),
                    scores.isEmpty() ? null : scores.stream().min(Comparator.naturalOrder()).orElseThrow(),
                    scores.size(),
                    assessmentCandidates.size()
                );
            })
            .toList();
    }

    private Map<UUID, UUID> resolveActiveClassIdByStudent(List<AssessmentCandidate> candidates) {
        var studentIds = candidates.stream().map(candidate -> candidate.getStudentId()).distinct().toList();
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        return schoolClassUserRepository.findByUserIdIn(studentIds).stream()
            .filter(m -> m.isActive())
            .collect(Collectors.toMap(m -> m.getUserId(), m -> m.getSchoolClassId(), (a, b) -> a));
    }

    private Map<UUID, UUID> resolveClassIdByAssessment(List<Assessment> classTests, Map<UUID, List<AssessmentCandidate>> candidatesByAssessmentId,
            Map<UUID, UUID> classIdByStudentId) {
        Map<UUID, UUID> classIdByAssessmentId = new HashMap<>();
        for (var assessment : classTests) {
            var assessmentCandidates = candidatesByAssessmentId.getOrDefault(assessment.getId(), List.of());
            var classId = assessmentCandidates.stream()
                .map(c -> classIdByStudentId.get(c.getStudentId()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey())
                .orElse(null);
            classIdByAssessmentId.put(assessment.getId(), classId);
        }
        return classIdByAssessmentId;
    }

    private Map<UUID, String> resolveClassNames(Collection<UUID> classIds) {
        var distinctIds = classIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return schoolClassRepository.findAllById(distinctIds).stream()
            .collect(Collectors.toMap(sc -> sc.getId(), sc -> sc.getName()));
    }

    private List<BigDecimal> officialScoresOf(List<AssessmentCandidate> candidates,
            Map<UUID, AssessmentCandidateAttempts> attemptsByCandidateId) {
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
