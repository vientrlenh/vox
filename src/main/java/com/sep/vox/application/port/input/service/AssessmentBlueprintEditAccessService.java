package com.sep.vox.application.port.input.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Loads a blueprint, version, section or slot for the current user and rejects the call with
 * {@link ForbiddenException} unless the user may edit the blueprint it belongs to.
 *
 * <p>The caller's school is resolved first, so a user without a school gets {@code ForbiddenException}
 * before any lookup can answer {@code ResourceNotFoundException}.
 */
@Service
@RequiredArgsConstructor
public class AssessmentBlueprintEditAccessService {

    private static final String ACCESS_DENIED = "Quyền truy cập bị từ chối";

    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public record EditableVersion(AssessmentBlueprint blueprint, AssessmentBlueprintVersion version) {
    }

    public record EditableSection(AssessmentBlueprintSection section, AssessmentBlueprintVersion version) {
    }

    public record EditableSlot(AssessmentBlueprintSlot slot, AssessmentBlueprintVersion version) {
    }

    public AssessmentBlueprint requireEditableBlueprint(UUID blueprintId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = requireCurrentSchoolId(currentUserId);
        return loadEditableBlueprint(blueprintId, currentUserId, currentSchoolId);
    }

    public EditableVersion requireEditableVersion(UUID versionId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = requireCurrentSchoolId(currentUserId);
        return loadEditableVersion(versionId, currentUserId, currentSchoolId);
    }

    public EditableSection requireEditableSection(UUID sectionId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = requireCurrentSchoolId(currentUserId);
        AssessmentBlueprintSection section = assessmentBlueprintSectionRepository.findById(sectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy section"));
        EditableVersion editable = loadEditableVersion(section.getBlueprintVersionId(), currentUserId, currentSchoolId);
        return new EditableSection(section, editable.version());
    }

    public EditableSlot requireEditableSlot(UUID slotId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = requireCurrentSchoolId(currentUserId);
        AssessmentBlueprintSlot slot = assessmentBlueprintSlotRepository.findById(slotId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy slot"));
        EditableVersion editable = loadEditableVersion(slot.getBlueprintVersionId(), currentUserId, currentSchoolId);
        return new EditableSlot(slot, editable.version());
    }

    private UUID requireCurrentSchoolId(UUID currentUserId) {
        return schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException(ACCESS_DENIED));
    }

    private EditableVersion loadEditableVersion(UUID versionId, UUID currentUserId, UUID currentSchoolId) {
        AssessmentBlueprintVersion version = assessmentBlueprintVersionRepository.findById(versionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version blueprint"));
        AssessmentBlueprint blueprint = loadEditableBlueprint(version.getBlueprintId(), currentUserId, currentSchoolId);
        return new EditableVersion(blueprint, version);
    }

    private AssessmentBlueprint loadEditableBlueprint(UUID blueprintId, UUID currentUserId, UUID currentSchoolId) {
        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(blueprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint đề thi"));
        if (!assessmentBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException(ACCESS_DENIED);
        }
        return blueprint;
    }
}
