package com.sep.vox.application.port.input.usecase.assessmentpaper;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import java.time.Instant;
import java.util.List;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteAssessmentPaperCommand;
import com.sep.vox.application.port.input.service.AssessmentPaperAuthoringAccessService;
import com.sep.vox.application.port.input.service.RecalculateAssessmentTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;

@Service
public class DeleteAssessmentPaperUseCase implements IUseCase<DeleteAssessmentPaperCommand, Void> {

    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentPaperAuthoringAccessService assessmentPaperAuthoringAccessService;
    private final RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService;
    private final UserContextPort userContextPort;

    public DeleteAssessmentPaperUseCase(
            AssessmentPaperRepository assessmentPaperRepository,
            AssessmentPaperSectionRepository assessmentPaperSectionRepository,
            AssessmentPaperItemRepository assessmentPaperItemRepository,
            AssessmentRepository assessmentRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentPaperAuthoringAccessService assessmentPaperAuthoringAccessService,
            RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService,
            UserContextPort userContextPort) {
        this.assessmentPaperRepository = assessmentPaperRepository;
        this.assessmentPaperSectionRepository = assessmentPaperSectionRepository;
        this.assessmentPaperItemRepository = assessmentPaperItemRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentPaperAuthoringAccessService = assessmentPaperAuthoringAccessService;
        this.recalculateAssessmentTimeDurationService = recalculateAssessmentTimeDurationService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteAssessmentPaperCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        AssessmentPaper paper = assessmentPaperRepository.findById(input.paperId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));
        Assessment assessment = assessmentRepository.findById(paper.getAssessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        AssessmentPaperAuthoringAccessService.PaperActor actor = assessmentPaperAuthoringAccessService.requireCanAuthor(assessment, currentUserId);
        // Once an assessment has started, a paper variant is live data -- the same boundary that AssignAssessmentPapersUseCase
        // builds for the paper assignment step. Previously this use case only checked the status of the paper variant, so REOPENing a
        // locked paper variant and then deleting it in the middle of the session still slipped through.
        AssessmentEditingGuard.requireAssessmentEditable(assessment);
        // Người ra đề chỉ xoá được đề của chính mình. Chủ tịch hội đồng và quản trị trường xoá được mọi
        // mã đề: đổi khung đề bắt buộc phải xoá sạch mã đề hiện có, mà người ra đề cũ có thể đã rời hội
        // đồng — không có nhánh này thì kỳ thi kẹt vĩnh viễn với khung đề cũ.
        if (!actor.canDecide() && !currentUserId.equals(paper.getCreatedBy())) {
            throw new ForbiddenException("Chỉ người đã tạo đề thi này mới được xoá");
        }
        if (paper.getStatus() != AssessmentPaperStatus.DRAFT) {
            throw new IllegalStateException("Chỉ xoá được đề thi khi còn ở trạng thái DRAFT");
        }

        // Phiên thi trỏ vào mã đề qua paper_id NOT NULL: đã có học sinh làm bài thì không có cách nào
        // dọn con trỏ, chỉ còn cách không cho xoá.
        if (assessmentSessionRepository.existsByPaperId(paper.getId())) {
            throw new IllegalStateException("Không thể xoá mã đề đã có học sinh làm bài");
        }

        // Candidates also point at a paper variant through assigned_paper_id, and no FK cleans that up either. Return them to
        // "no paper assigned" instead of leaving a pointer to a row that has disappeared: UpdateAssessmentStatusUseCase
        // counts exactly the candidates without a paper variant to block scheduling, so an orphan pointer would pass through that
        // gate and only blow up when the student enters the assessment room.
        List<AssessmentCandidate> affectedCandidates = assessmentCandidateRepository.findByAssignedPaperId(paper.getId());
        if (!affectedCandidates.isEmpty()) {
            Instant now = Instant.now();
            affectedCandidates.forEach(candidate -> candidate.unassignPaper(now, currentUserId));
            assessmentCandidateRepository.saveAll(affectedCandidates);
        }

        // Phần thi và câu trong đề treo trên paper_id mà không có FK nào dọn hộ: xoá mỗi dòng đề là
        // để lại section/item mồ côi. Con trước cha.
        List<UUID> paperIds = List.of(paper.getId());
        assessmentPaperItemRepository.deleteByPaperIdIn(paperIds);
        assessmentPaperSectionRepository.deleteByPaperIdIn(paperIds);
        assessmentPaperRepository.deleteById(paper.getId());
        recalculateAssessmentTimeDurationService.recalculate(paper.getAssessmentId());
        return null;
    }
}
