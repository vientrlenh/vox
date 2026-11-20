package com.sep.vox.domain.service.assessment;

import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * The kind of assessment that a grading screen is talking about.
 *
 * <p>A centralized assessment and a class test are two different grading flows -- the school
 * coordinates the first, the teacher who created the test grades the second -- so every screen must fix its own
 * kind. When the client says nothing it is understood as {@link AssessmentKind#CENTRALIZED}: the school's coordination
 * table is the only place that does not pass this parameter yet, and leaving it empty there
 * is exactly the bug of mixing the two kinds into one list.
 *
 * <p>Placed in {@code application/common} instead of being rewritten in each use case: the default is
 * a business rule, and written in two places the two will sooner or later diverge.
 */
public final class GradingScopeKind {

    private GradingScopeKind() {
    }

    /**
     * @param value the name of an {@link AssessmentKind} taken from the client, may be null/empty
     * @return the name of the kind used to filter the read model
     * @throws IllegalArgumentException when the string is not an {@link AssessmentKind} -- returning the
     *         default now would silently hand the user the data of the other kind
     */
    public static String orCentralized(String value) {
        if (value == null || value.isBlank()) {
            return AssessmentKind.CENTRALIZED.name();
        }
        return AssessmentKind.valueOf(value.trim()).name();
    }
}
