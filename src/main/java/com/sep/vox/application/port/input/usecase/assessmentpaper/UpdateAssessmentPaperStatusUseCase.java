package com.sep.vox.application.port.input.usecase.assessmentpaper;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentpaper.UpdateAssessmentPaperStatusCommand;
import com.sep.vox.application.port.input.service.AssessmentPaperAuthoringAccessService;
import com.sep.vox.application.port.input.service.AssessmentPaperAuthoringAccessService.PaperActor;
import com.sep.vox.application.port.input.service.AssessmentPaperAutoAssigner;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentPaperDto;
import com.sep.vox.domain.mapper.AssessmentPaperDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

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
@RequiredArgsConstructor
public class UpdateAssessmentPaperStatusUseCase implements IUseCase<UpdateAssessmentPaperStatusCommand, AssessmentPaperDto> {

    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentPaperAuthoringAccessService assessmentPaperAuthoringAccessService;
    private final AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService;
    private final AssessmentPaperAutoAssigner assessmentPaperAutoAssigner;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentPaperDto execute(UpdateAssessmentPaperStatusCommand input) {
        UpdateAssessmentPaperStatusCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        AssessmentPaper paper = assessmentPaperRepository.findById(command.paperId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        Assessment assessment = assessmentRepository.findById(paper.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        PaperActor actor = assessmentPaperAuthoringAccessService.resolve(assessment, currentUserId);

        applyAction(command, paper, assessment, actor, currentUserId);

        Instant now = Instant.now();
        AssessmentPaper saved = assessmentPaperRepository.save(paper);

        // Backfill AFTER saving: AssessmentPaperAutoAssigner re-reads the paper variant statuses to decide "every paper variant is
        // LOCKED", so the new status has to be in the DB first.
        if ("LOCK".equals(command.action())) {
            assessmentPaperAutoAssigner.backfillAssessment(assessment, now, currentUserId);
        }
        return AssessmentPaperDtoMapper.toDto(saved);
    }

    private UpdateAssessmentPaperStatusCommand normalize(UpdateAssessmentPaperStatusCommand input) {
        return new UpdateAssessmentPaperStatusCommand(
            input.paperId(),
            StringNormalization.normalizeCode(input.action()),
            StringNormalization.trimAndCollapseSpaces(input.note())
        );
    }

    private void applyAction(
            UpdateAssessmentPaperStatusCommand command,
            AssessmentPaper paper,
            Assessment assessment,
            PaperActor actor,
            UUID currentUserId) {
        switch (command.action()) {
            case "SUBMIT" -> submit(paper, assessment, actor);
            case "APPROVE" -> approve(paper, assessment, actor, currentUserId);
            case "REQUEST_REVISION" -> requestRevision(command, paper, actor, currentUserId);
            case "LOCK" -> lock(paper, assessment, actor, currentUserId);
            case "REOPEN" -> reopen(paper, actor, currentUserId);
            default -> throw new IllegalArgumentException("Action không hợp lệ");
        }
    }

    private void submit(AssessmentPaper paper, Assessment assessment, PaperActor actor) {
        requireCanAuthor(actor);
        requireAllSlotsAssigned(paper, "Đề thi còn ô câu hỏi chưa được gán, không thể nộp duyệt");
        requirePaperWithinPlan(assessment, paper);
        requireTransition(paper, AssessmentPaperStatus.DRAFT, AssessmentPaperStatus.IN_REVIEW);
    }

    private void approve(AssessmentPaper paper, Assessment assessment, PaperActor actor, UUID currentUserId) {
        requireCanReview(actor);
        requireNotAuthor(paper, currentUserId);
        requirePaperWithinPlan(assessment, paper);
        requireTransition(paper, AssessmentPaperStatus.IN_REVIEW, AssessmentPaperStatus.APPROVED);
    }

    private void requestRevision(UpdateAssessmentPaperStatusCommand command, AssessmentPaper paper, PaperActor actor, UUID currentUserId) {
        requireCanReview(actor);
        requireNotAuthor(paper, currentUserId);
        if (command.note() == null || command.note().isBlank()) {
            throw new IllegalArgumentException("Yêu cầu sửa lại bắt buộc phải có góp ý");
        }
        requireTransition(paper, AssessmentPaperStatus.IN_REVIEW, AssessmentPaperStatus.DRAFT);
    }

    private void lock(AssessmentPaper paper, Assessment assessment, PaperActor actor, UUID currentUserId) {
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

    private void reopen(AssessmentPaper paper, PaperActor actor, UUID currentUserId) {
        // No requireNotAuthor here: someone who just took the DRAFT -> LOCKED shortcut must be able to reopen
        // their own paper variant, otherwise they lock themselves out and the paper variant is stuck forever.
        requireCanDecide(actor);
        requireTransition(paper, AssessmentPaperStatus.LOCKED, AssessmentPaperStatus.DRAFT);
        unassignCandidatesOf(paper, currentUserId);
    }

    /**
     * Unlocking is for editing the paper variant again, and a candidate pointing at it would have the attempt changed under their feet. Return
     * them to "no paper assigned" -- the same handling as {@code DeleteAssessmentPaperUseCase}. Once it is locked again,
     * {@link AssessmentPaperAutoAssigner#backfillAssessment} backfills, so nobody is stuck forever.
     */
    private void unassignCandidatesOf(AssessmentPaper paper, UUID currentUserId) {
        List<AssessmentCandidate> affected = assessmentCandidateRepository.findByAssignedPaperId(paper.getId());
        if (affected.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        affected.forEach(candidate -> candidate.unassignPaper(now, currentUserId));
        assessmentCandidateRepository.saveAll(affected);
    }

    /**
     * Đi tắt khi không có người thứ hai để duyệt: bài trên lớp (chỉ có một CHAIR), hoặc kỳ thi tập
     * trung mà chính người quyết định là người soạn ra mã đề này.
     */
    private boolean isOneStepLock(Assessment assessment, AssessmentPaper paper, UUID currentUserId) {
        return assessment.getKind() == AssessmentKind.CLASS_TEST || paper.isCreatedBy(currentUserId);
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
        if (paper.isCreatedBy(currentUserId)) {
            throw new ForbiddenException("Không được tự duyệt đề thi do chính mình tạo");
        }
    }

    private void requireAllSlotsAssigned(AssessmentPaper paper, String message) {
        if (assessmentPaperItemRepository.existsUnassignedItemByPaperId(paper.getId())) {
            throw new IllegalArgumentException(message);
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
            throw new IllegalArgumentException("Trạng thái đề thi hiện tại không hợp lệ cho action này");
        }
        paper.setStatus(to);
    }
}
