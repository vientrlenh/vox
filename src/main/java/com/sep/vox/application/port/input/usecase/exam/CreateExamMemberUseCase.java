package com.sep.vox.application.port.input.usecase.exam;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateExamMemberCommand;
import com.sep.vox.application.port.input.service.ExamMemberManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.ExamMemberDto;
import com.sep.vox.domain.mapper.ExamMemberDtoMapper;
import com.sep.vox.domain.model.exam.ExamMember;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class CreateExamMemberUseCase implements IUseCase<CreateExamMemberCommand, ExamMemberDto> {

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final ExamMemberManageAccessService examMemberManageAccessService;

    public CreateExamMemberUseCase(
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            ExamMemberManageAccessService examMemberManageAccessService) {
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.examMemberManageAccessService = examMemberManageAccessService;
    }

    @Override
    @Transactional
    public ExamMemberDto execute(CreateExamMemberCommand input) {
        var command = new CreateExamMemberCommand(
            input.examId(),
            input.userId(),
            StringNormalization.normalizeCode(input.role())
        );
        var exam = examRepository.findById(command.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));

        var actor = examMemberManageAccessService.requireCanManage(exam);
        var role = ExamMemberRole.valueOf(command.role());
        examMemberManageAccessService.requireCanTouchRole(actor, role);

        if (!schoolUserRepository.existsBySchoolIdAndUserId(exam.getSchoolId(), command.userId())) {
            throw new IllegalStateException("Người dùng không thuộc trường của bài kiểm tra");
        }
        if (examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), command.userId(), role)) {
            throw new IllegalStateException("Người dùng đã có vai trò này trong bài kiểm tra");
        }

        var member = new ExamMember(
            exam.getId(),
            command.userId(),
            role,
            Instant.now(),
            actor.userId()
        );
        return ExamMemberDtoMapper.toDto(examMemberRepository.save(member));
    }
}
