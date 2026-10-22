package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.AssignAssessmentPapersCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.assessmentcandidate.AssignAssessmentPapersResponse;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;

@Service
public class AssignAssessmentPapersUseCase implements IUseCase<AssignAssessmentPapersCommand, AssignAssessmentPapersResponse> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public AssignAssessmentPapersUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentPaperRepository assessmentPaperRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentPaperRepository = assessmentPaperRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssignAssessmentPapersResponse execute(AssignAssessmentPapersCommand input) {
        var assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = authorize(assessment);
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        if (input.assignments() == null || input.assignments().isEmpty()) {
            return new AssignAssessmentPapersResponse(0);
        }

        // Toàn bộ mã đề của kỳ thi phải đã khoá (LOCKED) mới được phân đề.
        var papers = assessmentPaperRepository.findByAssessmentId(assessment.getId());
        if (papers.isEmpty() || papers.stream().anyMatch(paper -> paper.getStatus() != AssessmentPaperStatus.LOCKED)) {
            throw new IllegalStateException("Tất cả mã đề của kỳ thi phải được khoá trước khi phân đề");
        }
        var lockedPaperIds = papers.stream()
            .map(paper -> paper.getId())
            .collect(Collectors.toSet());

        var candidateIds = input.assignments().stream()
            .map(assignment -> assignment.candidateId())
            .collect(Collectors.toCollection(HashSet::new));
        var candidatesById = assessmentCandidateRepository.findByIdInAndAssessmentId(candidateIds, assessment.getId()).stream()
            .collect(Collectors.toMap(candidate -> candidate.getId(), Function.identity()));

        // Validate all-or-nothing: mọi cặp phải hợp lệ trước khi ghi bất kỳ thứ gì.
        for (var assignment : input.assignments()) {
            if (!candidatesById.containsKey(assignment.candidateId())) {
                throw new IllegalStateException("Thí sinh không thuộc kỳ thi này");
            }
            if (assignment.paperId() == null || !lockedPaperIds.contains(assignment.paperId())) {
                throw new IllegalStateException("Mã đề không hợp lệ hoặc chưa được khoá");
            }
        }

        var now = Instant.now();
        var toSave = new ArrayList<AssessmentCandidate>();
        Set<UUID> handled = new HashSet<>();
        for (var assignment : input.assignments()) {
            if (!handled.add(assignment.candidateId())) {
                continue;
            }
            var candidate = candidatesById.get(assignment.candidateId());
            candidate.assignPaper(assignment.paperId(), now, currentUserId);
            toSave.add(candidate);
        }

        var saved = assessmentCandidateRepository.saveAll(toSave);
        return new AssignAssessmentPapersResponse(saved.size());
    }

    private UUID authorize(Assessment assessment) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userContextPort.isSchoolAdmin();
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId())) {
            return currentUserId;
        }
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            return currentUserId;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
