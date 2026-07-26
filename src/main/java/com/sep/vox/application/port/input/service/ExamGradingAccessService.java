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
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * Giải chuỗi assignment -> candidate result -> session -> exam -> school và phân
 * quyền theo đó.
 *
 * <p>Tách khỏi {@link ExamSessionModerationAccessService}: quyền chấm tay không đến
 * từ vai trò coi thi mà từ <em>chính dòng phân công</em>. Giáo viên được gán bài
 * nào thì thao tác được đúng bài đó — kể cả gỡ cờ nghi vấn — và không cần là thành
 * viên kỳ thi.
 */
@Service
public class ExamGradingAccessService {

    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public ExamGradingAccessService(
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamSessionRepository examSessionRepository,
            ExamRepository examRepository,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examSessionRepository = examSessionRepository;
        this.examRepository = examRepository;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    /** Ngữ cảnh đã giải xong của một phân công chấm bài. */
    public record GradingContext(
        ExamGradingAssignment assignment,
        ExamCandidateResult candidateResult,
        ExamSession session,
        UUID schoolId,
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
    public GradingContext load(UUID assignmentId) {
        var assignment = examGradingAssignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phân công chấm bài."));
        return loadFromCandidateResult(assignment, assignment.getCandidateResultId());
    }

    @Transactional(readOnly = true)
    public GradingContext loadByCandidateResultId(UUID candidateResultId) {
        var assignment = examGradingAssignmentRepository.findOpenByCandidateResultId(candidateResultId)
            .orElse(null);
        return loadFromCandidateResult(assignment, candidateResultId);
    }

    private GradingContext loadFromCandidateResult(ExamGradingAssignment assignment, UUID candidateResultId) {
        var candidateResult = examCandidateResultRepository.findById(candidateResultId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kết quả bài thi."));
        var session = examSessionRepository.findById(candidateResult.getSessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi."));
        var exam = examRepository.findById(candidateResult.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra."));
        return new GradingContext(assignment, candidateResult, session, exam.getSchoolId(), exam.getName());
    }

    /** School admin cùng trường với bài thi. Dùng cho gán / đổi / gỡ phân công. */
    public void authorizeSchoolAdmin(UUID schoolId, UUID currentUserId) {
        if (userContextPort.isSystemAdmin()) {
            return;
        }
        var isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        if (isSchoolAdmin && currentSchoolId != null && currentSchoolId.equals(schoolId)) {
            return;
        }
        throw new ForbiddenException("BẢO MẬT: Bạn không có quyền phân công chấm bài của trường này.");
    }

    /**
     * Chính giáo viên được gán bài này. Đây là chốt phân quyền duy nhất cho mọi
     * thao tác chấm — kể cả gỡ cờ và đánh INVALID.
     */
    public void authorizeAssignedTeacher(GradingContext context, UUID currentUserId) {
        var assignment = context.assignment();
        if (assignment == null || !currentUserId.equals(assignment.getTeacherId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không được phân công chấm bài thi này.");
        }
    }

    /** Trường của người đang đăng nhập — mọi màn phân công đều bị giới hạn trong đó. */
    public UUID requireCurrentSchoolId(UUID currentUserId) {
        return schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Tài khoản không thuộc trường học nào."));
    }

    /** Giáo viên cùng trường — kiểm tra trước khi gán bài cho họ. */
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
