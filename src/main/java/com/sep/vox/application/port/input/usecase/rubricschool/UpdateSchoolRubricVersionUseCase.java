package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.service.rubric.ScoreRangeValidator;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateSchoolRubricVersionUseCase
        implements IUseCase<UpdateSchoolRubricVersionCommand, UUID> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricCriterionRepository rubricCriterionRepository;

    @Override
    @Transactional
    public UUID execute(UpdateSchoolRubricVersionCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        requireActiveSchoolMember(currentUserId, command.schoolId());
        RubricVersion version = loadDraftVersionOwnedBySchool(command);

        String safeName = normalizeRequiredText(
                command.name(),
                "The Rubric version name must not be empty."
        );
        boolean descriptionProvided = isDescriptionProvided(command);
        String safeDescription = normalizeDescription(command);

        requireValidEffectivePeriod(command, version);
        requireScaleCoversExistingChildren(command, version);
        String safeMethod = command.totalScoreMethod() == null ? null : command.totalScoreMethod().name();

        updateVersionAtomically(command, safeName, safeDescription, descriptionProvided, safeMethod);
        return command.versionId();
    }

    // Empty (null or blank) is treated as unchanged -- keep the old description (SQL COALESCE),
    // consistent with UpdateSchoolRubricCriterionUseCase/UpdateSchoolRubricResultBandUseCase. It used
    // to use normalizeRequiredText(), which required a description, contradicting the "partial update"
    // comment on the other fields of the version.
    // A blank description keeps the stored one; an explicit null clears it.
    private boolean isDescriptionProvided(UpdateSchoolRubricVersionCommand command) {
        return command.descriptionProvided() && !isDescriptionBlank(command);
    }

    private String normalizeDescription(UpdateSchoolRubricVersionCommand command) {
        return (command.description() != null && !isDescriptionBlank(command))
                ? StringNormalization.trimAndCollapseSpaces(command.description())
                : null;
    }

    private boolean isDescriptionBlank(UpdateSchoolRubricVersionCommand command) {
        return command.description() != null && command.description().isBlank();
    }

    private void requireActiveSchoolMember(UUID currentUserId, UUID schoolId) {
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("The account does not exist or has been locked.");
        }

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() ->
                        new ForbiddenException("The account is not linked to a school.")
                );
        if (!schoolUser.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException(
                    "You are not allowed to update data of another school."
            );
        }
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("School not found.")
                );
        if (!school.isActive()) {
            throw new ForbiddenException("This school is disabled.");
        }
    }

    private RubricVersion loadDraftVersionOwnedBySchool(UpdateSchoolRubricVersionCommand command) {
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Rubric Version not found.")
                );
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException(
                    "Only a Rubric Version in DRAFT status can be updated."
            );
        }

        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Parent Rubric not found.")
                );
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL
                || !rubric.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException(
                    "This Rubric Version does not belong to your school."
            );
        }
        return version;
    }

    // Partial update: any field that is not passed (null) keeps the current value of the version
    // (updateRubricVersionAtomic uses COALESCE(:param, v.field) at the SQL layer), except effectiveTo,
    // which is cleared by an explicit null.
    private void requireValidEffectivePeriod(UpdateSchoolRubricVersionCommand command, RubricVersion version) {
        Instant finalEffectiveFrom = command.effectiveFrom() != null
                ? command.effectiveFrom() : version.getEffectiveFrom();
        Instant finalEffectiveTo = command.effectiveToProvided()
                ? command.effectiveTo() : version.getEffectiveTo();
        if (finalEffectiveTo != null
                && finalEffectiveFrom.isAfter(finalEffectiveTo)) {
            throw new IllegalArgumentException(
                    "The effective start date must not be later than the effective end date."
            );
        }
    }

    // Block narrowing the scoring scale so that ResultBands/Criteria created earlier would fall outside the new scale.
    private void requireScaleCoversExistingChildren(UpdateSchoolRubricVersionCommand command, RubricVersion version) {
        BigDecimal finalScoreMin = command.scoringScaleMin() != null
                ? command.scoringScaleMin() : version.getScoringScaleMin();
        BigDecimal finalScoreMax = command.scoringScaleMax() != null
                ? command.scoringScaleMax() : version.getScoringScaleMax();
        if (finalScoreMin.compareTo(finalScoreMax) > 0) {
            throw new IllegalArgumentException(
                    "The minimum score must not be greater than the maximum score."
            );
        }

        rubricResultBandRepository.findByRubricVersionId(command.versionId()).forEach(band ->
                ScoreRangeValidator.assertWithinScale(finalScoreMin, finalScoreMax,
                        band.getScoreMin(), band.getScoreMax(), band.getName()));
        rubricCriterionRepository.findByRubricVersionId(command.versionId()).forEach(criterion ->
                ScoreRangeValidator.assertWithinScale(finalScoreMin, finalScoreMax,
                        criterion.getMinScore(), criterion.getMaxScore(), criterion.getName()));
    }

    // For the fields that support partial updates (effectiveFrom/scoringScaleMin/scoringScaleMax), the original
    // value (possibly null) is passed straight from the command so the SQL COALESCE keeps the value currently in
    // the DB at the time of the update -- this avoids overwriting it with the `version` snapshot read earlier (a
    // lost update when another request updates these fields concurrently). The merged final values are only used
    // above to validate business rules.
    private void updateVersionAtomically(UpdateSchoolRubricVersionCommand command, String safeName,
                                         String safeDescription, boolean descriptionProvided, String safeMethod) {
        try {
            rubricVersionRepository.updateRubricVersionAtomic(
                    command.versionId(),
                    null,
                    safeName,
                    safeDescription,
                    descriptionProvided,
                    command.effectiveFrom(),
                    command.effectiveTo(),
                    command.effectiveToProvided(),
                    command.scoringScaleMin(),
                    command.scoringScaleMax(),
                    safeMethod
            );
        } catch (DataConflictException e) {
            throw new IllegalArgumentException(
                    "Cannot update the Rubric Version because the data is duplicated or invalid."
            );
        }
    }

    private String normalizeRequiredText(String value, String errorMessage) {
        if (value == null) {
            throw new IllegalArgumentException(errorMessage);
        }

        String normalizedValue = StringNormalization.trimAndCollapseSpaces(value);

        if (normalizedValue.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }

        return normalizedValue;
    }
}
