package com.sep.vox.application.port.input.usecase.examblueprint;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteExamBlueprintSlotCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class DeleteExamBlueprintSlotUseCase implements IUseCase<DeleteExamBlueprintSlotCommand, Void> {

    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintRepository examBlueprintRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public DeleteExamBlueprintSlotUseCase(
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintRepository examBlueprintRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintRepository = examBlueprintRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteExamBlueprintSlotCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        var slot = examBlueprintSlotRepository.findById(input.slotId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy slot"));
        var version = examBlueprintVersionRepository.findById(slot.getBlueprintVersionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint"));
        var blueprint = examBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));
        if (!blueprint.getSchoolId().equals(currentSchoolId)
                || !examMemberRepository.existsByUserIdAndRoleAndSchoolId(currentUserId, ExamMemberRole.AUTHOR, blueprint.getSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (version.getStatus() != ExamBlueprintVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được xoá slot khi version đang DRAFT");
        }

        examBlueprintSlotRepository.deleteById(slot.getId());
        return null;
    }
}
