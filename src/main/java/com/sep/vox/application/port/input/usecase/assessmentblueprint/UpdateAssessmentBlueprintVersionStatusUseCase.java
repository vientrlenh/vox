package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.question.Question;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.AssessmentBlueprintVersionPublishedEvent;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentblueprint.UpdateAssessmentBlueprintVersionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.dto.AssessmentBlueprintVersionDto;
import com.sep.vox.domain.mapper.AssessmentBlueprintVersionDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class UpdateAssessmentBlueprintVersionStatusUseCase
        implements IUseCase<UpdateAssessmentBlueprintVersionStatusCommand, AssessmentBlueprintVersionDto> {

    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final QuestionRepository questionRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserRoleQueryRepository userRoleQueryRepository;

    @Override
    @Transactional
    public AssessmentBlueprintVersionDto execute(UpdateAssessmentBlueprintVersionStatusCommand input) {
        UpdateAssessmentBlueprintVersionStatusCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Access denied"));

        AssessmentBlueprintVersion version = assessmentBlueprintVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("Blueprint version not found"));
        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment blueprint not found"));

        requireStatusActor(version, blueprint, currentUserId, currentSchoolId);
        Instant now = Instant.now();
        applyStatusAction(version, command.action());
        AssessmentBlueprintVersion saved = assessmentBlueprintVersionRepository.save(version);

        if (command.action().equals("PUBLISH")) {
            saveVersionPublishedEvent(blueprint, saved, now);
        }
        return AssessmentBlueprintVersionDtoMapper.toDto(saved);
    }

    private UpdateAssessmentBlueprintVersionStatusCommand normalize(UpdateAssessmentBlueprintVersionStatusCommand input) {
        return new UpdateAssessmentBlueprintVersionStatusCommand(
            input.versionId(),
            StringNormalization.normalizeCode(input.action()),
            StringNormalization.trimAndCollapseSpaces(input.note())
        );
    }

    private void applyStatusAction(AssessmentBlueprintVersion version, String action) {
        switch (action) {
            case "PUBLISH" -> {
                if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
                    throw new IllegalArgumentException("Only a DRAFT version can be published");
                }
                validatePublishable(version);
                version.setStatus(AssessmentBlueprintVersionStatus.PUBLISHED);
            }
            case "ARCHIVE" -> {
                if (version.getStatus() != AssessmentBlueprintVersionStatus.PUBLISHED) {
                    throw new IllegalArgumentException("Only a PUBLISHED version can be archived");
                }
                version.setStatus(AssessmentBlueprintVersionStatus.ARCHIVED);
            }
            default -> throw new IllegalArgumentException("Invalid action");
        }
    }

    private void saveVersionPublishedEvent(AssessmentBlueprint blueprint, AssessmentBlueprintVersion saved, Instant now) {
        List<UUID> schoolAdminIds = schoolUserRepository.findBySchoolIdWithRole(blueprint.getSchoolId(), "SCHOOL_ADMIN")
            .stream()
            .map(su -> su.getUserId())
            .toList();
        AssessmentBlueprintVersionPublishedEvent event = new AssessmentBlueprintVersionPublishedEvent(
            schoolAdminIds,
            blueprint.getCode(),
            blueprint.getName(),
            blueprint.getId(),
            saved.getId()
        );
        String payload = jsonSerializationPort.toJson(event);
        Outbox outbox = Outbox.create(AggregateTypeConstant.ASSESSMENT_BLUEPRINT_VERSION, saved.getId(), EventTypeConstant.ASSESSMENT_BLUEPRINT_VERSION_PUBLISHED, payload, now);
        outboxRepository.save(outbox);
    }

    private void requireStatusActor(AssessmentBlueprintVersion version, AssessmentBlueprint blueprint, UUID currentUserId, UUID currentSchoolId) {
        if (!assessmentBlueprintRepository.canChangeVersionStatus(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException(
                "Access denied: you must be a SCHOOL_ADMIN or a CHAIR/REVIEWER of the assessment that uses this blueprint");
        }
        if (version.isCreatedBy(currentUserId) && !maySelfApproveVersion(blueprint, currentUserId, currentSchoolId)) {
            throw new ForbiddenException("The creator of a version must not change the status of their own version");
        }
    }

    // Who may SELF-APPROVE a version they created themselves.
    //
    // SCHOOL_ADMIN/SYSTEM_ADMIN: allowed, because they are the highest authority over the blueprint and in practice
    // a school usually has only 1 admin -- blocking absolutely would leave the version stuck in DRAFT forever.
    //
    // CHAIR: allowed, on par with the admin (changed 2026-08-15). Previously maker-checker applied to the
    // teacher branch too, so after a CHAIR created a version they had to find someone else to press publish before it could
    // be attached to their own assessment (AttachAssessmentBlueprintUseCase requires the version to be
    // PUBLISHED). In a small school that "someone else" often does not exist, and the CHAIR
    // is already the person ultimately responsible for the assessment -- making them ask for approval of the paper
    // frame of the very assessment they chair only produces a formal signature.
    //
    // REVIEWER: NO. They exist precisely to be the second person, and letting them self-approve would erase their own
    // role. This is why isChairOfAssessmentUsingBlueprint has to be asked instead of reusing
    // canChangeVersionStatus above -- that one lumps CHAIR together with REVIEWER.
    private boolean maySelfApproveVersion(AssessmentBlueprint blueprint, UUID currentUserId, UUID currentSchoolId) {
        boolean isAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()) || "SYSTEM_ADMIN".equals(role.roleCode()));
        return isAdmin || assessmentBlueprintRepository.isChairOfAssessmentUsingBlueprint(
            blueprint.getId(), currentUserId, currentSchoolId);
    }

    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");

    private void validatePublishable(AssessmentBlueprintVersion version) {
        List<AssessmentBlueprintSection> sections = assessmentBlueprintSectionRepository.findByBlueprintVersionId(version.getId());
        if (sections.isEmpty()) {
            throw new IllegalArgumentException("A blueprint version must have at least one section before it is published");
        }
        BigDecimal weightSum = sections.stream()
            .map(section -> section.getSectionWeight() == null ? BigDecimal.ZERO : section.getSectionWeight())
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        if (weightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalArgumentException("The total section weight must be 1.00 before publishing");
        }

        List<AssessmentBlueprintSlot> allSlots = assessmentBlueprintSlotRepository.findByBlueprintVersionId(version.getId());
        requireFixedQuestionsPublished(allSlots);
        requireSlotWeightsPerSection(sections, allSlots);
    }

    private void requireFixedQuestionsPublished(List<AssessmentBlueprintSlot> allSlots) {
        allSlots.stream()
            .filter(slot -> slot.getSlotType() == AssessmentBlueprintSlotType.FIXED)
            .forEach(slot -> {
                Question question = slot.getFixedQuestionId() == null
                    ? null
                    : questionRepository.findById(slot.getFixedQuestionId()).orElse(null);
                if (question == null || question.getStatus() != QuestionStatus.PUBLISHED) {
                    throw new IllegalArgumentException("The fixed question of the slot must be PUBLISHED");
                }
            });
    }

    private void requireSlotWeightsPerSection(List<AssessmentBlueprintSection> sections, List<AssessmentBlueprintSlot> allSlots) {
        Map<UUID, List<AssessmentBlueprintSlot>> slotsBySectionId = allSlots.stream()
            .collect(Collectors.groupingBy(slot -> slot.getSectionId()));
        for (AssessmentBlueprintSection section : sections) {
            List<AssessmentBlueprintSlot> slots = slotsBySectionId.getOrDefault(section.getId(), List.of());
            BigDecimal slotWeightSum = slots.stream()
                .map(slot -> slot.getWeight() == null ? BigDecimal.ZERO : slot.getWeight())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
            if (slotWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
                throw new IllegalArgumentException(
                    "The total question slot weight in section \"" + section.getTitle() + "\" must be 1.00 before publishing");
            }
        }
    }
}
