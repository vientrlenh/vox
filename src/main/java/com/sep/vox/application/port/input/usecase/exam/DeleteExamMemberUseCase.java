package com.sep.vox.application.port.input.usecase.exam;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteExamMemberCommand;
import com.sep.vox.application.port.input.service.ExamMemberManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;

@Service
public class DeleteExamMemberUseCase implements IUseCase<DeleteExamMemberCommand, Void> {

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamMemberManageAccessService examMemberManageAccessService;

    public DeleteExamMemberUseCase(
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            ExamMemberManageAccessService examMemberManageAccessService) {
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.examMemberManageAccessService = examMemberManageAccessService;
    }

    @Override
    @Transactional
    public Void execute(DeleteExamMemberCommand input) {
        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var member = examMemberRepository.findById(input.memberId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thành viên bài kiểm tra"));

        if (!member.getExamId().equals(exam.getId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var actor = examMemberManageAccessService.requireCanManage(exam);
        examMemberManageAccessService.requireCanTouchRole(actor, member.getRole());

        examMemberRepository.deleteById(member.getId());
        return null;
    }
}
