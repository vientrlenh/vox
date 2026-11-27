package com.sep.vox.application.port.input.usecase.assessmentpaper;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import java.time.Instant;
import java.util.List;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentpaper.DeleteAssessmentPaperCommand;
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
@RequiredArgsConstructor
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

    @Override
    @Transactional
    public Void execute(DeleteAssessmentPaperCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        AssessmentPaper paper = assessmentPaperRepository.findById(input.paperId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        Assessment assessment = assessmentRepository.findById(paper.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        AssessmentPaperAuthoringAccessService.PaperActor actor = assessmentPaperAuthoringAccessService.requireCanAuthor(assessment, currentUserId);
        // Once an assessment has started, a paper variant is live data -- the same boundary that AssignAssessmentPapersUseCase
        // builds for the paper assignment step. Previously this use case only checked the status of the paper variant, so REOPENing a
        // locked paper variant and then deleting it in the middle of the session still slipped through.
        AssessmentEditingGuard.requireAssessmentEditable(assessment);
        requireDeletable(paper, actor, currentUserId);
        unassignCandidates(paper, currentUserId);
        deletePaperRows(paper);
        recalculateAssessmentTimeDurationService.recalculate(paper.getAssessmentId());
        return null;
    }

    private void requireDeletable(AssessmentPaper paper, AssessmentPaperAuthoringAccessService.PaperActor actor, UUID currentUserId) {
        // The paper author may only delete their own paper. The council chair and the school admin may delete any
        // paper variant: changing the blueprint requires wiping every existing paper variant, and the original author may
        // have left the council already -- without this branch the assessment would be stuck forever on the old blueprint.
        if (!actor.canDecide() && !paper.isCreatedBy(currentUserId)) {
            throw new ForbiddenException("Chỉ người đã tạo đề thi này mới được xoá");
        }
        if (paper.getStatus() != AssessmentPaperStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ xoá được đề thi khi còn ở trạng thái DRAFT");
        }

        // An exam session points at the paper variant through paper_id NOT NULL: once a student has taken it there is
        // no way to clean the pointer up, so the only option is to refuse the delete.
        if (assessmentSessionRepository.existsByPaperId(paper.getId())) {
            throw new IllegalArgumentException("Không thể xoá mã đề đã có học sinh làm bài");
        }
    }

    private void unassignCandidates(AssessmentPaper paper, UUID currentUserId) {
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
    }

    private void deletePaperRows(AssessmentPaper paper) {
        // The paper's sections and items hang off paper_id and no FK cleans them up: deleting only the paper row
        // would leave orphaned sections/items. Children first, then the parent.
        List<UUID> paperIds = List.of(paper.getId());
        assessmentPaperItemRepository.deleteByPaperIdIn(paperIds);
        assessmentPaperSectionRepository.deleteByPaperIdIn(paperIds);
        assessmentPaperRepository.deleteById(paper.getId());
    }
}
