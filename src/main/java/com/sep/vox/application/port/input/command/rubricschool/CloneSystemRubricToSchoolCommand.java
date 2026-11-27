package com.sep.vox.application.port.input.command.rubricschool;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;

/**
 * @param code           the rubric code set by the SCHOOL, not copied from the template. It is the axis that tells apart
 *                       copies of the same template (e.g. ENG-K10 / ENG-K11 / ENG-K12), and
 *                       it is also what the unique constraint
 *                       {@code (owner_type, school_id, language_id, framework_id, code)} checks.
 * @param totalScoreMethod the scoring method the school chooses, which may differ from the template.
 * @param policies       the template policies (tied to the very version being copied) that the school wants to build
 *                       along with the copy. Empty = copy only the criteria set, the school creates policies later itself.
 */
public record CloneSystemRubricToSchoolCommand(
    UUID schoolId,
    UUID sourceRubricVersionId,
    String code,
    String name,
    String description,
    RubricTotalScoreMethod totalScoreMethod,
    List<PolicyToClone> policies
) {

    /**
     * One template policy, with ITS OWN SCOPE.
     *
     * <p>The scope must go with each policy and not with the whole copy: several policies share
     * one Rubric version so that regular and specialized classes are graded with the same criteria but different target
     * bands -- while each scope may have only one policy in effect
     * ({@code existsActiveForScopeAnyRubricVersion}). Sharing one scope for both would get the second
     * one rejected within the same call.
     *
     * <p>When the template already declares a Grade, the copy keeps that grade and all three scope columns here must be left
     * empty; when the template declares no grade, exactly one of the three must be chosen.
     */
    public record PolicyToClone(
        UUID sourcePolicyId,
        UUID gradeLevelId,
        UUID schoolGradeId,
        UUID schoolClassId,
        Instant effectiveFrom,
        Instant effectiveTo
    ) {}
}
