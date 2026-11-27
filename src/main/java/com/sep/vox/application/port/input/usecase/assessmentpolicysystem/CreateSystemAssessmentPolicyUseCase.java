package com.sep.vox.application.port.input.usecase.assessmentpolicysystem;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.assessmentpolicysystem.CreateAssessmentPolicyCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateSystemAssessmentPolicyUseCase implements IUseCase<List<CreateAssessmentPolicyCommand>, List<UUID>> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final SupportedLanguageRepository languageRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    // Used to issue the next "version" number per scope across the whole batch.
    //
    // Since 2026-08-22 gradeLevelId is also part of the key: a system policy can now be declared per GRADE LEVEL. A grade level
    // is already a catalog shared across the whole system and is not tied to any school year, so a
    // template policy for "Grade 10" stays valid when a school opens next year's school year -- no need to declare it again.
    // null is still allowed = the template applies to every grade level, keeping the old behavior.
    private record VersionScopeKey(UUID languageId, UUID frameworkVersionId, UUID gradeLevelId) {}

    @Override
    @Transactional
    public List<UUID> execute(List<CreateAssessmentPolicyCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            throw new IllegalArgumentException("The list of Assessment Policies to create must not be empty.");
        }

        requireActiveSystemAdmin();

        List<AssessmentPolicy> policiesToSave = new ArrayList<>();
        Map<VersionScopeKey, Integer> nextVersionByScope = new HashMap<>();
        Set<VersionScopeKey> scopeClaimsInBatch = new HashSet<>();

        for (CreateAssessmentPolicyCommand command : commands) {
            policiesToSave.add(buildDraftPolicy(command, nextVersionByScope, scopeClaimsInBatch));
        }

        List<AssessmentPolicy> savedPolicies = assessmentPolicyRepository.saveAll(policiesToSave);
        return savedPolicies.stream().map(ap -> ap.getId()).toList();
    }

    private void requireActiveSystemAdmin() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Account not found."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("The account has been locked.");
    }

    private AssessmentPolicy buildDraftPolicy(
            CreateAssessmentPolicyCommand command,
            Map<VersionScopeKey, Integer> nextVersionByScope,
            Set<VersionScopeKey> scopeClaimsInBatch) {
        FrameworkVersion frameworkVersion = requirePublishedFrameworkAndLanguage(command);
        requireSystemWideScope(command);
        requireTargetBand(command);
        requireValidEffectivePeriod(command);

        AssessmentPolicyStrictness strictness = command.strictness() != null ? command.strictness() : AssessmentPolicyStrictness.STANDARD;

        // Each new Policy in the batch takes its own version number within the same scope (language + framework),
        // because the DB is only unique by scope+version and does not distinguish by Rubric Version
        VersionScopeKey versionScopeKey = new VersionScopeKey(command.languageId(), command.frameworkVersionId(),
                command.gradeLevelId());

        claimScopeOnce(command, versionScopeKey, scopeClaimsInBatch);
        requireUsableRubricVersion(command, frameworkVersion);
        int nextVersion = nextVersionFor(versionScopeKey, nextVersionByScope);

        return AssessmentPolicy.draftSystemWide(command.gradeLevelId(), command.languageId(),
            command.frameworkVersionId(), command.rubricVersionId(), command.targetFrameworkBandId(), command.passingScore(),
            strictness, nextVersion, command.effectiveFrom(), command.effectiveTo());
    }

    private FrameworkVersion requirePublishedFrameworkAndLanguage(CreateAssessmentPolicyCommand command) {
        FrameworkVersion frameworkVersion = frameworkVersionRepository.findById(command.frameworkVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Framework version not found."));
        if (frameworkVersion.getStatus() != FrameworkVersionStatus.PUBLISHED) {
            throw new IllegalArgumentException("A Policy can only be created on a PUBLISHED Framework.");
        }
        if (!languageRepository.existsById(command.languageId())) {
            throw new ResourceNotFoundException("The language does not exist.");
        }
        return frameworkVersion;
    }

    private void requireSystemWideScope(CreateAssessmentPolicyCommand command) {
        // Grade level (optional). The SYSTEM flow has no school so it does NOT accept a school year / class --
        // those belong to one specific school. Only the Grade level is a system-wide concept.
        if (command.schoolGradeId() != null || command.schoolClassId() != null) {
            throw new IllegalArgumentException(
                    "A system policy can only be scoped down to the grade level; school year and class belong to each school's scope.");
        }
        if (command.gradeLevelId() != null
                && gradeLevelRepository.findById(command.gradeLevelId()).isEmpty()) {
            throw new ResourceNotFoundException("Grade level not found.");
        }
    }

    private void requireTargetBand(CreateAssessmentPolicyCommand command) {
        FrameworkResultBand targetBand = frameworkResultBandRepository.findById(command.targetFrameworkBandId())
                .orElseThrow(() -> new ResourceNotFoundException("Target band not found."));
        if (!targetBand.getFrameworkVersionId().equals(command.frameworkVersionId())) {
            throw new IllegalArgumentException("The target band must belong to the selected competency framework.");
        }
    }

    private void requireValidEffectivePeriod(CreateAssessmentPolicyCommand command) {
        if (command.effectiveFrom() == null) throw new IllegalArgumentException("The start date must not be empty.");
        if (command.effectiveTo() != null && command.effectiveFrom().isAfter(command.effectiveTo())) {
            throw new IllegalArgumentException("The end date must not be before the start date.");
        }
    }

    /**
     * Each scope (language + framework version) may have EXACTLY ONE policy in effect.
     *
     * <p>Added 2026-08-14: before that the SYSTEM flow had no duplicate check at all -- neither inside the batch nor
     * against the DB -- so system policies overlapped freely while findActivePolicy could use only one of them.
     * See CreateSchoolAssessmentPolicyUseCase for the full reason.
     */
    private void claimScopeOnce(CreateAssessmentPolicyCommand command, VersionScopeKey versionScopeKey, Set<VersionScopeKey> scopeClaimsInBatch) {
        if (!scopeClaimsInBatch.add(versionScopeKey)) {
            throw new ResourceDuplicatedException(
                    "The same creation request contains two Assessment Policies with the same language and"
                            + " framework. Each scope may have only one policy.");
        }
        if (assessmentPolicyRepository.existsActiveForScopeAnyRubricVersion(
                null, command.languageId(), command.frameworkVersionId(),
                command.gradeLevelId(), null, null)) {
            throw new ResourceDuplicatedException("This scope already has a system Assessment Policy still"
                    + " in force (DRAFT or PUBLISHED). Archive the old one before creating a new one.");
        }
    }

    /**
     * Several policies ARE ALLOWED to share one Rubric Version (V44 dropped the 1-1 constraint of V38): a specialised
     * class and a regular class of the same grade level need separate policies at Class scope but still grade with
     * the same set of criteria. What must stay unique is "each scope has only one policy in effect" -- checked
     * by the scope claim and by existsActiveForScopeAnyRubricVersion in {@link #claimScopeOnce}.
     */
    private void requireUsableRubricVersion(CreateAssessmentPolicyCommand command, FrameworkVersion frameworkVersion) {
        UUID rubricVersionId = command.rubricVersionId();
        RubricVersion rubricVersion = rubricVersionRepository.findById(rubricVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("Rubric version not found, ID: " + rubricVersionId));
        Rubric rubric = rubricRepository.findById(rubricVersion.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent rubric not found for version ID: " + rubricVersionId));

        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new IllegalArgumentException("Only a SYSTEM Rubric may be used in the System Admin flow.");
        }
        // Blocks ARCHIVED, does NOT block PUBLISHED -- see CreateSchoolAssessmentPolicyUseCase for the full reason.
        // Both flows must read this rule the same way, otherwise the system template and the school's copy
        // would have two different lifecycles.
        if (rubricVersion.getStatus() == RubricStatus.ARCHIVED) {
            throw new IllegalArgumentException(
                    "A Policy cannot be attached to an archived (ARCHIVED) rubric version.");
        }
        if (!rubric.getFrameworkId().equals(frameworkVersion.getFrameworkId())) {
            throw new IllegalArgumentException("The rubric version and the competency framework do not match.");
        }
    }

    private int nextVersionFor(VersionScopeKey versionScopeKey, Map<VersionScopeKey, Integer> nextVersionByScope) {
        int nextVersion = nextVersionByScope.computeIfAbsent(versionScopeKey, key ->
                assessmentPolicyRepository.findMaxVersionForScope(
                        null, key.languageId(), key.frameworkVersionId(),
                        key.gradeLevelId(), null, null) + 1);
        nextVersionByScope.put(versionScopeKey, nextVersion + 1);
        return nextVersion;
    }
}
