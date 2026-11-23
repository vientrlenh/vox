package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import com.sep.vox.application.port.input.command.AssessmentPaperAssignmentItem;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import java.time.Instant;
import java.util.ArrayList;
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

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.AssignAssessmentPapersCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.assessmentcandidate.AssignAssessmentPapersResponse;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;

@Service
@RequiredArgsConstructor
public class AssignAssessmentPapersUseCase implements IUseCase<AssignAssessmentPapersCommand, AssignAssessmentPapersResponse> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssignAssessmentPapersResponse execute(AssignAssessmentPapersCommand input) {
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        UUID currentUserId = authorize(assessment);
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        if (input.assignments() == null || input.assignments().isEmpty()) {
            return new AssignAssessmentPapersResponse(0);
        }

        // Toàn bộ mã đề của kỳ thi phải đã khoá (LOCKED) mới được phân đề.
        List<AssessmentPaper> papers = assessmentPaperRepository.findByAssessmentId(assessment.getId());
        if (papers.isEmpty() || papers.stream().anyMatch(paper -> paper.getStatus() != AssessmentPaperStatus.LOCKED)) {
            throw new IllegalArgumentException("Tất cả mã đề của kỳ thi phải được khoá trước khi phân đề");
        }
        Set<UUID> lockedPaperIds = papers.stream()
            .map(paper -> paper.getId())
            .collect(Collectors.toSet());

        Set<UUID> candidateIds = input.assignments().stream()
            .map(assignment -> assignment.candidateId())
            .collect(Collectors.toCollection(HashSet::new));
        Map<UUID, AssessmentCandidate> candidatesById = assessmentCandidateRepository.findByIdInAndAssessmentId(candidateIds, assessment.getId()).stream()
            .collect(Collectors.toMap(candidate -> candidate.getId(), Function.identity()));

        // Validate all-or-nothing: mọi cặp phải hợp lệ trước khi ghi bất kỳ thứ gì.
        for (AssessmentPaperAssignmentItem assignment : input.assignments()) {
            if (!candidatesById.containsKey(assignment.candidateId())) {
                throw new IllegalArgumentException("Thí sinh không thuộc kỳ thi này");
            }
            if (assignment.paperId() == null || !lockedPaperIds.contains(assignment.paperId())) {
                throw new IllegalArgumentException("Mã đề không hợp lệ hoặc chưa được khoá");
            }
        }

        Instant now = Instant.now();
        List<AssessmentCandidate> toSave = new ArrayList<AssessmentCandidate>();
        Set<UUID> handled = new HashSet<>();
        for (AssessmentPaperAssignmentItem assignment : input.assignments()) {
            if (!handled.add(assignment.candidateId())) {
                continue;
            }
            AssessmentCandidate candidate = candidatesById.get(assignment.candidateId());
            candidate.assignPaper(assignment.paperId(), now, currentUserId);
            toSave.add(candidate);
        }

        List<AssessmentCandidate> saved = assessmentCandidateRepository.saveAll(toSave);
        return new AssignAssessmentPapersResponse(saved.size());
    }

    private UUID authorize(Assessment assessment) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId())) {
            return currentUserId;
        }
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            return currentUserId;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
