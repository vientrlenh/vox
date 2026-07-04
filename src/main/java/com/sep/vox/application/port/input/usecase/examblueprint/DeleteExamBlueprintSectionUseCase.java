package com.sep.vox.application.port.input.usecase.examblueprint;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteExamBlueprintSectionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class DeleteExamBlueprintSectionUseCase implements IUseCase<DeleteExamBlueprintSectionCommand, Void> {

    private final ExamBlueprintSectionRepository examBlueprintSectionRepository;
    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintRepository examBlueprintRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public DeleteExamBlueprintSectionUseCase(
            ExamBlueprintSectionRepository examBlueprintSectionRepository,
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintRepository examBlueprintRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.examBlueprintSectionRepository = examBlueprintSectionRepository;
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintRepository = examBlueprintRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteExamBlueprintSectionCommand input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        var section = examBlueprintSectionRepository.findById(input.sectionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy section"));
        var version = examBlueprintVersionRepository.findById(section.getBlueprintVersionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint"));
        var blueprint = examBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));
        if (!examBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (version.getStatus() != ExamBlueprintVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được xoá section khi version đang DRAFT");
        }

        for (var slot : examBlueprintSlotRepository.findBySectionId(section.getId())) {
            examBlueprintSlotRepository.deleteById(slot.getId());
        }
        examBlueprintSectionRepository.deleteById(section.getId());
        return null;
    }
}
