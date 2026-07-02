package com.sep.vox.application.port.input.usecase.examblueprint;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.ExamBlueprintVersionPublishedEvent;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamBlueprintVersionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamBlueprintVersionDto;
import com.sep.vox.domain.mapper.ExamBlueprintVersionDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprint;
import com.sep.vox.domain.model.exam.ExamBlueprintVersion;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateExamBlueprintVersionStatusUseCase
        implements IUseCase<UpdateExamBlueprintVersionStatusCommand, ExamBlueprintVersionDto> {

    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintRepository examBlueprintRepository;
    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;
    private final EventPublisherPort eventPublisherPort;

    public UpdateExamBlueprintVersionStatusUseCase(
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintRepository examBlueprintRepository,
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort,
            EventPublisherPort eventPublisherPort) {
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintRepository = examBlueprintRepository;
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    @Transactional
    public ExamBlueprintVersionDto execute(UpdateExamBlueprintVersionStatusCommand input) {
        var command = new UpdateExamBlueprintVersionStatusCommand(
            input.versionId(),
            StringNormalization.normalizeCode(input.action()),
            StringNormalization.trimAndCollapseSpaces(input.note())
        );
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyen truy cap bi tu choi"));

        var version = examBlueprintVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay version blueprint"));
        var blueprint = examBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay blueprint de thi"));

        if (!blueprint.getSchoolId().equals(currentSchoolId)) {
            throw new ForbiddenException("Quyen truy cap bi tu choi");
        }
        requireStatusActor(version, blueprint, currentUserId);

        switch (command.action()) {
            case "PUBLISH" -> {
                if (version.getStatus() != ExamBlueprintVersionStatus.DRAFT) {
                    throw new IllegalStateException("Chi duoc publish version o trang thai DRAFT");
                }
                version.setStatus(ExamBlueprintVersionStatus.PUBLISHED);
            }
            case "ARCHIVE" -> {
                if (version.getStatus() != ExamBlueprintVersionStatus.PUBLISHED) {
                    throw new IllegalStateException("Chi duoc archive version o trang thai PUBLISHED");
                }
                version.setStatus(ExamBlueprintVersionStatus.ARCHIVED);
            }
            default -> throw new IllegalStateException("Action khong hop le");
        }

        version.setUpdatedAt(OffsetDateTime.now());
        version.setUpdatedBy(currentUserId);
        var saved = examBlueprintVersionRepository.save(version);

        if (command.action().equals("PUBLISH")) {
            eventPublisherPort.publish(new ExamBlueprintVersionPublishedEvent(
                blueprint.getSchoolId(),
                blueprint.getCode(),
                blueprint.getName()
            ));
        }

        return ExamBlueprintVersionDtoMapper.toDto(saved);
    }

    private void requireStatusActor(ExamBlueprintVersion version, ExamBlueprint blueprint, UUID currentUserId) {
        var exam = examRepository.findByBlueprintId(blueprint.getId())
            .orElseThrow(() -> new ForbiddenException("Blueprint chua duoc gan vao ky thi, khong the doi trang thai version"));
        var isChair = examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.CHAIR);
        var isReviewer = examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.REVIEWER);
        if (!isChair && !isReviewer) {
            throw new ForbiddenException("Chi CHAIR hoac REVIEWER cua ky thi moi duoc doi trang thai version blueprint");
        }
        if (currentUserId.equals(version.getCreatedBy())) {
            throw new ForbiddenException("Nguoi tao version khong duoc tu doi trang thai version cua chinh minh");
        }
    }
}
