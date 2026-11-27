package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.service.rubric.RubricOrderValidator;
import com.sep.vox.domain.service.rubric.RubricResultBandValidator;
import com.sep.vox.domain.service.rubric.ScoreRangeValidator;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricResultBandCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UpdateSchoolRubricResultBandUseCase implements IUseCase<UpdateSchoolRubricResultBandCommand, UUID> {

    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public UUID execute(UpdateSchoolRubricResultBandCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        requireActiveSchoolMember(currentUserId, command.schoolId());

        RubricResultBand resultBand = rubricResultBandRepository.findById(command.resultBandId())
                .orElseThrow(() -> new ResourceNotFoundException("This Result Band was not found."));
        RubricVersion version = loadDraftVersionOwnedBySchool(resultBand, command.schoolId());

        // The code must not change after creation, so it is never updated.
        String safeName = (command.name() != null && !command.name().isBlank()) ? StringNormalization.trimAndCollapseSpaces(command.name()) : null;
        // A blank description keeps the stored one; an explicit null clears it.
        boolean descriptionBlank = command.description() != null && command.description().isBlank();
        boolean descriptionProvided = command.descriptionProvided() && !descriptionBlank;
        String safeDesc = (command.description() != null && !descriptionBlank) ? StringNormalization.trimAndCollapseSpaces(command.description()) : null;
        if (command.order() != null && command.order() <= 0) {
            throw new IllegalArgumentException("The order must be greater than 0.");
        }
        requireValidRangeAndOrder(command, resultBand, version, safeName);

        updateResultBandAtomically(command, safeName, safeDesc, descriptionProvided);
        return command.resultBandId();
    }

    private void requireActiveSchoolMember(UUID currentUserId, UUID schoolId) {
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("The account does not exist or is locked.");
        }
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("The account is not linked to a school."));
        if (!schoolUser.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("SECURITY: you are not allowed to edit data of another school.");
        }
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found."));
        if (!school.isActive()) {
            throw new ForbiddenException("This school is disabled.");
        }
    }

    // The Result Band must belong to a Rubric of the caller's school.
    private RubricVersion loadDraftVersionOwnedBySchool(RubricResultBand resultBand, UUID schoolId) {
        RubricVersion version = rubricVersionRepository.findById(resultBand.getRubricVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("The Rubric Version containing this Result Band was not found."));
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Result Bands can only be edited while the Rubric version is in DRAFT status.");
        }

        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent Rubric not found."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("Action denied: this Result Band does not belong to your school.");
        }
        return version;
    }

    private void requireValidRangeAndOrder(UpdateSchoolRubricResultBandCommand command, RubricResultBand resultBand,
                                           RubricVersion version, String safeName) {
        BigDecimal finalMinScore = command.scoreMin() != null ? command.scoreMin() : resultBand.getScoreMin();
        BigDecimal finalMaxScore = command.scoreMax() != null ? command.scoreMax() : resultBand.getScoreMax();
        if (finalMinScore.compareTo(finalMaxScore) > 0) {
            throw new IllegalArgumentException("The floor score (scoreMin) must not be greater than the ceiling score (scoreMax).");
        }

        List<RubricResultBand> siblingBands = rubricResultBandRepository.findByRubricVersionId(resultBand.getRubricVersionId())
                .stream()
                .filter(b -> !b.getId().equals(resultBand.getId()))
                .toList();
        String nameForError = safeName != null ? safeName : resultBand.getName();
        RubricResultBandValidator.assertNoOverlap(siblingBands, finalMinScore, finalMaxScore, nameForError);

        if (command.order() != null) {
            Set<Integer> siblingOrders = siblingBands.stream()
                    .map(b -> b.getOrder())
                    .collect(Collectors.toCollection(HashSet::new));
            RubricOrderValidator.assertNoDuplicateOrder(siblingOrders, command.order(), nameForError);
        }
        ScoreRangeValidator.assertWithinScale(version.getScoringScaleMin(), version.getScoringScaleMax(),
                finalMinScore, finalMaxScore, nameForError);
    }

    private void updateResultBandAtomically(UpdateSchoolRubricResultBandCommand command, String safeName,
                                            String safeDesc, boolean descriptionProvided) {
        try {
            rubricResultBandRepository.updateResultBandAtomic(
                    command.resultBandId(),
                    null,
                    safeName,
                    safeDesc,
                    descriptionProvided,
                    command.scoreMin(),
                    command.scoreMax(),
                    command.order()
            );
        } catch (DataConflictException e) {
            throw new IllegalArgumentException("Cannot update the Result Band because the data is duplicated or invalid.");
        }
    }
}