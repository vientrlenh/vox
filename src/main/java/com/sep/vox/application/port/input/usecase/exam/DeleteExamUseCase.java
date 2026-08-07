package com.sep.vox.application.port.input.usecase.exam;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteExamCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.exam.DeleteExamResponse;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Xoá kỳ thi. Chỉ kỳ thi còn DRAFT mới được xoá cứng — từ lúc lên lịch trở đi ca thi đã công bố
 * cho học sinh nên bài không được phép biến mất, chỉ chuyển CANCELLED. Ranh giới này khớp với
 * {@code UpdateExamStatusUseCase.CANCELLABLE_FROM}: kết quả đã công bố thì không rút lại được.
 *
 * <p>Nhánh xoá cứng phải tự dọn hết dữ liệu con: không bảng nào có FK hay ON DELETE CASCADE trỏ về
 * {@code exams}, nên bỏ sót một bảng là để lại ca thi/thí sinh mồ côi — chúng rơi khỏi mọi màn hình
 * dùng JOIN nhưng vẫn hiện ở màn hình nào nạp theo id, ví dụ lịch thi của học sinh.
 */
@Service
public class DeleteExamUseCase implements IUseCase<DeleteExamCommand, DeleteExamResponse> {

    private final ExamRepository examRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperSectionRepository examPaperSectionRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamQuestionSecureLockService examQuestionSecureLockService;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public DeleteExamUseCase(
            ExamRepository examRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamScheduleProctorRepository examScheduleProctorRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamPaperRepository examPaperRepository,
            ExamPaperSectionRepository examPaperSectionRepository,
            ExamPaperItemRepository examPaperItemRepository,
            ExamMemberRepository examMemberRepository,
            ExamQuestionSecureLockService examQuestionSecureLockService,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examPaperRepository = examPaperRepository;
        this.examPaperSectionRepository = examPaperSectionRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.examMemberRepository = examMemberRepository;
        this.examQuestionSecureLockService = examQuestionSecureLockService;
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

        return switch (exam.getStatus()) {
            case RESULTS_PUBLISHED ->
                throw new IllegalStateException("Không thể xoá kỳ thi đã công bố kết quả");
            // Đã huỷ rồi thì bấm xoá lần nữa không có gì để làm: trả đúng kết quả cũ thay vì ném lỗi.
            case CANCELLED -> new DeleteExamResponse(false, true);
            case DRAFT -> {
                cascadeDelete(exam.getId(), currentUserId);
                yield new DeleteExamResponse(true, false);
            }
            default -> {
                cancel(exam, currentUserId);
                yield new DeleteExamResponse(false, true);
            }
        };
    }

    private void cancel(Exam exam, UUID currentUserId) {
        var now = Instant.now();
        exam.setStatus(ExamStatus.CANCELLED);
        exam.setUpdatedAt(now);
        exam.setUpdatedBy(currentUserId);
        examRepository.save(exam);
        cancelSchedules(exam.getId(), currentUserId, now);
    }

    /**
     * Huỷ kỳ thi phải kéo theo ca thi: ca còn PUBLISHED vẫn lọt qua {@code isVisibleToStudent()} và
     * {@code allowsAttendance()}, và các truy vấn vào ca ({@code findByIdAndInSchedule}...) hard-code
     * {@code status = 'PUBLISHED'} — bỏ ca lại là để nguyên một ca "sẵn sàng" của kỳ thi đã huỷ.
     *
     * <p>Chỉ đụng DRAFT/PUBLISHED, đúng luật của {@code UpdateExamScheduleStatusUseCase.cancel}.
     * COMPLETED/MOVED là trạng thái kết thúc: ghi đè sẽ xoá dấu vết ca đã thi xong và làm lệch
     * {@code movedToScheduleId}. DELETED đã bị {@code findByExamId} lọc sẵn. Đây là cascade nên gặp
     * trạng thái không huỷ được thì bỏ qua chứ không ném lỗi.
     *
     * <p>Bản song sinh nằm ở {@code UpdateExamStatusUseCase} (action CANCEL) — sửa thì sửa cả hai.
     */
    private void cancelSchedules(UUID examId, UUID currentUserId, Instant now) {
        for (var schedule : examScheduleRepository.findByExamId(examId)) {
            if (schedule.getStatus() != ExamScheduleStatus.DRAFT
                    && schedule.getStatus() != ExamScheduleStatus.PUBLISHED) {
                continue;
            }
            schedule.setStatus(ExamScheduleStatus.CANCELLED);
            schedule.setUpdatedAt(now);
            schedule.setUpdatedBy(currentUserId);
            examScheduleRepository.save(schedule);
        }
    }

    /**
     * Xoá con trước cha. Không có FK nào chặn nên sai thứ tự hay bỏ sót bảng đều không báo lỗi —
     * chỉ lặng lẽ để lại dòng mồ côi.
     */
    private void cascadeDelete(UUID examId, UUID currentUserId) {
        // Câu hỏi bị khoá trỏ vào exam_secure_pools của kỳ thi: xoá pool mà quên mở khoá thì câu hỏi
        // nằm lại ngân hàng đề ở trạng thái khoá vĩnh viễn.
        examQuestionSecureLockService.releaseAllForExam(examId, currentUserId);

        var paperIds = examPaperRepository.findByExamId(examId).stream()
            .map(paper -> paper.getId())
            .toList();
        if (!paperIds.isEmpty()) {
            examPaperItemRepository.deleteByPaperIdIn(paperIds);
            examPaperSectionRepository.deleteByPaperIdIn(paperIds);
            examPaperRepository.deleteByExamId(examId);
        }

        // Cố ý dùng findAllIdsByExamId chứ không phải findByExamId: bản kia lọc bỏ ca đã xoá mềm,
        // và giám thị của những ca đó cũng phải được dọn.
        List<UUID> scheduleIds = examScheduleRepository.findAllIdsByExamId(examId);
        if (!scheduleIds.isEmpty()) {
            examScheduleProctorRepository.deleteByScheduleIdIn(scheduleIds);
        }
        examCandidateRepository.deleteByExamId(examId);
        examScheduleRepository.deleteByExamId(examId);

        examMemberRepository.deleteByExamId(examId);
        examRepository.deleteById(examId);
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
