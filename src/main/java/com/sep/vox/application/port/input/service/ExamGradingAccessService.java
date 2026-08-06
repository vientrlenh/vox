package com.sep.vox.application.port.input.service;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
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
    private final ExamMemberRepository examMemberRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public ExamGradingAccessService(
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamSessionRepository examSessionRepository,
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examSessionRepository = examSessionRepository;
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
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
        var assignment = examGradingAssignmentRepository.findByIdForUpdate(assignmentId)
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
    public boolean isClassTestChair(UUID examId, UUID currentUserId) {
        if (examId == null || currentUserId == null) {
            return false;
        }
        var exam = examRepository.findById(examId).orElse(null);
        if (exam == null || exam.getKind() != ExamKind.CLASS_TEST) {
            return false;
        }
        return examMemberRepository.existsByExamIdAndUserIdAndRole(examId, currentUserId, ExamMemberRole.CHAIR);
    }

    public void authorizeClassTestChair(UUID examId, UUID currentUserId) {
        if (isClassTestChair(examId, currentUserId)) {
            return;
        }
        throw new ForbiddenException("BẢO MẬT: Bạn không phải giáo viên tạo bài kiểm tra trên lớp này.");
    }

    /** School admin cùng trường, HOẶC giáo viên tạo bài kiểm tra trên lớp đó. */
    public void authorizeSchoolAdminOrClassTestChair(UUID schoolId, UUID examId, UUID currentUserId) {
        if (isSchoolAdminOfSchool(schoolId, currentUserId) || isClassTestChair(examId, currentUserId)) {
            return;
        }
        throw new ForbiddenException("BẢO MẬT: Bạn không có quyền thao tác trên bài kiểm tra này.");
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
        return examCandidateResultRepository.findById(candidateResultId)
            .map(result -> isClassTestExam(result.getExamId()))
            .orElse(false);
    }

    /** Như {@link #isClassTestResult} nhưng theo lô — hai query cho cả danh sách, không N+1. */
    public Set<UUID> classTestResultIds(Collection<UUID> candidateResultIds) {
        if (candidateResultIds == null || candidateResultIds.isEmpty()) {
            return Set.of();
        }
        var results = examCandidateResultRepository.findByIdIn(candidateResultIds);
        var classTestExamIds = examRepository
            .findByIdIn(results.stream().map(result -> result.getExamId()).distinct().toList()).stream()
            .filter(exam -> exam.getKind() == ExamKind.CLASS_TEST)
            .map(exam -> exam.getId())
            .collect(Collectors.toSet());
        return results.stream()
            .filter(result -> classTestExamIds.contains(result.getExamId()))
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

    public boolean isClassTestExam(UUID examId) {
        if (examId == null) {
            return false;
        }
        return examRepository.findById(examId)
            .map(exam -> exam.getKind() == ExamKind.CLASS_TEST)
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
