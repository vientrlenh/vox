package com.sep.vox.application.port.input.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelBandScope;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.GradeLevelBandScopeRepository;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;

/**
 * Guards the target-band ceiling per grade when creating/editing an Assessment Policy.
 *
 * <p><b>Why a detour is needed to find the grade:</b> an Assessment Policy must choose EXACTLY ONE of three scopes
 * (Class | School-year grade | Grade) -- see CreateSchoolAssessmentPolicyUseCase. That means two
 * thirds of policies do NOT carry a gradeLevelId, while the ceiling table is keyed by
 * (gradeLevelId, frameworkVersionId). If we only guard when the policy picks the Grade scope directly then the ceiling
 * can be disabled just by picking a narrower scope -- and picking the Class scope is the more natural
 * action, so the ceiling would slip by accident without anyone trying to bypass it. So here we always trace back
 * Class -> School-year grade -> Grade before comparing the ceiling.
 *
 * <p><b>Two kinds of assessment, two different ceilings:</b> the column {@code default_target_band_id} carries the
 * meaning of the CENTRALIZE ceiling (centralized assessment -- the policy applies to a whole Grade or a whole School year, i.e.
 * {@code schoolClassId} must NOT be chosen); the column {@code hard_max_band_id} carries the meaning of the CLASS_TEST ceiling
 * (a specialized class with its own tests -- the policy is anchored to exactly 1 specific {@code schoolClassId}). The column
 * names stay as in the original design (no schema change), but the ACTUAL semantics follow the kind of
 * assessment, not "default suggestion" versus "hard ceiling". Only the exact Class scope may be loosened up to the
 * CLASS_TEST ceiling; Grade and School year (which merge many classes, cannot single out a specialized class) are both forced to the
 * lower CENTRALIZE ceiling -- see {@link Batch#effectiveCeilingBand}.
 *
 * <p><b>Does not apply to system policies:</b> CreateSystemAssessmentPolicyUseCase creates a policy with
 * all four scope columns null (system-wide, by language + framework only) -- there is no grade to infer,
 * so by nature the per-grade ceiling table cannot cover that flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GradeLevelBandScopeGuardService {

    private final GradeLevelBandScopeRepository bandScopeRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolClassRepository schoolClassRepository;

    /**
     * Throws IllegalArgumentException if the target band exceeds the ceiling of the corresponding grade level.
     *
     * <p>Pass all three scope columns exactly as the policy is being built; only one of the three is non-null.
     */
    public void assertWithinScope(UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId,
            UUID frameworkVersionId, FrameworkResultBand targetBand) {
        newBatch().assertWithinScope(gradeLevelId, schoolGradeId, schoolClassId, frameworkVersionId, targetBand);
    }

    /**
     * The CENTRALIZE ceiling (centralized assessment, not anchored to 1 specific Class) of the grade. Empty when the grade
     * has no ceiling configured for this framework version.
     */
    public Optional<FrameworkResultBand> defaultTargetBand(UUID gradeLevelId, UUID frameworkVersionId) {
        return bandScopeRepository.findByGradeLevelIdAndFrameworkVersionId(gradeLevelId, frameworkVersionId)
                .flatMap(scope -> frameworkResultBandRepository.findById(scope.getDefaultTargetBandId()));
    }

    /**
     * The import variant: same logic but remembers lookup results within one run.
     *
     * <p>Importing Assessment Policies runs hundreds of Excel rows that reuse a few grades and a few
     * frameworks -- calling {@link #assertWithinScope} directly per row would become N+1. Batch reduces this to
     * O(number of distinct values) queries. Do NOT reuse across imports (the cache has no expiry).
     */
    public Batch newBatch() {
        return new Batch();
    }

    public final class Batch {

        private final Map<UUID, Optional<UUID>> gradeLevelIdBySchoolGradeId = new HashMap<>();
        private final Map<UUID, Optional<UUID>> schoolGradeIdBySchoolClassId = new HashMap<>();
        private final Map<ScopeKey, Optional<GradeLevelBandScope>> scopeByKey = new HashMap<>();
        private final Map<UUID, Optional<FrameworkResultBand>> bandById = new HashMap<>();
        private final Map<UUID, Optional<GradeLevel>> gradeLevelById = new HashMap<>();

        private Batch() {}

        public void assertWithinScope(UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId,
                UUID frameworkVersionId, FrameworkResultBand targetBand) {
            if (targetBand == null || frameworkVersionId == null) {
                return;
            }

            UUID resolvedGradeLevelId = resolveGradeLevelId(gradeLevelId, schoolGradeId, schoolClassId);
            if (resolvedGradeLevelId == null) {
                warnGradeLevelNotResolved(schoolClassId, schoolGradeId);
                return;
            }

            Optional<FrameworkResultBand> effectiveCeiling =
                    findEffectiveCeiling(resolvedGradeLevelId, frameworkVersionId, schoolClassId);
            if (effectiveCeiling.isPresent() && targetBand.getOrder() > effectiveCeiling.get().getOrder()) {
                throw new IllegalArgumentException(
                        buildMessage(resolvedGradeLevelId, targetBand, effectiveCeiling.get()));
            }
        }

        private void warnGradeLevelNotResolved(UUID schoolClassId, UUID schoolGradeId) {
            // The grade level cannot be derived -> there is no ceiling to compare against. A broken Class -> School grade -> Grade level
            // chain is abnormal data (a class pointing to a school year that has disappeared), so shout.
            if (schoolClassId != null || schoolGradeId != null) {
                log.warn(
                    "Cannot infer the Grade from the scope (schoolClassId={}, schoolGradeId={}) -- skipping"
                        + " the target-band ceiling check. The class/school-year hierarchy data is inconsistent.",
                    schoolClassId, schoolGradeId
                );
            }
        }

        private Optional<FrameworkResultBand> findEffectiveCeiling(
                UUID resolvedGradeLevelId, UUID frameworkVersionId, UUID schoolClassId) {
            Optional<GradeLevelBandScope> scope = findScope(resolvedGradeLevelId, frameworkVersionId);
            if (scope.isEmpty()) {
                // DELIBERATELY open: a (grade level, framework version) pair with no declared ceiling blocks nothing.
                // Blocking everything would stall every policy-creation flow on day one, because V42 seeds no
                // ceiling row. Switching to block-by-default only needs a change in exactly this branch.
                log.debug(
                    "Grade {} has no band ceiling configured for framework version {} -- no ceiling applied.",
                    resolvedGradeLevelId, frameworkVersionId
                );
                return Optional.empty();
            }
            return ceilingOfScope(scope.get(), resolvedGradeLevelId, frameworkVersionId, schoolClassId);
        }

        private Optional<FrameworkResultBand> ceilingOfScope(GradeLevelBandScope scope,
                UUID resolvedGradeLevelId, UUID frameworkVersionId, UUID schoolClassId) {
            Optional<FrameworkResultBand> centralizeCapBand = findBand(scope.getDefaultTargetBandId());
            Optional<FrameworkResultBand> classTestCapBand = findBand(scope.getHardMaxBandId());
            if (centralizeCapBand.isEmpty() || classTestCapBand.isEmpty()) {
                // The FK in V42 should make this impossible; if it still happens the configuration is broken and
                // the user is not at fault -- do not block them, but leave a trace.
                log.error(
                    "The band ceiling of grade {} (framework version {}) points to a band that does not exist"
                        + " (centralize={}, classTest={}).",
                    resolvedGradeLevelId, frameworkVersionId,
                    scope.getDefaultTargetBandId(), scope.getHardMaxBandId()
                );
                return Optional.empty();
            }
            return Optional.of(effectiveCeilingBand(schoolClassId, centralizeCapBand.get(), classTestCapBand.get()));
        }

        /**
         * Only a policy anchored to exactly 1 specific Class ({@code schoolClassId} non-null) is a
         * CLASS_TEST, loosened up to the ceiling {@code classTestCapBand}. A policy applying to a whole Grade or a whole
         * School year (merging many classes at once, cannot single out a specialized class) is a CENTRALIZE assessment,
         * only allowed the lower ceiling {@code centralizeCapBand}.
         */
        private static FrameworkResultBand effectiveCeilingBand(UUID schoolClassId,
                FrameworkResultBand centralizeCapBand, FrameworkResultBand classTestCapBand) {
            return schoolClassId != null ? classTestCapBand : centralizeCapBand;
        }

        /**
         * Traces back from the narrow scope to the Grade: Class -> School-year grade -> Grade.
         *
         * @return the Grade id, or null when the scope leads to no grade.
         */
        public UUID resolveGradeLevelId(UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId) {
            if (gradeLevelId != null) {
                return gradeLevelId;
            }
            UUID effectiveGradeId = schoolGradeId;
            if (effectiveGradeId == null && schoolClassId != null) {
                effectiveGradeId = schoolGradeIdBySchoolClassId
                        .computeIfAbsent(schoolClassId, id -> schoolClassRepository.findById(id)
                                .map(schoolClass -> schoolClass.getSchoolGradeId()))
                        .orElse(null);
            }
            if (effectiveGradeId == null) {
                return null;
            }
            return gradeLevelIdBySchoolGradeId
                    .computeIfAbsent(effectiveGradeId, id -> schoolGradeRepository.findById(id)
                            .map(schoolGrade -> schoolGrade.getGradeLevelId()))
                    .orElse(null);
        }

        private Optional<GradeLevelBandScope> findScope(UUID gradeLevelId, UUID frameworkVersionId) {
            return scopeByKey.computeIfAbsent(
                    new ScopeKey(gradeLevelId, frameworkVersionId),
                    key -> bandScopeRepository.findByGradeLevelIdAndFrameworkVersionId(
                            key.gradeLevelId(), key.frameworkVersionId()));
        }

        private Optional<FrameworkResultBand> findBand(UUID bandId) {
            return bandById.computeIfAbsent(bandId, frameworkResultBandRepository::findById);
        }

        private String buildMessage(UUID gradeLevelId, FrameworkResultBand targetBand, FrameworkResultBand hardMaxBand) {
            String gradeLevelName = gradeLevelById
                    .computeIfAbsent(gradeLevelId, gradeLevelRepository::findById)
                    .map(level -> level.getName())
                    .orElse("the selected grade");
            return "Target band '" + targetBand.getLabel() + "' exceeds the ceiling of " + gradeLevelName
                    + ". The highest allowed band is '" + hardMaxBand.getLabel() + "'.";
        }
    }

    private record ScopeKey(UUID gradeLevelId, UUID frameworkVersionId) {}
}
