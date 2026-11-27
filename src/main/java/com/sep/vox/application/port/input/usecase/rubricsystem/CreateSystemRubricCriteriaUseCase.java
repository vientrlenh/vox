package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.domain.service.rubric.RubricOrderValidator;
import com.sep.vox.domain.service.rubric.ScoreRangeValidator;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricsystem.CreateSystemRubricCriteriaCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExample;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;
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
public class CreateSystemRubricCriteriaUseCase implements IUseCase<CreateSystemRubricCriteriaCommand, List<UUID>> {

    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final FrameworkCriterionRepository frameworkCriterionRepository; // Added: this repository

    @Override
    @Transactional
    public List<UUID> execute(CreateSystemRubricCriteriaCommand command) {
        requireActiveCurrentUser();
        RubricVersion version = loadDraftVersionOfSystemRubric(command.versionId());
        requireExistingFrameworkCriteria(command);

        Set<String> uniqueCodes = new HashSet<>();
        Set<UUID> uniqueFrameworkIds = new HashSet<>();
        Set<Integer> ordersSoFar = collectStoredOrders(command.versionId());

        List<RubricCriterion> criteriaToSave = command.criteria().stream()
                .map(cCmd -> toCriterion(version, cCmd, uniqueCodes, uniqueFrameworkIds, ordersSoFar))
                .collect(Collectors.toList());

        saveCriteria(criteriaToSave);

        return criteriaToSave.stream().map(rc -> rc.getId()).collect(Collectors.toList());
    }

    private void requireActiveCurrentUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Account not found."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Your account has been locked.");
        }
    }

    private RubricVersion loadDraftVersionOfSystemRubric(UUID versionId) {
        RubricVersion version = rubricVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Rubric version not found."));

        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Criteria can only be added while the Rubric is in DRAFT status.");
        }

        // Security: the rubric must really be a SYSTEM rubric.
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent Rubric not found."));

        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("This Rubric belongs to a school. A System Admin is not allowed to edit it.");
        }
        return version;
    }

    // Avoids N+1: collect the ids and validate the frameworks with a single query.
    private void requireExistingFrameworkCriteria(CreateSystemRubricCriteriaCommand command) {
        Set<UUID> inputFrameworkIds = command.criteria().stream()
                .map(c -> c.frameworkCriterionId())
                .collect(Collectors.toSet());

        List<FrameworkCriterion> existingFwCriteria = frameworkCriterionRepository.findAllByIds(inputFrameworkIds.stream().toList());

        // Fewer rows found than distinct ids sent means at least one id does not exist.
        if (existingFwCriteria.size() != inputFrameworkIds.size()) {
            throw new ResourceNotFoundException("Invalid data: one or more submitted Framework Criterion IDs do not exist in the system.");
        }
    }

    // Orders already stored in the version plus the new ones of this batch, to reject duplicate orders.
    private Set<Integer> collectStoredOrders(UUID versionId) {
        Set<Integer> ordersSoFar = new HashSet<>();
        rubricCriterionRepository.findByRubricVersionId(versionId)
                .forEach(c -> ordersSoFar.add(c.getOrder()));
        return ordersSoFar;
    }

    private void saveCriteria(List<RubricCriterion> criteriaToSave) {
        try {
            rubricCriterionRepository.saveAll(criteriaToSave);
        } catch (DataConflictException e) {
            // The validation above rules out in-batch duplicates, so a conflict here is a unique constraint violation (a code/framework that duplicates OLD data saved by an earlier request).
            throw new IllegalArgumentException("Data save error: the criterion code, Framework or order already exists in this system Rubric version.");
        }
    }

    private RubricCriterion toCriterion(RubricVersion version,
                                        CreateSystemRubricCriteriaCommand.CriterionItemCommand cCmd,
                                        Set<String> uniqueCodes, Set<UUID> uniqueFrameworkIds,
                                        Set<Integer> ordersSoFar) {
        String safeCode = StringNormalization.trimAndCollapseSpaces(cCmd.code());
        String safeName = StringNormalization.trimAndCollapseSpaces(cCmd.name());

        requireUniqueInBatch(cCmd, safeCode, safeName, uniqueCodes, uniqueFrameworkIds);
        requireValidScoresAndWeight(cCmd, safeCode);
        RubricOrderValidator.assertNoDuplicateOrder(ordersSoFar, cCmd.order(), safeName);
        ScoreRangeValidator.assertWithinScale(version.getScoringScaleMin(), version.getScoringScaleMax(),
                cCmd.minScore(), cCmd.maxScore(), safeName);

        return RubricCriterion.create(
                version.getId(), cCmd.frameworkCriterionId(), safeCode, safeName,
                cCmd.description() != null ? StringNormalization.trimAndCollapseSpaces(cCmd.description()) : null,
                toExamples(cCmd), cCmd.weight(), cCmd.minScore(), cCmd.maxScore(), cCmd.order(), cCmd.isRequired());
    }

    private void requireUniqueInBatch(CreateSystemRubricCriteriaCommand.CriterionItemCommand cCmd,
                                      String safeCode, String safeName,
                                      Set<String> uniqueCodes, Set<UUID> uniqueFrameworkIds) {
        if (!uniqueCodes.add(safeCode)) {
            throw new IllegalArgumentException("The submitted data repeats the criterion code (Code): " + safeCode);
        }

        if (!uniqueFrameworkIds.add(cCmd.frameworkCriterionId())) {
            throw new IllegalArgumentException("The submitted data repeats the Framework Criterion for the criterion: " + safeName);
        }
    }

    private void requireValidScoresAndWeight(CreateSystemRubricCriteriaCommand.CriterionItemCommand cCmd, String safeCode) {
        if (cCmd.minScore().compareTo(cCmd.maxScore()) > 0) {
            throw new IllegalArgumentException("Criterion '" + safeCode + "': the floor score must not be greater than the ceiling score.");
        }
        // Guard added 2026-08-14: it used to check only >= 0, so the REST path took a percentage (20)
        // directly while the file import path divides by 100 before saving (see
        // RubricCriterionImportCommitHandler). The same "20%" produced two values 100 times apart, and
        // it only blew up at publish time when the weights did not add up.
        if (cCmd.weight() != null
                && (cCmd.weight().compareTo(BigDecimal.ZERO) < 0
                        || cCmd.weight().compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("Criterion '" + safeCode
                    + "': the weight must be between 0 and 1 -- stored as a fraction,"
                    + " (0.2 = 20%, 1 = 100%).");
        }
    }

    private RubricCriterionExamples toExamples(CreateSystemRubricCriteriaCommand.CriterionItemCommand cCmd) {
        if (cCmd.examples() == null || cCmd.examples().isEmpty()) {
            return null;
        }
        List<RubricCriterionExample> exampleList = cCmd.examples().stream()
                .map(exCmd -> new RubricCriterionExample(exCmd.transcript(), exCmd.explanation(), exCmd.expectedScore()))
                .toList();
        return new RubricCriterionExamples(exampleList);
    }
}
