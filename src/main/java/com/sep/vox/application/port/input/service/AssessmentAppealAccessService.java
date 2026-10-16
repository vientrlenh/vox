package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentResultAppealRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;

/**
 * Resolves the appeal -> candidate result -> session -> assessment -> school chain and
 * authorizes the caller against it.
 *
 * <p>Kept separate from {@link AssessmentResultAccessService}: that one grants access to
 * the candidate themselves and to assessment members, neither of which is the right rule
 * for an appeal. An appeal reviewer is a teacher assigned to the <em>appeal</em>,
 * not a member of the assessment, so they would be rejected there.
 */
@Service
public class AssessmentAppealAccessService {

    private final AssessmentResultAppealRepository assessmentResultAppealRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentRepository assessmentRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    public AssessmentAppealAccessService(
            AssessmentResultAppealRepository assessmentResultAppealRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentRepository assessmentRepository,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort,
            AssessmentGradingAccessService assessmentGradingAccessService) {
        this.assessmentResultAppealRepository = assessmentResultAppealRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentRepository = assessmentRepository;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
    }

    /** Ngữ cảnh đã giải xong của một đơn phúc khảo — tránh mỗi use case tự nối lại chuỗi. */
    public record AppealContext(
        AssessmentResultAppeal appeal,
        AssessmentCandidateResult candidateResult,
        AssessmentSession session,
        UUID schoolId,
        UUID studentId,
        String assessmentName
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
        var appeal = assessmentResultAppealRepository.findById(appealId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn phúc khảo."));
        return loadFromCandidateResult(appeal, appeal.getCandidateResultId());
    }

    @Transactional(readOnly = true)
    public AppealContext loadByCandidateResultId(UUID candidateResultId) {
        return loadFromCandidateResult(null, candidateResultId);
    }

    private AppealContext loadFromCandidateResult(AssessmentResultAppeal appeal, UUID candidateResultId) {
        var candidateResult = assessmentCandidateResultRepository.findById(candidateResultId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kết quả bài thi."));
        var session = assessmentSessionRepository.findById(candidateResult.getSessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi."));
        var assessment = assessmentRepository.findById(candidateResult.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra."));
        var candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thí sinh của phiên thi."));
        return new AppealContext(
            appeal, candidateResult, session, assessment.getSchoolId(), candidate.getStudentId(), assessment.getName());
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

    /**
     * School admin cùng trường, HOẶC giáo viên tạo bài kiểm tra trên lớp đó.
     *
     * <p>Với bài trên lớp, nhà trường không tham gia chấm — nên chính chủ bài là người
     * duyệt/từ chối đơn và tự nhận chấm phúc khảo. Luật CHAIR nằm ở
     * {@link AssessmentGradingAccessService#isClassTestChair}, không suy lại ở đây.
     */
    public void authorizeSchoolAdminOrClassTestChair(AppealContext context, UUID currentUserId) {
        if (isClassTestChair(context, currentUserId)) {
            return;
        }
        authorizeSchoolAdmin(context, currentUserId);
    }

    public boolean isClassTestChair(AppealContext context, UUID currentUserId) {
        return assessmentGradingAccessService.isClassTestChair(
            context.candidateResult().getAssessmentId(), currentUserId);
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
