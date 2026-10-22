package com.sep.vox.application.port.input.usecase.assessment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteAssessmentCommand;
import com.sep.vox.application.port.input.service.AssessmentScheduleClosureService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.assessment.DeleteAssessmentResponse;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;

/**
 * Xoá kỳ thi. Chỉ kỳ thi còn DRAFT mới được xoá cứng — từ lúc lên lịch trở đi ca thi đã công bố
 * cho học sinh nên bài không được phép biến mất, chỉ chuyển CANCELLED. Ranh giới này khớp với
 * {@code UpdateAssessmentStatusUseCase.CANCELLABLE_FROM}: kết quả đã công bố thì không rút lại được.
 *
 * <p>Nhánh xoá cứng phải tự dọn hết dữ liệu con: không bảng nào có FK hay ON DELETE CASCADE trỏ về
 * {@code assessments}, nên bỏ sót một bảng là để lại ca thi/thí sinh mồ côi — chúng rơi khỏi mọi màn hình
 * dùng JOIN nhưng vẫn hiện ở màn hình nào nạp theo id, ví dụ lịch thi của học sinh.
 */
@Service
public class DeleteAssessmentUseCase implements IUseCase<DeleteAssessmentCommand, DeleteAssessmentResponse> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private final AssessmentScheduleClosureService assessmentScheduleClosureService;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public DeleteAssessmentUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentScheduleProctorRepository assessmentScheduleProctorRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentPaperRepository assessmentPaperRepository,
            AssessmentPaperSectionRepository assessmentPaperSectionRepository,
            AssessmentPaperItemRepository assessmentPaperItemRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            AssessmentQuestionSecureLockService assessmentQuestionSecureLockService,
            AssessmentScheduleClosureService assessmentScheduleClosureService,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentScheduleProctorRepository = assessmentScheduleProctorRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentPaperRepository = assessmentPaperRepository;
        this.assessmentPaperSectionRepository = assessmentPaperSectionRepository;
        this.assessmentPaperItemRepository = assessmentPaperItemRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.assessmentQuestionSecureLockService = assessmentQuestionSecureLockService;
        this.assessmentScheduleClosureService = assessmentScheduleClosureService;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public DeleteAssessmentResponse execute(DeleteAssessmentCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userContextPort.isSchoolAdmin();

        var assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        authorizeDelete(assessment.getId(), assessment.getSchoolId(), assessment.getKind(), currentUserId, currentSchoolId, schoolAdmin);

        return switch (assessment.getStatus()) {
            case RESULTS_PUBLISHED ->
                throw new IllegalStateException("Không thể xoá kỳ thi đã công bố kết quả");
            // Đã huỷ rồi thì bấm xoá lần nữa không có gì để làm: trả đúng kết quả cũ thay vì ném lỗi.
            case CANCELLED -> new DeleteAssessmentResponse(false, true);
            case DRAFT -> {
                cascadeDelete(assessment.getId(), currentUserId);
                yield new DeleteAssessmentResponse(true, false);
            }
            default -> {
                cancel(assessment, currentUserId);
                yield new DeleteAssessmentResponse(false, true);
            }
        };
    }

    private void cancel(Assessment assessment, UUID currentUserId) {
        var now = Instant.now();
        assessment.setStatus(AssessmentStatus.CANCELLED);
        assessment.setUpdatedAt(now);
        assessment.setUpdatedBy(currentUserId);
        assessmentRepository.save(assessment);
        // Huỷ kỳ thi phải kéo theo ca thi -- xem AssessmentScheduleClosureService để rõ vì sao.
        assessmentScheduleClosureService.cancelSchedulesForAssessment(assessment.getId(), currentUserId, now);
    }

    /**
     * Xoá con trước cha. Không có FK nào chặn nên sai thứ tự hay bỏ sót bảng đều không báo lỗi —
     * chỉ lặng lẽ để lại dòng mồ côi.
     */
    private void cascadeDelete(UUID assessmentId, UUID currentUserId) {
        // Câu hỏi bị khoá trỏ vào assessment_secure_pools của kỳ thi: xoá pool mà quên mở khoá thì câu hỏi
        // nằm lại ngân hàng đề ở trạng thái khoá vĩnh viễn.
        assessmentQuestionSecureLockService.releaseAllForAssessment(assessmentId, currentUserId);

        var paperIds = assessmentPaperRepository.findByAssessmentId(assessmentId).stream()
            .map(paper -> paper.getId())
            .toList();
        if (!paperIds.isEmpty()) {
            assessmentPaperItemRepository.deleteByPaperIdIn(paperIds);
            assessmentPaperSectionRepository.deleteByPaperIdIn(paperIds);
            assessmentPaperRepository.deleteByAssessmentId(assessmentId);
        }

        // Cố ý dùng findAllIdsByAssessmentId chứ không phải findByAssessmentId: bản kia lọc bỏ ca đã xoá mềm,
        // và giám thị của những ca đó cũng phải được dọn.
        List<UUID> scheduleIds = assessmentScheduleRepository.findAllIdsByAssessmentId(assessmentId);
        if (!scheduleIds.isEmpty()) {
            assessmentScheduleProctorRepository.deleteByScheduleIdIn(scheduleIds);
        }
        assessmentCandidateRepository.deleteByAssessmentId(assessmentId);
        assessmentScheduleRepository.deleteByAssessmentId(assessmentId);

        assessmentMemberRepository.deleteByAssessmentId(assessmentId);
        assessmentRepository.deleteById(assessmentId);
    }

    private void authorizeDelete(
            java.util.UUID assessmentId,
            java.util.UUID assessmentSchoolId,
            AssessmentKind kind,
            java.util.UUID currentUserId,
            java.util.UUID currentSchoolId,
            boolean schoolAdmin) {
        if (kind == AssessmentKind.CENTRALIZED) {
            if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessmentSchoolId)) {
                return;
            }
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (!assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, currentUserId, AssessmentMemberRole.CHAIR)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }
}
