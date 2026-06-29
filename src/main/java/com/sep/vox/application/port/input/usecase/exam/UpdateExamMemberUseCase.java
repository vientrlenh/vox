package com.sep.vox.application.port.input.usecase.exam;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamMemberCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamMemberDto;
import com.sep.vox.domain.mapper.ExamMemberDtoMapper;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateExamMemberUseCase implements IUseCase<UpdateExamMemberCommand, ExamMemberDto> {

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public UpdateExamMemberUseCase(
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamMemberDto execute(UpdateExamMemberCommand input) {
        var command = new UpdateExamMemberCommand(
            input.examId(),
            input.memberId(),
            StringNormalization.normalizeCode(input.role())
        );
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        var exam = examRepository.findById(command.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var member = examMemberRepository.findById(command.memberId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thành viên bài kiểm tra"));

        if (!member.getExamId().equals(exam.getId())
                || exam.getKind() != ExamKind.CENTRALIZED
                || !schoolAdmin
                || currentSchoolId == null
                || !currentSchoolId.equals(exam.getSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        member.setRole(ExamMemberRole.valueOf(command.role()));
        return ExamMemberDtoMapper.toDto(examMemberRepository.save(member));
    }
}
