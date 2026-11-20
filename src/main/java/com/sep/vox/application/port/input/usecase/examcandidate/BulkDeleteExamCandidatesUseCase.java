package com.sep.vox.application.port.input.usecase.examcandidate;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.BulkDeleteExamCandidatesCommand;
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
 * Xoá cả nhóm thí sinh khỏi kỳ thi trong đúng MỘT transaction.
 *
 * <p>Cùng luật với {@link DeleteExamCandidateUseCase} cho từng người, chỉ khác ở chỗ soát cả lô
 * bằng hai query rồi mới đụng vào dữ liệu. Để giao diện gọi endpoint xoá từng người N lần thì xoá
 * 40 thí sinh mà hỏng ở người thứ 25 sẽ để lại danh sách dở dang — giống lý do
 * {@code BulkAssignExamCandidateScheduleUseCase} tồn tại.
 *
 * <p>All-or-nothing: chỉ cần MỘT thí sinh trong lô đã có bài thi là cả lượt bị từ chối, kèm tên
 * đích danh những người vướng. Xoá một phần rồi báo lỗi chung chung sẽ khiến người dùng không biết
 * ai đã bị xoá và ai chưa, mà đây là thao tác không hoàn tác được.
 */
@Service
public class BulkDeleteExamCandidatesUseCase implements IUseCase<BulkDeleteExamCandidatesCommand, Void> {

    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public BulkDeleteExamCandidatesUseCase(
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
    public Void execute(BulkDeleteExamCandidatesCommand input) {
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        authorize(exam);
        ExamEditingGuard.requireScheduleEditable(exam);

        var candidateIds = input.candidateIds() == null ? List.<UUID>of() : input.candidateIds();
        if (candidateIds.isEmpty()) {
            return null;
        }

        var candidates = examCandidateRepository.findByIdInAndExamId(candidateIds, exam.getId());
        // Tra theo (id, examId) nên thiếu dòng nghĩa là có id không tồn tại hoặc thuộc kỳ thi khác —
        // hỏng cả lượt thay vì âm thầm xoá một phần.
        if (candidates.size() != candidateIds.stream().distinct().count()) {
            throw new NotFoundException("Không tìm thấy thí sinh");
        }

        // Một query cho cả lô: phiên đã xoá mềm không tính (findByCandidateIdIn đã loại DELETED),
        // nên thí sinh chỉ còn lượt thi đã xoá vẫn gỡ được khỏi kỳ thi.
        var busyCandidateIds = examSessionRepository
            .findByCandidateIdIn(candidates.stream().map(candidate -> candidate.getId()).toList())
            .stream()
            .map(session -> session.getCandidateId())
            .collect(java.util.stream.Collectors.toSet());

        if (!busyCandidateIds.isEmpty()) {
            // Chỉ nêu SỐ LƯỢNG: ở tầng này chỉ có studentId dạng UUID, in ra thì người dùng cũng
            // không nhận ra ai. Giao diện đã loại sẵn nhóm này khỏi lượt xoá nên đây là lưới an
            // toàn cho trường hợp danh sách vừa đổi, và người dùng chỉ cần biết phải chọn lại.
            throw new IllegalStateException(busyCandidateIds.size()
                + " thí sinh trong danh sách đã có bài thi nên không thể xóa khỏi kỳ thi."
                + " Hãy tải lại danh sách và chọn lại.");
        }

        examCandidateRepository.deleteByIdIn(candidateIds);
        return null;
    }

    /** Cùng luật với {@link DeleteExamCandidateUseCase}: quản trị trường sở tại hoặc chủ tịch hội đồng. */
    private void authorize(Exam exam) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(exam.getSchoolId())) {
            return;
        }
        if (examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.CHAIR)) {
            return;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
