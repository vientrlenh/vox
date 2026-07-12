package com.sep.vox.application.port.input.usecase.examblueprint;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamBlueprintSectionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamBlueprintSectionDto;
import com.sep.vox.domain.mapper.ExamBlueprintSectionDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateExamBlueprintSectionUseCase implements IUseCase<UpdateExamBlueprintSectionCommand, ExamBlueprintSectionDto> {

    private final ExamBlueprintSectionRepository examBlueprintSectionRepository;
    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintRepository examBlueprintRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public UpdateExamBlueprintSectionUseCase(
            ExamBlueprintSectionRepository examBlueprintSectionRepository,
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintRepository examBlueprintRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.examBlueprintSectionRepository = examBlueprintSectionRepository;
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintRepository = examBlueprintRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamBlueprintSectionDto execute(UpdateExamBlueprintSectionCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        var section = examBlueprintSectionRepository.findById(command.sectionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy section"));
        var version = examBlueprintVersionRepository.findById(section.getBlueprintVersionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint"));
        var blueprint = examBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));
        if (!examBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (version.getStatus() != ExamBlueprintVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được sửa section khi version đang DRAFT");
        }

        if (command.order() != section.getOrder()) {
            var siblings = examBlueprintSectionRepository.findByBlueprintVersionId(version.getId());
            if (siblings.stream().anyMatch(s -> !s.getId().equals(section.getId()) && s.getOrder() == command.order())) {
                throw new IllegalStateException("Thứ tự section đã tồn tại trong version này");
            }
        }

        section.setOrder(command.order());
        section.setTitle(command.title());
        section.setInstruction(command.instruction());
        section.setSectionTimeLimitSeconds(command.sectionTimeLimitSeconds());
        section.setSectionWeight(command.sectionWeight() == null ? BigDecimal.ONE : command.sectionWeight());
        section.setUpdatedAt(OffsetDateTime.now());
        section.setUpdatedBy(currentUserId);

        return ExamBlueprintSectionDtoMapper.toDto(examBlueprintSectionRepository.save(section));
    }

    private UpdateExamBlueprintSectionCommand normalize(UpdateExamBlueprintSectionCommand input) {
        return new UpdateExamBlueprintSectionCommand(
            input.sectionId(),
            input.order(),
            StringNormalization.trimAndCollapseSpaces(input.title()),
            StringNormalization.trimAndCollapseSpaces(input.instruction()),
            input.sectionTimeLimitSeconds(),
            input.sectionWeight()
        );
    }
}
