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
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamBlueprintVersionDto;
import com.sep.vox.domain.mapper.ExamBlueprintVersionDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprintVersion;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateExamBlueprintVersionStatusUseCase
        implements IUseCase<UpdateExamBlueprintVersionStatusCommand, ExamBlueprintVersionDto> {

    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintRepository examBlueprintRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;
    private final EventPublisherPort eventPublisherPort;

    public UpdateExamBlueprintVersionStatusUseCase(
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintRepository examBlueprintRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort,
            EventPublisherPort eventPublisherPort) {
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintRepository = examBlueprintRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
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
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        var version = examBlueprintVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint"));
        var blueprint = examBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));

        if (!blueprint.getSchoolId().equals(currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        requireChairOrAdminOverride(version, blueprint.getSchoolId(), currentUserId);

        switch (command.action()) {
            case "PUBLISH" -> {
                if (version.getStatus() != ExamBlueprintVersionStatus.DRAFT) {
                    throw new IllegalStateException("Chỉ được publish version ở trạng thái DRAFT");
                }
                version.setStatus(ExamBlueprintVersionStatus.PUBLISHED);
            }
            case "ARCHIVE" -> {
                if (version.getStatus() != ExamBlueprintVersionStatus.PUBLISHED) {
                    throw new IllegalStateException("Chỉ được archive version ở trạng thái PUBLISHED");
                }
                version.setStatus(ExamBlueprintVersionStatus.ARCHIVED);
            }
            default -> throw new IllegalStateException("Action không hợp lệ");
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

    private void requireChairOrAdminOverride(ExamBlueprintVersion version, UUID schoolId, UUID currentUserId) {
        if (examMemberRepository.existsByUserIdAndRoleAndSchoolId(currentUserId, ExamMemberRole.CHAIR, schoolId)) {
            requireNotAuthor(version, currentUserId);
            return;
        }
        if (isSchoolAdminOverrideAllowed(version, schoolId, currentUserId)) {
            return;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }

    private void requireNotAuthor(ExamBlueprintVersion version, UUID currentUserId) {
        if (currentUserId.equals(version.getCreatedBy())) {
            throw new ForbiddenException("Người tạo version không được tự duyệt version của chính mình");
        }
    }

    private boolean isSchoolAdminOverrideAllowed(ExamBlueprintVersion version, UUID schoolId, UUID currentUserId) {
        if (currentUserId.equals(version.getCreatedBy())) {
            return false;
        }
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(roleInfo -> "SCHOOL_ADMIN".equals(roleInfo.roleCode()));
        if (!schoolAdmin) {
            return false;
        }
        return !examMemberRepository.existsByRoleAndSchoolIdExcludingUserId(ExamMemberRole.CHAIR, schoolId, version.getCreatedBy());
    }
}
