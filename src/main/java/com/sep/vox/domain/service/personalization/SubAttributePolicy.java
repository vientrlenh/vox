package com.sep.vox.domain.service.personalization;

import java.util.Map;
import java.util.Set;

public final class SubAttributePolicy {

    private static final Set<String> PRONUNCIATION_FLUENCY = Set.of(
        "PRONUNCIATION",
        "FLUENCY"
    );

    /**
     * Closed taxonomy, 4 labels. Previously 13.
     *
     * <p>The test for keeping a label: <b>which lever can that label steer when generating a paper?</b> Each of the four labels below
     * maps to exactly one real lever -- the question time frame, the reasoning type,
     * the question form. The nine cut labels (sv_agreement, article_use, word_form,
     * third_person_s_omission, limited_range, repetition, weak_collocation, connector_overuse,
     * topic_drift) can all be MEASURED, but there is no way to generate a paper that targets them: one cannot
     * write a prompt "ask a question that makes the student get articles wrong". They only show up and nothing more.
     *
     * <p>VOCABULARY therefore has no label -- it can only be practised at the criterion level. Vocabulary
     * weakness is still MEASURED normally through the score, it just cannot be broken down further.
     */
    private static final Map<String, Set<String>> SUB_ATTRIBUTES = Map.of(
        "GRAMMAR", Set.of(
            // steers: tighten alternation of time frames between questions
            "tense_control",
            // steers: prefer reasoning_type ∈ {causal, hypothetical}
            "complex_clause_control"
        ),
        "COHERENCE", Set.of(
            // steers: prefer question_type ∈ {LONG_ANSWER, DESCRIPTION}
            "weak_progression",
            // steers: prefer question_type = OPINION
            "limited_support"
        )
    );

    private SubAttributePolicy() {
    }

    /**
     * Normalises the proposed sub-attribute against the closed set allowed for the criterion.
     * PRONUNCIATION/FLUENCY have no sub-attribute; a proposal outside the closed set is ignored (returns null).
     */
    public static String plannedSubAttribute(
            String criterion,
            String proposedSubAttribute) {
        if (PRONUNCIATION_FLUENCY.contains(criterion)) {
            return null;
        }
        Set<String> allowed = SUB_ATTRIBUTES.get(criterion);
        return allowed != null
                && proposedSubAttribute != null
                && allowed.contains(proposedSubAttribute)
            ? proposedSubAttribute
            : null;
    }

    /** The criterion that owns this sub-attribute in the taxonomy, or null if it belongs to no criterion. */
    public static String criterionForSubAttribute(String subAttribute) {
        return SUB_ATTRIBUTES.entrySet().stream()
            .filter(entry -> entry.getValue().contains(subAttribute))
            .map(entry -> entry.getKey())
            .findFirst()
            .orElse(null);
    }
}
