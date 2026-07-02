package com.sep.vox.application.port.input.usecase.exampaper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteExamPaperCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;

@Service
public class DeleteExamPaperUseCase implements IUseCase<DeleteExamPaperCommand, Void> {

    private final ExamPaperRepository examPaperRepository;
    private final ExamMemberRepository examMemberRepository;
    private final UserContextPort userContextPort;

    public DeleteExamPaperUseCase(
            ExamPaperRepository examPaperRepository,
            ExamMemberRepository examMemberRepository,
            UserContextPort userContextPort) {
        this.examPaperRepository = examPaperRepository;
        this.examMemberRepository = examMemberRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteExamPaperCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var paper = examPaperRepository.findById(input.paperId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));

        if (!examMemberRepository.existsByExamIdAndUserIdAndRole(paper.getExamId(), currentUserId, ExamMemberRole.AUTHOR)
                || !currentUserId.equals(paper.getCreatedBy())) {
            throw new ForbiddenException("Chỉ author đã tạo đề thi này mới được xoá");
        }
        if (paper.getStatus() != ExamPaperStatus.DRAFT) {
            throw new IllegalStateException("Chỉ xoá được đề thi khi còn ở trạng thái DRAFT");
        }

        examPaperRepository.deleteById(paper.getId());
        return null;
    }
}
