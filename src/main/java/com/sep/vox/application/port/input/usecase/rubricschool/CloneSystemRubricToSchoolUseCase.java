package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.CloneSystemRubricToSchoolCommand;
import com.sep.vox.application.port.input.command.assessmentpolicysystem.CreateAssessmentPolicyCommand;
import com.sep.vox.application.port.input.service.RubricCloneService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.CreateSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * A school copies a published system template into a rubric of its own, together with the template scoring
 * policies attached to that version.
 *
 * <p>Why it must be copied instead of using the system's version directly: {@code CreateSchoolAssessmentPolicyUseCase}
 * refuses to attach a school policy to a rubric version that does not belong to that school. So copying is
 * not a convenience optimisation, it is the only way for a template to reach grading.
 *
 * <h2>Why the policies are handled here too</h2>
 *
 * <p>The copied rubric comes out as DRAFT, and a DRAFT version cannot be published until at least one linked
 * policy is PUBLISHED ({@code ChangeSchoolRubricVersionStatusUseCase}). So "copy the rubric" is always
 * followed by "create a policy" -- merging them into one step describes the mandatory order rather than
 * being a convenience bundle. The opposite route (copy the policy, then clone the rubric automatically) was
 * removed because it created a second rubric copy every time a school wanted to add one more policy.
 *
 * <p>Creating the policy is NOT rewritten here but delegated to {@code CreateSchoolAssessmentPolicyUseCase}:
 * every rule (framework still PUBLISHED, target band belongs to the framework and does not exceed the grade level's
 * ceiling, exactly one scope, one policy per scope, version numbering per scope) already lives there. Duplicating
 * them here would create two rule sets that drift apart.
 *
 * <p>Both use cases are {@code @Transactional}, so the inner call joins the same transaction: a
 * policy that fails validation rolls back the rubric copy too, leaving no orphan rubric.
 */
@Service
@RequiredArgsConstructor
public class CloneSystemRubricToSchoolUseCase implements IUseCase<CloneSystemRubricToSchoolCommand, UUID> {

    private final RubricRepository rubricRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricCloneService rubricCloneService;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final CreateSchoolAssessmentPolicyUseCase createSchoolAssessmentPolicyUseCase;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(CloneSystemRubricToSchoolCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        requireActiveMemberOfSchool(currentUserId, command.schoolId());

        RubricVersion sourceVersion = rubricVersionRepository.findById(command.sourceRubricVersionId())
            .orElseThrow(() -> new ResourceNotFoundException("Template Rubric version not found."));
        Rubric sourceRubric = rubricRepository.findById(sourceVersion.getRubricId())
            .orElseThrow(() -> new ResourceNotFoundException("Parent Rubric of the template not found."));
        requireClonableTemplate(sourceRubric, sourceVersion);

        // Same normalisation as CreateSchoolRubricUseCase: both paths write to one unique constraint, so
        // they must agree on what "the same code" means.
        String safeCode = StringNormalization.trimAndCollapseSpaces(command.code());
        String safeName = StringNormalization.trimAndCollapseSpaces(command.name());
        String safeDescription = command.description() == null
            ? null
            : StringNormalization.trimAndCollapseSpaces(command.description());
        requireCodeAndName(safeCode, safeName);
        requireCodeNotTaken(command.schoolId(), sourceRubric, safeCode);

        return cloneWithPolicies(command, sourceRubric, sourceVersion, safeCode, safeName, safeDescription, currentUserId);
    }

    private UUID cloneWithPolicies(CloneSystemRubricToSchoolCommand command, Rubric sourceRubric,
                                   RubricVersion sourceVersion, String safeCode, String safeName,
                                   String safeDescription, UUID currentUserId) {
        RubricTotalScoreMethod targetMethod = command.totalScoreMethod() == null
            ? sourceVersion.getTotalScoreMethod()
            : command.totalScoreMethod();

        RubricVersion clonedVersion = rubricCloneService.cloneToSchoolAsDraft(
            sourceRubric,
            sourceVersion,
            command.schoolId(),
            safeCode,
            safeName,
            safeDescription,
            targetMethod,
            currentUserId);

        createPoliciesFromTemplates(command, sourceVersion.getId(), clonedVersion.getId());

        return clonedVersion.getId();
    }

    private void requireActiveMemberOfSchool(UUID currentUserId, UUID schoolId) {
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new UnauthorizedException("Account error."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("The account is locked.");
        }

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
            .orElseThrow(() -> new ForbiddenException("Your account is not linked to any school."));
        if (!schoolUser.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("SECURITY: you are not allowed to copy into another school.");
        }
    }

    private void requireClonableTemplate(Rubric sourceRubric, RubricVersion sourceVersion) {
        if (sourceRubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("You can only copy from a system template Rubric.");
        }
        // Only published versions are accepted: a system draft is unfinished, unreviewed work and must not
        // leak outside the system admin's scope.
        if (sourceVersion.getStatus() != RubricStatus.PUBLISHED) {
            throw new IllegalArgumentException("You can only copy from a template version that has been published (PUBLISHED).");
        }
        // Cloning an empty template would create a version the school can never publish (the "must have
        // criteria" gate blocks it), and they would only find out at the last step.
        if (rubricCriterionRepository.findByRubricVersionId(sourceVersion.getId()).isEmpty()) {
            throw new IllegalArgumentException("This template has no criterion yet, so it cannot be copied.");
        }
    }

    private void requireCodeAndName(String safeCode, String safeName) {
        if (safeCode == null || safeCode.isBlank()) {
            throw new IllegalArgumentException("The Rubric code must not be empty.");
        }
        if (safeName == null || safeName.isBlank()) {
            throw new IllegalArgumentException("The Rubric name must not be empty.");
        }
    }

    // Checked early to give a readable error instead of a DB unique-constraint message the user cannot act on.
    private void requireCodeNotTaken(UUID schoolId, Rubric sourceRubric, String safeCode) {
        boolean isCodeTaken = rubricRepository.existsByOwnerTypeAndSchoolIdAndLanguageIdAndFrameworkIdAndCode(
            RubricOwnerType.SCHOOL.toString(),
            schoolId,
            sourceRubric.getLanguageId(),
            sourceRubric.getFrameworkId(),
            safeCode);
        if (isCodeTaken) {
            throw new ResourceDuplicatedException("Your school already has a Rubric with code '" + safeCode
                + "' for this language and framework. Use another code (for example named after the grade level:"
                + " ENG-K10, ENG-K11).");
        }
    }

    /**
     * Builds the school's policies from the template policies the school selected, pointing at the rubric copy
     * that was just created.
     *
     * <p>Only checks two things that {@code CreateSchoolAssessmentPolicyUseCase} cannot know by itself: whether the
     * template is valid and belongs to the version being copied, and whether the scope obeys the grade level
     * inheritance rule. Every other rule is left on the other side.
     */
    private void createPoliciesFromTemplates(
            CloneSystemRubricToSchoolCommand command, UUID sourceVersionId, UUID clonedVersionId) {

        List<CloneSystemRubricToSchoolCommand.PolicyToClone> requested = command.policies();
        if (requested == null || requested.isEmpty()) {
            // Copying the "ceiling" rubric: valid, but this version stays DRAFT until the school attaches
            // a policy to it itself.
            return;
        }

        // Load once and look up in memory: the template policy list of a version is always small
        // (currently 1 per version), and this way the "belongs to the version being copied" check
        // needs no extra query.
        List<AssessmentPolicy> templates = assessmentPolicyRepository
                .findPublishedSystemWideByRubricVersionId(sourceVersionId);

        List<CreateAssessmentPolicyCommand> policyCommands = requested.stream()
                .map(choice -> toCreateCommand(command, choice, templates, clonedVersionId))
                .toList();

        createSchoolAssessmentPolicyUseCase.execute(policyCommands);
    }

    private CreateAssessmentPolicyCommand toCreateCommand(
            CloneSystemRubricToSchoolCommand command,
            CloneSystemRubricToSchoolCommand.PolicyToClone choice,
            List<AssessmentPolicy> templates,
            UUID clonedVersionId) {

        AssessmentPolicy template = findPublishedTemplate(templates, choice);

        TargetScope scope = resolveScope(template, choice);

        return new CreateAssessmentPolicyCommand(
                command.schoolId(),
                template.getFrameworkVersionId(),
                clonedVersionId,
                template.getLanguageId(),
                scope.gradeLevelId(),
                scope.schoolGradeId(),
                scope.schoolClassId(),
                template.getTargetFrameworkBandId(),
                template.getPassingScore(),
                template.getStrictness(),
                choice.effectiveFrom(),
                choice.effectiveTo());
    }

    private AssessmentPolicy findPublishedTemplate(
            List<AssessmentPolicy> templates, CloneSystemRubricToSchoolCommand.PolicyToClone choice) {

        if (choice.sourcePolicyId() == null) {
            throw new IllegalArgumentException("The id of the template policy to copy is missing.");
        }

        // Looking only in the list of the exact version being cloned already rejects the id of a template
        // policy that belongs to another version -- the copy would carry parameters written for another rubric.
        AssessmentPolicy template = templates.stream()
                .filter(candidate -> candidate.getId().equals(choice.sourcePolicyId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No published template policy was found for this Rubric version."));
        if (template.getStatus() != AssessmentPolicyStatus.PUBLISHED) {
            throw new IllegalArgumentException("You can only copy from a template policy that has been published (PUBLISHED).");
        }
        return template;
    }

    /**
     * When the template declares a grade level the copy MUST keep exactly that grade level -- letting the school switch to
     * another one would turn a "Grade 10" template into a Grade 12 policy that still carries the parameters written for Grade
     * 10. When the template declares no grade level the school chooses, and the "exactly one scope" check lives in
     * {@code CreateSchoolAssessmentPolicyUseCase} so it is not repeated here.
     */
    private TargetScope resolveScope(
            AssessmentPolicy template, CloneSystemRubricToSchoolCommand.PolicyToClone choice) {

        if (template.getGradeLevelId() != null) {
            if (choice.gradeLevelId() != null || choice.schoolGradeId() != null || choice.schoolClassId() != null) {
                throw new IllegalArgumentException(
                        "This template policy is already tied to a specific grade level, so the copy keeps that grade level;"
                                + " it does not accept another scope.");
            }
            return new TargetScope(template.getGradeLevelId(), null, null);
        }
        return new TargetScope(choice.gradeLevelId(), choice.schoolGradeId(), choice.schoolClassId());
    }

    /** The scope the copy will apply, after merging the template's scope with the school's choice. */
    private record TargetScope(UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId) {}
}
