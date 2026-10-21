package com.sep.vox.application.port.input.usecase.assessmentpaper;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.UpdateAssessmentPaperStatusCommand;
import com.sep.vox.application.port.input.service.AssessmentPaperAuthoringAccessService;
import com.sep.vox.application.port.input.service.AssessmentPaperAuthoringAccessService.PaperActor;
import com.sep.vox.application.port.input.service.AssessmentPaperAutoAssigner;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.dto.assessment.AssessmentPaperDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;

/**
 * Vòng đời một mã đề. Kỳ thi tập trung có hai đường đi, chọn theo <b>ai soạn ra mã đề đó</b>:
 *
 * <ul>
 *   <li><b>Có người thứ hai</b> — mã đề do người khác soạn: DRAFT → IN_REVIEW → APPROVED → LOCKED,
 *       với {@code requireNotAuthor} chặn người soạn tự duyệt bài của mình. Nhờ vậy trạng thái
 *       {@code APPROVED} luôn có nghĩa "đã qua mắt người thứ hai".</li>
 *   <li><b>Không có người thứ hai</b> — chủ tịch hội đồng hoặc quản trị trường tự soạn mã đề: đi tắt
 *       một bước DRAFT → LOCKED. Bắt họ bấm nộp duyệt rồi tự duyệt chính bài mình chỉ tạo ra một dấu
 *       {@code APPROVED} giả, còn cấm hẳn thì trường ít người không khoá nổi mã đề để phân đề.</li>
 * </ul>
 *
 * <p>Bài trên lớp luôn đi đường tắt: giáo viên tạo bài vừa là CHAIR vừa là người soạn mọi mã đề.
 */
@Service
public class UpdateAssessmentPaperStatusUseCase implements IUseCase<UpdateAssessmentPaperStatusCommand, AssessmentPaperDto> {

    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentPaperAuthoringAccessService assessmentPaperAuthoringAccessService;
    private final AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService;
    private final AssessmentPaperAutoAssigner assessmentPaperAutoAssigner;
    private final UserContextPort userContextPort;

    public UpdateAssessmentPaperStatusUseCase(
            AssessmentPaperRepository assessmentPaperRepository,
            AssessmentPaperItemRepository assessmentPaperItemRepository,
            AssessmentRepository assessmentRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentPaperAuthoringAccessService assessmentPaperAuthoringAccessService,
            AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService,
            AssessmentPaperAutoAssigner assessmentPaperAutoAssigner,
            UserContextPort userContextPort) {
        this.assessmentPaperRepository = assessmentPaperRepository;
        this.assessmentPaperItemRepository = assessmentPaperItemRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentPaperAuthoringAccessService = assessmentPaperAuthoringAccessService;
        this.assessmentTimeQuotaGuardService = assessmentTimeQuotaGuardService;
        this.assessmentPaperAutoAssigner = assessmentPaperAutoAssigner;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentPaperDto execute(UpdateAssessmentPaperStatusCommand input) {
        var command = new UpdateAssessmentPaperStatusCommand(
            input.paperId(),
            StringNormalization.normalizeCode(input.action()),
            StringNormalization.trimAndCollapseSpaces(input.note())
        );
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var paper = assessmentPaperRepository.findById(command.paperId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        var assessment = assessmentRepository.findById(paper.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        var actor = assessmentPaperAuthoringAccessService.resolve(assessment, currentUserId);

        switch (command.action()) {
            case "SUBMIT" -> {
                requireCanAuthor(actor);
                requireAllSlotsAssigned(paper, "Đề thi còn ô câu hỏi chưa được gán, không thể nộp duyệt");
                requirePaperWithinPlan(assessment, paper);
                requireTransition(paper, AssessmentPaperStatus.DRAFT, AssessmentPaperStatus.IN_REVIEW);
            }
            case "APPROVE" -> {
                requireCanReview(actor);
                requireNotAuthor(paper, currentUserId);
                requirePaperWithinPlan(assessment, paper);
                requireTransition(paper, AssessmentPaperStatus.IN_REVIEW, AssessmentPaperStatus.APPROVED);
            }
            case "REQUEST_REVISION" -> {
                requireCanReview(actor);
                requireNotAuthor(paper, currentUserId);
                if (command.note() == null || command.note().isBlank()) {
                    throw new IllegalStateException("Yêu cầu sửa lại bắt buộc phải có góp ý");
                }
                requireTransition(paper, AssessmentPaperStatus.IN_REVIEW, AssessmentPaperStatus.DRAFT);
            }
            case "LOCK" -> {
                requireCanDecide(actor);
                requirePaperWithinPlan(assessment, paper);
                if (isOneStepLock(assessment, paper, currentUserId)) {
                    requireAllSlotsAssigned(paper, "Mã đề còn ô câu hỏi chưa được gán, không thể khoá");
                    requireTransition(paper, AssessmentPaperStatus.DRAFT, AssessmentPaperStatus.LOCKED);
                } else {
                    requireNotAuthor(paper, currentUserId);
                    requireTransition(paper, AssessmentPaperStatus.APPROVED, AssessmentPaperStatus.LOCKED);
                }
            }
            case "REOPEN" -> {
                // Không có requireNotAuthor ở đây: người vừa đi đường tắt DRAFT → LOCKED phải mở lại
                // được chính mã đề của mình, nếu không họ tự khoá mình ra ngoài và mã đề kẹt vĩnh viễn.
                requireCanDecide(actor);
                requireTransition(paper, AssessmentPaperStatus.LOCKED, AssessmentPaperStatus.DRAFT);
                unassignCandidatesOf(paper, currentUserId);
            }
            default -> throw new IllegalStateException("Action không hợp lệ");
        }

        var now = Instant.now();
        paper.setUpdatedAt(now);
        paper.setUpdatedBy(currentUserId);
        var saved = assessmentPaperRepository.save(paper);

        // Gán bù SAU khi lưu: AssessmentPaperAutoAssigner đọc lại trạng thái mã đề để chốt "mọi mã đề đã
        // LOCKED", nên trạng thái mới phải nằm trong DB trước đã.
        if ("LOCK".equals(command.action())) {
            assessmentPaperAutoAssigner.backfillAssessment(assessment, now, currentUserId);
        }
        return AssessmentPaperDto.toDto(saved);
    }

    /**
     * Mở khoá là để sửa lại mã đề, mà thí sinh đang trỏ vào nó thì bài thi bị đổi dưới chân họ. Trả
     * họ về "chưa phân đề" -- cùng cách xử lý với {@code DeleteAssessmentPaperUseCase}. Khoá lại thì
     * {@link AssessmentPaperAutoAssigner#backfillAssessment} gán bù, nên không ai kẹt vĩnh viễn.
     */
    private void unassignCandidatesOf(AssessmentPaper paper, UUID currentUserId) {
        var affected = assessmentCandidateRepository.findByAssignedPaperId(paper.getId());
        if (affected.isEmpty()) {
            return;
        }
        var now = Instant.now();
        affected.forEach(candidate -> candidate.unassignPaper(now, currentUserId));
        assessmentCandidateRepository.saveAll(affected);
    }

    /**
     * Đi tắt khi không có người thứ hai để duyệt: bài trên lớp (chỉ có một CHAIR), hoặc kỳ thi tập
     * trung mà chính người quyết định là người soạn ra mã đề này.
     */
    private boolean isOneStepLock(Assessment assessment, AssessmentPaper paper, UUID currentUserId) {
        return assessment.getKind() == AssessmentKind.CLASS_TEST || currentUserId.equals(paper.getCreatedBy());
    }

    private void requireCanAuthor(PaperActor actor) {
        if (!actor.canAuthor()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }

    private void requireCanReview(PaperActor actor) {
        if (!actor.canReview()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }

    private void requireCanDecide(PaperActor actor) {
        if (!actor.canDecide()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }

    private void requireNotAuthor(AssessmentPaper paper, UUID currentUserId) {
        if (currentUserId.equals(paper.getCreatedBy())) {
            throw new ForbiddenException("Không được tự duyệt đề thi do chính mình tạo");
        }
    }

    private void requireAllSlotsAssigned(AssessmentPaper paper, String message) {
        if (assessmentPaperItemRepository.existsUnassignedItemByPaperId(paper.getId())) {
            throw new IllegalStateException(message);
        }
    }

    private void requirePaperWithinPlan(Assessment assessment, AssessmentPaper paper) {
        assessmentTimeQuotaGuardService.requireWithinPlan(
            assessment.getSchoolId(),
            paper.getTimeDurationSeconds(),
            "Mã đề " + paper.getCode()
        );
    }

    private void requireTransition(AssessmentPaper paper, AssessmentPaperStatus from, AssessmentPaperStatus to) {
        if (paper.getStatus() != from) {
            throw new IllegalStateException("Trạng thái đề thi hiện tại không hợp lệ cho action này");
        }
        paper.setStatus(to);
    }
}
