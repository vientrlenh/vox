package com.sep.vox.application.port.input.usecase.exampaper;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteExamPaperCommand;
import com.sep.vox.application.port.input.service.ExamPaperAuthoringAccessService;
import com.sep.vox.application.port.input.service.RecalculateExamTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;

@Service
public class DeleteExamPaperUseCase implements IUseCase<DeleteExamPaperCommand, Void> {

    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperSectionRepository examPaperSectionRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final ExamRepository examRepository;
    private final ExamPaperAuthoringAccessService examPaperAuthoringAccessService;
    private final RecalculateExamTimeDurationService recalculateExamTimeDurationService;
    private final UserContextPort userContextPort;

    public DeleteExamPaperUseCase(
            ExamPaperRepository examPaperRepository,
            ExamPaperSectionRepository examPaperSectionRepository,
            ExamPaperItemRepository examPaperItemRepository,
            ExamRepository examRepository,
            ExamPaperAuthoringAccessService examPaperAuthoringAccessService,
            RecalculateExamTimeDurationService recalculateExamTimeDurationService,
            UserContextPort userContextPort) {
        this.examPaperRepository = examPaperRepository;
        this.examPaperSectionRepository = examPaperSectionRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.examRepository = examRepository;
        this.examPaperAuthoringAccessService = examPaperAuthoringAccessService;
        this.recalculateExamTimeDurationService = recalculateExamTimeDurationService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteExamPaperCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var paper = examPaperRepository.findById(input.paperId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));
        var exam = examRepository.findById(paper.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        var actor = examPaperAuthoringAccessService.requireCanAuthor(exam, currentUserId);
        // Người ra đề chỉ xoá được đề của chính mình. Chủ tịch hội đồng và quản trị trường xoá được mọi
        // mã đề: đổi khung đề bắt buộc phải xoá sạch mã đề hiện có, mà người ra đề cũ có thể đã rời hội
        // đồng — không có nhánh này thì kỳ thi kẹt vĩnh viễn với khung đề cũ.
        if (!actor.canDecide() && !currentUserId.equals(paper.getCreatedBy())) {
            throw new ForbiddenException("Chỉ người đã tạo đề thi này mới được xoá");
        }
        if (paper.getStatus() != ExamPaperStatus.DRAFT) {
            throw new IllegalStateException("Chỉ xoá được đề thi khi còn ở trạng thái DRAFT");
        }

        // Phần thi và câu trong đề treo trên paper_id mà không có FK nào dọn hộ: xoá mỗi dòng đề là
        // để lại section/item mồ côi. Con trước cha.
        var paperIds = List.of(paper.getId());
        examPaperItemRepository.deleteByPaperIdIn(paperIds);
        examPaperSectionRepository.deleteByPaperIdIn(paperIds);
        examPaperRepository.deleteById(paper.getId());
        recalculateExamTimeDurationService.recalculate(paper.getExamId());
        return null;
    }
}
