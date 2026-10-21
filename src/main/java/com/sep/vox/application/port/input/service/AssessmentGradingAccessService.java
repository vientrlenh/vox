package com.sep.vox.application.port.input.service;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;

/**
 * Giải chuỗi assignment -> candidate result -> session -> assessment -> school và phân
 * quyền theo đó.
 *
 * <p>Tách khỏi {@link AssessmentSessionModerationAccessService}: quyền chấm tay không đến
 * từ vai trò coi thi mà từ <em>chính dòng phân công</em>. Giáo viên được gán bài
 * nào thì thao tác được đúng bài đó — kể cả gỡ cờ nghi vấn — và không cần là thành
 * viên kỳ thi.
 */
@Service
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

    public AssessmentGradingAccessService(
            AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentRepository assessmentRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.assessmentGradingAssignmentRepository = assessmentGradingAssignmentRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    /**
     * Ngữ cảnh đã giải xong của một phân công chấm bài.
     *
     * @param assessmentKind lấy sẵn từ Assessment đã nạp ở {@link #loadFromCandidateResult}. Các event
     *        phát ra từ luồng chấm mang theo trường này để thông báo biết mở màn hình của
     *        bài tập trung hay bài kiểm tra lớp -- hai nhánh có route khác nhau, mà nơi
     *        phát event thì không còn giữ Assessment trong tay.
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
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản bị khóa.");
        }
        return currentUserId;
    }

    @Transactional(readOnly = true)
    public GradingContext load(UUID assignmentId) {
        var assignment = assessmentGradingAssignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công chấm bài."));
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
        var assignment = assessmentGradingAssignmentRepository.findByIdForUpdate(assignmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công chấm bài."));
        return loadFromCandidateResult(assignment, assignment.getCandidateResultId());
    }

    @Transactional(readOnly = true)
    public GradingContext loadByCandidateResultId(UUID candidateResultId) {
        var assignment = assessmentGradingAssignmentRepository.findOpenByCandidateResultId(candidateResultId)
            .orElse(null);
        return loadFromCandidateResult(assignment, candidateResultId);
    }

    private GradingContext loadFromCandidateResult(AssessmentGradingAssignment assignment, UUID candidateResultId) {
        var candidateResult = assessmentCandidateResultRepository.findById(candidateResultId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kết quả bài thi."));
        var session = assessmentSessionRepository.findById(candidateResult.getSessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi."));
        var assessment = assessmentRepository.findById(candidateResult.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra."));
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
        var assignment = context.assignment();
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
        var assessment = assessmentRepository.findById(assessmentId).orElse(null);
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
     * CHAIR của kỳ thi này, KHÔNG phân biệt loại kỳ thi — chốt quyền cho các lối ĐỌC
     * đóng phạm vi trong đúng một kỳ thi.
     *
     * <p>Khác {@link #isClassTestChair} ở đúng một điểm: không có cửa {@code CLASS_TEST}.
     * Cửa đó bảo vệ quyền GHI của luồng chấm — ai được chấm bài nào, bài chuyển sang
     * trạng thái nào — và lý do nó chặn chủ tịch kỳ thi tập trung là vì bài tập trung do
     * nhà trường điều phối. Lý do ấy không áp cho việc đọc: chủ tịch bấm được nút công bố
     * kết quả ({@code UpdateAssessmentStatusUseCase.authorizeMutation}) trong khi
     * {@code requirePublishReadiness} đòi mọi bài đã RELEASED hoặc INVALID, nên không cho
     * họ nhìn thứ đang chặn nút đó là bắt họ bấm mù rồi không có đường nào tự gỡ.
     *
     * <p>⚠️ CHỈ dùng cho use case ĐỌC. Mọi thao tác điều phối chấm (gán / đổi / gỡ phân
     * công, chốt sổ) vẫn phải đi qua {@link #isClassTestChair}: hàng đợi giáo viên là tài
     * nguyên chung của cả trường và cân theo tải toàn trường, nên một chủ tịch không được
     * tự tiêu nó cho kỳ thi của mình.
     */
    public boolean isAssessmentChair(UUID assessmentId, UUID currentUserId) {
        if (assessmentId == null || currentUserId == null) {
            return false;
        }
        return assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR);
    }

    /** School admin cùng trường, HOẶC chủ tịch của chính kỳ thi đó. Chỉ cho lối ĐỌC — xem {@link #isAssessmentChair}. */
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
        var results = assessmentCandidateResultRepository.findByIdIn(candidateResultIds);
        var classTestAssessmentIds = assessmentRepository
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
        var isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
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
