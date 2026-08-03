package com.sep.vox.application.port.input.usecase.exampaper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteExamPaperCommand;
import com.sep.vox.application.port.input.service.RecalculateExamTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;

@Service
public class DeleteExamPaperUseCase implements IUseCase<DeleteExamPaperCommand, Void> {

    private final ExamPaperRepository examPaperRepository;
    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final RecalculateExamTimeDurationService recalculateExamTimeDurationService;
    private final UserContextPort userContextPort;

    public DeleteExamPaperUseCase(
            ExamPaperRepository examPaperRepository,
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            RecalculateExamTimeDurationService recalculateExamTimeDurationService,
            UserContextPort userContextPort) {
        this.examPaperRepository = examPaperRepository;
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
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

        // Bài trên lớp không có vai trò AUTHOR: giáo viên tạo bài là CHAIR và tự soạn mọi mã đề.
        var requiredRole = exam.getKind() == ExamKind.CLASS_TEST ? ExamMemberRole.CHAIR : ExamMemberRole.AUTHOR;
        if (!examMemberRepository.existsByExamIdAndUserIdAndRole(paper.getExamId(), currentUserId, requiredRole)
                || !currentUserId.equals(paper.getCreatedBy())) {
            throw new ForbiddenException("Chỉ người đã tạo đề thi này mới được xoá");
        }
        if (paper.getStatus() != ExamPaperStatus.DRAFT) {
            throw new IllegalStateException("Chỉ xoá được đề thi khi còn ở trạng thái DRAFT");
        }

        examPaperRepository.deleteById(paper.getId());
        recalculateExamTimeDurationService.recalculate(paper.getExamId());
        return null;
    }
}
