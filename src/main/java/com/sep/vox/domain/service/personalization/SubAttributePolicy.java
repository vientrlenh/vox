package com.sep.vox.domain.service.personalization;

import java.util.Map;
import java.util.Set;

public final class SubAttributePolicy {

    private static final Set<String> PRONUNCIATION_FLUENCY = Set.of(
        "PRONUNCIATION",
        "FLUENCY"
    );
    private static final Map<String, Set<String>> SUB_ATTRIBUTES = Map.of(
        "GRAMMAR", Set.of(
            "sv_agreement",
            "tense_control",
            "complex_clause_control",
            "third_person_s_omission",
            "article_use",
            "word_form"
        ),
        "VOCABULARY", Set.of(
            "limited_range",
            "repetition",
            "weak_collocation"
        ),
        "COHERENCE", Set.of(
            "weak_progression",
            "limited_support",
            "connector_overuse",
            "topic_drift"
        )
    );

    private SubAttributePolicy() {
    }

    /**
     * Chuẩn hoá sub-attribute đề xuất theo đúng tập đóng cho phép của tiêu chí.
     * PRONUNCIATION/FLUENCY không có sub-attribute; đề xuất không nằm trong tập đóng bị bỏ qua (trả null).
     */
    public static String plannedSubAttribute(
            String criterion,
            String proposedSubAttribute) {
        if (PRONUNCIATION_FLUENCY.contains(criterion)) {
            return null;
        }
        var allowed = SUB_ATTRIBUTES.get(criterion);
        return allowed != null
                && proposedSubAttribute != null
                && allowed.contains(proposedSubAttribute)
            ? proposedSubAttribute
            : null;
    }

    /** Tiêu chí sở hữu sub-attribute này trong taxonomy, hoặc null nếu không thuộc tiêu chí nào. */
    public static String criterionForSubAttribute(String subAttribute) {
        return SUB_ATTRIBUTES.entrySet().stream()
            .filter(entry -> entry.getValue().contains(subAttribute))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }
}
