package com.sep.vox.application.port.input.usecase.assessmentpaper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteAssessmentPaperCommand;
import com.sep.vox.application.port.input.service.AssessmentPaperAuthoringAccessService;
import com.sep.vox.application.port.input.service.RecalculateAssessmentTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;
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
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var paper = assessmentPaperRepository.findById(input.paperId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        var assessment = assessmentRepository.findById(paper.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));

        var actor = assessmentPaperAuthoringAccessService.requireCanAuthor(assessment, currentUserId);
        // Kỳ thi đã bắt đầu thì mã đề là dữ liệu đang chạy — cùng ranh giới mà AssignAssessmentPapersUseCase
        // dựng cho khâu phân đề. Trước đây use case này chỉ soi trạng thái mã đề, nên REOPEN một mã
        // đề đã khoá rồi xoá nó giữa lúc thi vẫn lọt.
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

        // Thí sinh cũng trỏ vào mã đề qua assigned_paper_id, cũng không có FK nào dọn hộ. Trả họ về
        // "chưa phân đề" thay vì để lại con trỏ tới một dòng đã biến mất: UpdateAssessmentStatusUseCase
        // đếm đúng những thí sinh chưa có mã đề để chặn lên lịch, nên con trỏ mồ côi sẽ lọt qua cửa
        // đó rồi mới nổ lúc học sinh vào phòng thi.
        var affectedCandidates = assessmentCandidateRepository.findByAssignedPaperId(paper.getId());
        if (!affectedCandidates.isEmpty()) {
            var now = Instant.now();
            affectedCandidates.forEach(candidate -> candidate.unassignPaper(now, currentUserId));
            assessmentCandidateRepository.saveAll(affectedCandidates);
        }

        // Phần thi và câu trong đề treo trên paper_id mà không có FK nào dọn hộ: xoá mỗi dòng đề là
        // để lại section/item mồ côi. Con trước cha.
        var paperIds = List.of(paper.getId());
        assessmentPaperItemRepository.deleteByPaperIdIn(paperIds);
        assessmentPaperSectionRepository.deleteByPaperIdIn(paperIds);
        assessmentPaperRepository.deleteById(paper.getId());
        recalculateAssessmentTimeDurationService.recalculate(paper.getAssessmentId());
        return null;
    }
}
