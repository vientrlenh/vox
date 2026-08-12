package com.sep.vox.application.port.input.usecase.exam;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamMemberCommand;
import com.sep.vox.application.port.input.service.ExamMemberManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.ExamMemberDto;
import com.sep.vox.domain.mapper.ExamMemberDtoMapper;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;

@Service
public class UpdateExamMemberUseCase implements IUseCase<UpdateExamMemberCommand, ExamMemberDto> {

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamMemberManageAccessService examMemberManageAccessService;

    public UpdateExamMemberUseCase(
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            ExamMemberManageAccessService examMemberManageAccessService) {
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.examMemberManageAccessService = examMemberManageAccessService;
    }

    @Override
    @Transactional
    public ExamMemberDto execute(UpdateExamMemberCommand input) {
        var command = new UpdateExamMemberCommand(
            input.examId(),
            input.memberId(),
            StringNormalization.normalizeCode(input.role())
        );
        var exam = examRepository.findById(command.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var member = examMemberRepository.findById(command.memberId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thành viên bài kiểm tra"));

        if (!member.getExamId().equals(exam.getId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var actor = examMemberManageAccessService.requireCanManage(exam);
        // Đổi vai trò chạm tới hai vai: vai cũ bị gỡ và vai mới được gán. Kiểm cả hai, nếu không thì
        // chủ tịch hội đồng hạ cấp được chủ tịch khác xuống AUTHOR chỉ vì vai đích không phải CHAIR.
        var newRole = ExamMemberRole.valueOf(command.role());
        examMemberManageAccessService.requireCanTouchRole(actor, member.getRole());
        examMemberManageAccessService.requireCanTouchRole(actor, newRole);

        member.setRole(newRole);
        return ExamMemberDtoMapper.toDto(examMemberRepository.save(member));
    }
}
