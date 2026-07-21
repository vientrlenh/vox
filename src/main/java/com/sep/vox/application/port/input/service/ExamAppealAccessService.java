package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * Resolves the appeal -> candidate result -> session -> exam -> school chain and
 * authorizes the caller against it.
 *
 * <p>Kept separate from {@link ExamResultAccessService}: that one grants access to
 * the candidate themselves and to exam members, neither of which is the right rule
 * for an appeal. An appeal reviewer is a teacher assigned to the <em>appeal</em>,
 * not a member of the exam, so they would be rejected there.
 */
@Service
public class ExamAppealAccessService {

    private final ExamResultAppealRepository examResultAppealRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public ExamAppealAccessService(
            ExamResultAppealRepository examResultAppealRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamSessionRepository examSessionRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamRepository examRepository,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examResultAppealRepository = examResultAppealRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examSessionRepository = examSessionRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examRepository = examRepository;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    /** Ngữ cảnh đã giải xong của một đơn phúc khảo — tránh mỗi use case tự nối lại chuỗi. */
    public record AppealContext(
        ExamResultAppeal appeal,
        ExamCandidateResult candidateResult,
        ExamSession session,
        UUID schoolId,
        UUID studentId,
        String examName
    ) {
    }

    public UUID requireActiveUserId() {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản bị khóa.");
        }
        return currentUserId;
    }

    @Transactional(readOnly = true)
    public AppealContext load(UUID appealId) {
        var appeal = examResultAppealRepository.findById(appealId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn phúc khảo."));
        return loadFromCandidateResult(appeal, appeal.getCandidateResultId());
    }

    @Transactional(readOnly = true)
    public AppealContext loadByCandidateResultId(UUID candidateResultId) {
        return loadFromCandidateResult(null, candidateResultId);
    }

    private AppealContext loadFromCandidateResult(ExamResultAppeal appeal, UUID candidateResultId) {
        var candidateResult = examCandidateResultRepository.findById(candidateResultId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kết quả bài thi."));
        var session = examSessionRepository.findById(candidateResult.getSessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi."));
        var exam = examRepository.findById(candidateResult.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra."));
        var candidate = examCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh của phiên thi."));
        return new AppealContext(
            appeal, candidateResult, session, exam.getSchoolId(), candidate.getStudentId(), exam.getName());
    }

    /** School admin cùng trường với bài thi. Dùng cho duyệt / từ chối / phân công / công bố. */
    public void authorizeSchoolAdmin(AppealContext context, UUID currentUserId) {
        if (userContextPort.isSystemAdmin()) {
            return;
        }
        var isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        if (isSchoolAdmin && currentSchoolId != null && currentSchoolId.equals(context.schoolId())) {
            return;
        }
        throw new ForbiddenException("BẢO MẬT: Bạn không có quyền thao tác trên đơn phúc khảo của trường này.");
    }

    /** Chính chủ bài thi. Dùng cho nộp đơn. */
    public void authorizeOwningStudent(AppealContext context, UUID currentUserId) {
        if (!currentUserId.equals(context.studentId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn chỉ có thể phúc khảo bài thi của chính mình.");
        }
    }

    /** Giáo viên cùng trường — dùng khi kiểm tra ứng viên được phân công. */
    public boolean isTeacherOfSchool(UUID userId, UUID schoolId) {
        var isTeacher = userRoleQueryRepository.findByUserIdWithRoleInfo(userId).stream()
            .anyMatch(role -> "TEACHER".equals(role.roleCode()));
        if (!isTeacher) {
            return false;
        }
        return schoolUserRepository.findByUserId(userId)
            .map(schoolUser -> schoolId.equals(schoolUser.getSchoolId()))
            .orElse(false);
    }
}
