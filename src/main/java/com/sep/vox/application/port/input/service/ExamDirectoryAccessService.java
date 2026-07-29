package com.sep.vox.application.port.input.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Ai được xem danh bạ nguồn thí sinh / giám thị của một kỳ thi, và xem tới đâu.
 *
 * <p>Sinh ra để vá một lệch quyền cụ thể: các use case nhập thí sinh
 * ({@code ImportExamCandidatesFrom*UseCase}, {@code AddExamCandidateUseCase}) cho phép
 * CHAIR ghi, nhưng danh sách lớp / niên khóa / học sinh mà họ phải chọn lại nằm sau
 * các query GraphQL gated {@code SCHOOL_ADMIN}. Quyền đọc và quyền ghi giờ đi chung
 * một luật ở đây.
 *
 * <p>Phạm vi tách theo {@link ExamKind}: kỳ thi tập trung cần lớp mà chủ tịch hội đồng
 * không dạy, nên mở toàn trường; còn bài trên lớp thì người tạo tự động thành CHAIR
 * ({@code CreateClassTestUseCase}) — mở toàn trường ở đó là phát quyền đọc danh bạ
 * trường cho mọi giáo viên.
 */
@Service
public class ExamDirectoryAccessService {

    /** Trần số lớp gom được khi giải phạm vi "lớp của tôi" — khớp các use case nhập thí sinh. */
    private static final int MAX_CLASSES = 200;

    private final ExamRepository examRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ExamMemberRepository examMemberRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public ExamDirectoryAccessService(
            ExamRepository examRepository,
            SchoolUserRepository schoolUserRepository,
            SchoolClassRepository schoolClassRepository,
            ExamMemberRepository examMemberRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.examMemberRepository = examMemberRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    /**
     * @param schoolWide true = toàn bộ trường của kỳ thi; false = chỉ những lớp mà
     *                   {@code callerId} là thành viên.
     */
    public record ExamDirectoryScope(UUID callerId, UUID schoolId, boolean schoolWide) {
    }

    public Exam requireExam(UUID examId) {
        return examRepository.findById(examId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
    }

    public ExamDirectoryScope resolveByExamId(UUID examId) {
        return resolve(requireExam(examId));
    }

    public ExamDirectoryScope resolve(Exam exam) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(exam.getSchoolId())) {
            return new ExamDirectoryScope(currentUserId, exam.getSchoolId(), true);
        }
        if (examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.CHAIR)) {
            return new ExamDirectoryScope(
                currentUserId, exam.getSchoolId(), exam.getKind() != ExamKind.CLASS_TEST);
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }

    /**
     * Các lớp đang hoạt động mà người gọi là thành viên, trong trường của kỳ thi.
     * Chỉ có nghĩa khi {@code scope.schoolWide()} là false.
     */
    public List<UUID> callerClassIds(ExamDirectoryScope scope) {
        // findByUserId là 1-based (PageRequest.of(page - 1, size)) → trang đầu là 1, KHÔNG phải 0.
        return schoolClassRepository
            .findByUserId(scope.schoolId(), scope.callerId(), null, SchoolClassStatus.ACTIVE, 1, MAX_CLASSES)
            .content()
            .stream()
            .map(SchoolClass::getId)
            .toList();
    }
}
