package com.sep.vox.application.port.input.usecase.examcandidate;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteExamCandidateCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.exam.ExamEditingGuard;

/**
 * Xoá hẳn một thí sinh khỏi kỳ thi.
 *
 * <p>Chỉ dành cho thí sinh chưa từng vào thi: một khi đã có phiên thi thì bản ghi thí sinh còn được
 * phiên thi và kết quả tham chiếu tới, xoá đi sẽ làm mồ côi dữ liệu chấm. Muốn gỡ thí sinh khỏi ca
 * mà vẫn giữ lịch sử thì dùng {@link AssignExamCandidateScheduleUseCase} với {@code scheduleId = null}.
 */
@Service
public class DeleteExamCandidateUseCase implements IUseCase<DeleteExamCandidateCommand, Void> {

    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public DeleteExamCandidateUseCase(
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamSessionRepository examSessionRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examSessionRepository = examSessionRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteExamCandidateCommand input) {
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        authorize(exam);
        ExamEditingGuard.requireScheduleEditable(exam);

        var candidate = examCandidateRepository.findById(input.candidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh"));
        if (!candidate.getExamId().equals(exam.getId())) {
            throw new NotFoundException("Không tìm thấy thí sinh");
        }

        if (!examSessionRepository.findAllByCandidateId(candidate.getId()).isEmpty()) {
            throw new IllegalStateException("Thí sinh đã có bài thi — không thể xóa khỏi kỳ thi");
        }

        examCandidateRepository.deleteById(candidate.getId());
        return null;
    }

    /** Cùng luật với {@link AssignExamCandidateScheduleUseCase}: quản trị trường sở tại hoặc chủ tịch hội đồng. */
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
