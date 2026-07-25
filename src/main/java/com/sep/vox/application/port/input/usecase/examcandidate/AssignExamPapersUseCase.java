package com.sep.vox.application.port.input.usecase.examcandidate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AssignExamPapersCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.examcandidate.AssignExamPapersResponse;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class AssignExamPapersUseCase implements IUseCase<AssignExamPapersCommand, AssignExamPapersResponse> {

    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public AssignExamPapersUseCase(
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamPaperRepository examPaperRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examPaperRepository = examPaperRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssignExamPapersResponse execute(AssignExamPapersCommand input) {
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = authorize(exam);

        if (input.assignments() == null || input.assignments().isEmpty()) {
            return new AssignExamPapersResponse(0);
        }

        // Toàn bộ mã đề của kỳ thi phải đã khoá (LOCKED) mới được phân đề.
        var papers = examPaperRepository.findByExamId(exam.getId());
        if (papers.isEmpty() || papers.stream().anyMatch(paper -> paper.getStatus() != ExamPaperStatus.LOCKED)) {
            throw new IllegalStateException("Tất cả mã đề của kỳ thi phải được khoá trước khi phân đề");
        }
        var lockedPaperIds = papers.stream()
            .map(paper -> paper.getId())
            .collect(Collectors.toSet());

        var candidateIds = input.assignments().stream()
            .map(assignment -> assignment.candidateId())
            .collect(Collectors.toCollection(HashSet::new));
        var candidatesById = examCandidateRepository.findByIdInAndExamId(candidateIds, exam.getId()).stream()
            .collect(Collectors.toMap(ExamCandidate::getId, Function.identity()));

        // Validate all-or-nothing: mọi cặp phải hợp lệ trước khi ghi bất kỳ thứ gì.
        for (var assignment : input.assignments()) {
            if (!candidatesById.containsKey(assignment.candidateId())) {
                throw new IllegalStateException("Thí sinh không thuộc kỳ thi này");
            }
            if (assignment.paperId() == null || !lockedPaperIds.contains(assignment.paperId())) {
                throw new IllegalStateException("Mã đề không hợp lệ hoặc chưa được khoá");
            }
        }

        var now = OffsetDateTime.now();
        var toSave = new ArrayList<ExamCandidate>();
        Set<UUID> handled = new HashSet<>();
        for (var assignment : input.assignments()) {
            if (!handled.add(assignment.candidateId())) {
                continue;
            }
            var candidate = candidatesById.get(assignment.candidateId());
            candidate.assignPaper(assignment.paperId(), now, currentUserId);
            toSave.add(candidate);
        }

        var saved = examCandidateRepository.saveAll(toSave);
        return new AssignExamPapersResponse(saved.size());
    }

    private UUID authorize(Exam exam) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(exam.getSchoolId())) {
            return currentUserId;
        }
        if (examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.CHAIR)) {
            return currentUserId;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
