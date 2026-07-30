package com.sep.vox.application.port.input.usecase.exam;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteExamCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.exam.DeleteExamResponse;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class DeleteExamUseCase implements IUseCase<DeleteExamCommand, DeleteExamResponse> {

    private final ExamRepository examRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public DeleteExamUseCase(
            ExamRepository examRepository,
            ExamPaperRepository examPaperRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examPaperRepository = examPaperRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public DeleteExamResponse execute(DeleteExamCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        authorizeDelete(exam.getId(), exam.getSchoolId(), exam.getKind(), currentUserId, currentSchoolId, schoolAdmin);

        var hasSubmittedSessions = examRepository.existsSubmittedSessionByExamId(exam.getId());
        // CLASS_TEST luôn có sẵn 1 ExamPaper ngay từ lúc tạo (pipeline tự sinh), nên "có paper" không phải dấu
        // hiệu "đang dùng" như ở CENTRALIZED - chỉ chặn hard-delete khi đã có học sinh nộp bài.
        var hasPapers = exam.getKind() == ExamKind.CENTRALIZED && examPaperRepository.existsByExamId(exam.getId());
        if (hasPapers || hasSubmittedSessions) {
            exam.setStatus(ExamStatus.CANCELLED);
            exam.setUpdatedAt(Instant.now());
            exam.setUpdatedBy(currentUserId);
            examRepository.save(exam);
            return new DeleteExamResponse(false, true);
        }

        examRepository.deleteById(exam.getId());
        return new DeleteExamResponse(true, false);
    }

    private void authorizeDelete(
            java.util.UUID examId,
            java.util.UUID examSchoolId,
            ExamKind kind,
            java.util.UUID currentUserId,
            java.util.UUID currentSchoolId,
            boolean schoolAdmin) {
        if (kind == ExamKind.CENTRALIZED) {
            if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(examSchoolId)) {
                return;
            }
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (!examMemberRepository.existsByExamIdAndUserIdAndRole(examId, currentUserId, ExamMemberRole.CHAIR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }
}
