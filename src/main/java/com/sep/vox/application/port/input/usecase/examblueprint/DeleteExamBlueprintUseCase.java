package com.sep.vox.application.port.input.usecase.examblueprint;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteExamBlueprintCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class DeleteExamBlueprintUseCase implements IUseCase<DeleteExamBlueprintCommand, Void> {

    private final ExamBlueprintRepository examBlueprintRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public DeleteExamBlueprintUseCase(
            ExamBlueprintRepository examBlueprintRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.examBlueprintRepository = examBlueprintRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteExamBlueprintCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        var blueprint = examBlueprintRepository.findById(input.blueprintId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));

        if (!blueprint.getSchoolId().equals(currentSchoolId)
                || !examMemberRepository.existsByUserIdAndRoleAndSchoolId(currentUserId, ExamMemberRole.AUTHOR, blueprint.getSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (blueprint.isActive()) {
            throw new IllegalStateException("Chỉ được xóa blueprint khi isActive=false");
        }
        if (examBlueprintRepository.existsUsedByExam(blueprint.getId())) {
            throw new IllegalStateException("Blueprint vẫn đang được dùng bởi bài kiểm tra");
        }

        examBlueprintRepository.deleteById(blueprint.getId());
        return null;
    }
}
