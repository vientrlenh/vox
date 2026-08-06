package com.sep.vox.domain.service.personalization;

import java.util.Map;
import java.util.Set;

public final class SubAttributePolicy {

    private static final Set<String> PRONUNCIATION_FLUENCY = Set.of(
        "PRONUNCIATION",
        "FLUENCY"
    );

    /**
     * Taxonomy đóng, 4 nhãn. Trước đây 13.
     *
     * <p>Phép thử để giữ một nhãn: <b>nhãn đó lái được cần gạt nào khi ra đề?</b> Bốn nhãn dưới
     * đây mỗi nhãn ứng với đúng một cần gạt có thật -- khung thời gian câu hỏi, kiểu lập luận,
     * dạng câu hỏi. Chín nhãn bị cắt (sv_agreement, article_use, word_form,
     * third_person_s_omission, limited_range, repetition, weak_collocation, connector_overuse,
     * topic_drift) đều ĐO được, nhưng không có cách nào ra một đề nhắm trúng chúng: không thể
     * viết prompt "hãy hỏi một câu khiến em sai mạo từ". Chúng chỉ hiện lên rồi thôi.
     *
     * <p>VOCABULARY vì thế không còn nhãn nào -- chỉ luyện được ở mức tiêu chí. Điểm yếu từ
     * vựng vẫn được ĐO bình thường qua điểm số, chỉ là không chia nhỏ hơn được.
     */
    private static final Map<String, Set<String>> SUB_ATTRIBUTES = Map.of(
        "GRAMMAR", Set.of(
            // lái: siết xen kẽ khung thời gian giữa các câu
            "tense_control",
            // lái: ưu tiên reasoning_type ∈ {causal, hypothetical}
            "complex_clause_control"
        ),
        "COHERENCE", Set.of(
            // lái: ưu tiên question_type ∈ {LONG_ANSWER, DESCRIPTION}
            "weak_progression",
            // lái: ưu tiên question_type = OPINION
            "limited_support"
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
