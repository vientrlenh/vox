package com.sep.vox.application.port.input.usecase.exampaper;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.ExamEditingGuard;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteExamPaperCommand;
import com.sep.vox.application.port.input.service.ExamPaperAuthoringAccessService;
import com.sep.vox.application.port.input.service.RecalculateExamTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Service
public class DeleteExamPaperUseCase implements IUseCase<DeleteExamPaperCommand, Void> {

    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperSectionRepository examPaperSectionRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamPaperAuthoringAccessService examPaperAuthoringAccessService;
    private final RecalculateExamTimeDurationService recalculateExamTimeDurationService;
    private final UserContextPort userContextPort;

    public DeleteExamPaperUseCase(
            ExamPaperRepository examPaperRepository,
            ExamPaperSectionRepository examPaperSectionRepository,
            ExamPaperItemRepository examPaperItemRepository,
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamSessionRepository examSessionRepository,
            ExamPaperAuthoringAccessService examPaperAuthoringAccessService,
            RecalculateExamTimeDurationService recalculateExamTimeDurationService,
            UserContextPort userContextPort) {
        this.examPaperRepository = examPaperRepository;
        this.examPaperSectionRepository = examPaperSectionRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examSessionRepository = examSessionRepository;
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
        // Kỳ thi đã bắt đầu thì mã đề là dữ liệu đang chạy — cùng ranh giới mà AssignExamPapersUseCase
        // dựng cho khâu phân đề. Trước đây use case này chỉ soi trạng thái mã đề, nên REOPEN một mã
        // đề đã khoá rồi xoá nó giữa lúc thi vẫn lọt.
        ExamEditingGuard.requireExamEditable(exam);
        // Người ra đề chỉ xoá được đề của chính mình. Chủ tịch hội đồng và quản trị trường xoá được mọi
        // mã đề: đổi khung đề bắt buộc phải xoá sạch mã đề hiện có, mà người ra đề cũ có thể đã rời hội
        // đồng — không có nhánh này thì kỳ thi kẹt vĩnh viễn với khung đề cũ.
        if (!actor.canDecide() && !currentUserId.equals(paper.getCreatedBy())) {
            throw new ForbiddenException("Chỉ người đã tạo đề thi này mới được xoá");
        }
        if (paper.getStatus() != ExamPaperStatus.DRAFT) {
            throw new IllegalStateException("Chỉ xoá được đề thi khi còn ở trạng thái DRAFT");
        }

        // Phiên thi trỏ vào mã đề qua paper_id NOT NULL: đã có học sinh làm bài thì không có cách nào
        // dọn con trỏ, chỉ còn cách không cho xoá.
        if (examSessionRepository.existsByPaperId(paper.getId())) {
            throw new IllegalStateException("Không thể xoá mã đề đã có học sinh làm bài");
        }

        // Thí sinh cũng trỏ vào mã đề qua assigned_paper_id, cũng không có FK nào dọn hộ. Trả họ về
        // "chưa phân đề" thay vì để lại con trỏ tới một dòng đã biến mất: UpdateExamStatusUseCase
        // đếm đúng những thí sinh chưa có mã đề để chặn lên lịch, nên con trỏ mồ côi sẽ lọt qua cửa
        // đó rồi mới nổ lúc học sinh vào phòng thi.
        var affectedCandidates = examCandidateRepository.findByAssignedPaperId(paper.getId());
        if (!affectedCandidates.isEmpty()) {
            var now = Instant.now();
            affectedCandidates.forEach(candidate -> candidate.unassignPaper(now, currentUserId));
            examCandidateRepository.saveAll(affectedCandidates);
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
