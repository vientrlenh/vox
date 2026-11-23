package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.user.User;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * Resolves the chain assignment -> candidate result -> session -> assessment -> school and authorizes
 * along it.
 *
 * <p>Kept apart from {@link AssessmentSessionModerationAccessService}: the permission to grade by hand does not come
 * from the proctoring role but from <em>the assignment row itself</em>. A teacher can operate on exactly the attempt
 * they were assigned -- including removing a suspicion flag -- and does not need to be a
 * member of the assessment.
 */
@Service
@RequiredArgsConstructor
public class AssessmentGradingAccessService {

    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    /**
     * The resolved context of a grading assignment.
     *
     * @param assessmentKind taken ready-made from the Assessment loaded in {@link #loadFromCandidateResult}. The events
     *        published from the grading flow carry this field so that notifications know whether to open the screen of a
     *        centralized assessment or a class test -- the two branches have different routes, and the place
     *        that publishes the event no longer holds the Assessment in hand.
     */
    public record GradingContext(
        AssessmentGradingAssignment assignment,
        AssessmentCandidateResult candidateResult,
        AssessmentSession session,
        UUID schoolId,
        String assessmentName,
        AssessmentKind assessmentKind
    ) {
    }

    public UUID requireActiveUserId() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản bị khóa.");
        }
        return currentUserId;
    }

    @Transactional(readOnly = true)
    public GradingContext load(UUID assignmentId) {
        AssessmentGradingAssignment assignment = assessmentGradingAssignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phân công chấm bài."));
        return loadFromCandidateResult(assignment, assignment.getCandidateResultId());
    }

    /**
     * Như {@link #load} nhưng khoá dòng phân công để ghi.
     *
     * <p>Dùng cho mọi luồng đọc-sửa-ghi trên một phân công. Không có khoá, hai request
     * cùng đọc một dòng ASSIGNED sẽ cùng vượt qua các kiểm tra kiểu {@code isCompleted()}
     * rồi cùng ghi — mà {@code save} ở đây là {@code merge} trên POJO detached nên ghi đè
     * trọn cả dòng. Khoá cho luồng sau chờ, đọc lại trạng thái đã cập nhật, và ném đúng
     * thông báo nghiệp vụ thay vì lặng lẽ chấm đè.
     *
     * <p>{@code MANDATORY} vì khoá chỉ có nghĩa khi nằm trong transaction ghi của use
     * case: nếu tự mở transaction riêng, khoá sẽ nhả ngay khi hàm này trả về.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public GradingContext loadForUpdate(UUID assignmentId) {
        AssessmentGradingAssignment assignment = assessmentGradingAssignmentRepository.findByIdForUpdate(assignmentId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phân công chấm bài."));
        return loadFromCandidateResult(assignment, assignment.getCandidateResultId());
    }

    @Transactional(readOnly = true)
    public GradingContext loadByCandidateResultId(UUID candidateResultId) {
        AssessmentGradingAssignment assignment = assessmentGradingAssignmentRepository.findOpenByCandidateResultId(candidateResultId)
            .orElse(null);
        return loadFromCandidateResult(assignment, candidateResultId);
    }

    private GradingContext loadFromCandidateResult(AssessmentGradingAssignment assignment, UUID candidateResultId) {
        AssessmentCandidateResult candidateResult = assessmentCandidateResultRepository.findById(candidateResultId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kết quả bài thi."));
        AssessmentSession session = assessmentSessionRepository.findById(candidateResult.getSessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi."));
        Assessment assessment = assessmentRepository.findById(candidateResult.getAssessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra."));
        return new GradingContext(
            assignment, candidateResult, session, assessment.getSchoolId(), assessment.getName(), assessment.getKind());
    }

    /** School admin cùng trường với bài thi. Dùng cho gán / đổi / gỡ phân công. */
    public void authorizeSchoolAdmin(UUID schoolId, UUID currentUserId) {
        if (isSchoolAdminOfSchool(schoolId, currentUserId)) {
            return;
        }
        throw new ForbiddenException("BẢO MẬT: Bạn không có quyền phân công chấm bài của trường này.");
    }

    /**
     * Chính giáo viên được gán bài này. Đây là chốt phân quyền duy nhất cho mọi
     * thao tác chấm — kể cả gỡ cờ và đánh INVALID.
     *
     * <p>School admin KHÔNG được đi cửa này: quyền chấm đến từ chính dòng phân công,
     * và vòng chấm ({@code roundType}) — thứ quyết định hành động nào hợp lệ và bài
     * được chuyển sang trạng thái nào — cũng nằm ở đó. Nhà trường muốn chấm thì tự
     * gán qua {@code authorizeSchoolAdmin}, để lại một dòng phân công có vòng rõ ràng.
     */
    public void authorizeAssignedTeacher(GradingContext context, UUID currentUserId) {
        AssessmentGradingAssignment assignment = context.assignment();
        if (assignment == null || !currentUserId.equals(assignment.getTeacherId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không được phân công chấm bài thi này.");
        }
    }

    /**
     * CHAIR của một bài kiểm tra trên lớp — chốt quyền DUY NHẤT cho mọi thao tác chấm
     * của bài trên lớp.
     *
     * <p>Kỳ thi {@code CENTRALIZED} luôn trả {@code false} kể cả khi người gọi là CHAIR
     * của nó: bài tập trung do nhà trường điều phối, quyền chấm đến từ dòng phân công
     * chứ không từ vai trò trong kỳ thi.
     */
    public boolean isClassTestChair(UUID assessmentId, UUID currentUserId) {
        if (assessmentId == null || currentUserId == null) {
            return false;
        }
        Assessment assessment = assessmentRepository.findById(assessmentId).orElse(null);
        if (assessment == null || assessment.getKind() != AssessmentKind.CLASS_TEST) {
            return false;
        }
        return assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR);
    }

    public void authorizeClassTestChair(UUID assessmentId, UUID currentUserId) {
        if (isClassTestChair(assessmentId, currentUserId)) {
            return;
        }
        throw new ForbiddenException("BẢO MẬT: Bạn không phải giáo viên tạo bài kiểm tra trên lớp này.");
    }

    /** School admin cùng trường, HOẶC giáo viên tạo bài kiểm tra trên lớp đó. */
    public void authorizeSchoolAdminOrClassTestChair(UUID schoolId, UUID assessmentId, UUID currentUserId) {
        if (isSchoolAdminOfSchool(schoolId, currentUserId) || isClassTestChair(assessmentId, currentUserId)) {
            return;
        }
        throw new ForbiddenException("BẢO MẬT: Bạn không có quyền thao tác trên bài kiểm tra này.");
    }

    /**
     * The CHAIR of this assessment, REGARDLESS of the assessment kind -- fixes the permission for the READ paths
     * that are scoped to exactly one assessment.
     *
     * <p>Different from {@link #isClassTestChair} in exactly one point: there is no {@code CLASS_TEST} gate.
     * That gate protects the WRITE permission of the grading flow -- who may grade which attempt, which
     * status an attempt moves to -- and the reason it blocks the chair of a centralized assessment is that a centralized attempt is coordinated by
     * the school. That reason does not apply to reading: the chair can press the publish-results button
     * ({@code UpdateAssessmentStatusUseCase.authorizeMutation}) while
     * {@code requirePublishReadiness} requires every attempt to be RELEASED or INVALID, so not
     * letting them see what is blocking that button would force them to press blind with no way to fix it themselves.
     *
     * <p>⚠️ ONLY for READ use cases. Every grading coordination action (assign / change / remove an
     * assignment, close the books) must still go through {@link #isClassTestChair}: the teacher queue is a
     * resource shared by the whole school and balanced by the school-wide load, so a chair must not
     * spend it on their own assessment.
     */
    public boolean isAssessmentChair(UUID assessmentId, UUID currentUserId) {
        if (assessmentId == null || currentUserId == null) {
            return false;
        }
        return assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR);
    }

    /** A school admin of the same school, OR the chair of that very assessment. READ paths only -- see {@link #isAssessmentChair}. */
    public void authorizeSchoolAdminOrAssessmentChair(UUID schoolId, UUID assessmentId, UUID currentUserId) {
        if (isSchoolAdminOfSchool(schoolId, currentUserId) || isAssessmentChair(assessmentId, currentUserId)) {
            return;
        }
        throw new ForbiddenException("BẢO MẬT: Bạn không có quyền xem dữ liệu chấm của kỳ thi này.");
    }

    /**
     * Bài thuộc một kỳ thi {@code CLASS_TEST}? Dùng để CHẶN school admin điều phối —
     * bài trên lớp do giáo viên tạo bài tự chấm, hai mô hình sở hữu chồng lên nhau là
     * hai bên tranh cùng một {@code active_result_id}.
     */
    public boolean isClassTestResult(UUID candidateResultId) {
        if (candidateResultId == null) {
            return false;
        }
        return assessmentCandidateResultRepository.findById(candidateResultId)
            .map(result -> isClassTestAssessment(result.getAssessmentId()))
            .orElse(false);
    }

    /** Như {@link #isClassTestResult} nhưng theo lô — hai query cho cả danh sách, không N+1. */
    public Set<UUID> classTestResultIds(Collection<UUID> candidateResultIds) {
        if (candidateResultIds == null || candidateResultIds.isEmpty()) {
            return Set.of();
        }
        List<AssessmentCandidateResult> results = assessmentCandidateResultRepository.findByIdIn(candidateResultIds);
        Set<UUID> classTestAssessmentIds = assessmentRepository
            .findByIdIn(results.stream().map(result -> result.getAssessmentId()).distinct().toList()).stream()
            .filter(assessment -> assessment.getKind() == AssessmentKind.CLASS_TEST)
            .map(assessment -> assessment.getId())
            .collect(Collectors.toSet());
        return results.stream()
            .filter(result -> classTestAssessmentIds.contains(result.getAssessmentId()))
            .map(result -> result.getId())
            .collect(Collectors.toSet());
    }

    public boolean anyClassTestResult(Collection<UUID> candidateResultIds) {
        return !classTestResultIds(candidateResultIds).isEmpty();
    }

    public static final String CLASS_TEST_COORDINATION_REJECTION =
        "Bài kiểm tra trên lớp do giáo viên tạo bài tự chấm, nhà trường không phân công.";

    /** Chặn nhà trường điều phối một bài của lớp. Xem {@link #isClassTestResult}. */
    public void rejectClassTestCoordination(UUID candidateResultId) {
        if (isClassTestResult(candidateResultId)) {
            throw new ForbiddenException(CLASS_TEST_COORDINATION_REJECTION);
        }
    }

    /** Như trên nhưng cho cả lô — chặn cả lô chứ không lọc lẻ, để kết quả không phụ thuộc dữ liệu ẩn. */
    public void rejectClassTestCoordination(Collection<UUID> candidateResultIds) {
        if (anyClassTestResult(candidateResultIds)) {
            throw new ForbiddenException(CLASS_TEST_COORDINATION_REJECTION);
        }
    }

    public boolean isClassTestAssessment(UUID assessmentId) {
        if (assessmentId == null) {
            return false;
        }
        return assessmentRepository.findById(assessmentId)
            .map(assessment -> assessment.getKind() == AssessmentKind.CLASS_TEST)
            .orElse(false);
    }

    private boolean isSchoolAdminOfSchool(UUID schoolId, UUID currentUserId) {
        if (userContextPort.isSystemAdmin()) {
            return true;
        }
        boolean isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        return isSchoolAdmin && currentSchoolId != null && currentSchoolId.equals(schoolId);
    }

    /** Trường của người đang đăng nhập — mọi màn phân công đều bị giới hạn trong đó. */
    public UUID requireCurrentSchoolId(UUID currentUserId) {
        return schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Tài khoản không thuộc trường học nào."));
    }

    /** Giáo viên cùng trường — kiểm tra trước khi gán bài cho họ. */
    public boolean isTeacherOfSchool(UUID userId, UUID schoolId) {
        boolean isTeacher = userRoleQueryRepository.findByUserIdWithRoleInfo(userId).stream()
            .anyMatch(role -> "TEACHER".equals(role.roleCode()));
        if (!isTeacher) {
            return false;
        }
        return schoolUserRepository.findByUserId(userId)
            .map(schoolUser -> schoolId.equals(schoolUser.getSchoolId()))
            .orElse(false);
    }
}
