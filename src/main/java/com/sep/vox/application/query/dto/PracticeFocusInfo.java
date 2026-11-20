package com.sep.vox.application.query.dto;

import java.util.List;
import java.util.Map;

/**
 * Tiêu điểm luyện tập của một học sinh: các tiêu chí xếp theo mức yếu, kèm sub-attribute đáng
 * luyện nhất của từng tiêu chí.
 *
 * Trước đây record này chỉ mang ĐÚNG HAI tiêu chí (primary/secondary), nên ba tiêu chí còn lại
 * không bao giờ được hỏi tới -- dù truy vấn nguồn
 * ({@code findFocusCriterionCodesOrderedByWeakness}) vốn đã trả về cả danh sách xếp hạng. Học
 * sinh yếu ngữ pháp thì mãi mãi chỉ luyện ngữ pháp, còn phát âm hay mạch lạc không bao giờ
 * được chạm tới cho tới khi nó tụt xuống thành một trong hai yếu nhất.
 */
public record PracticeFocusInfo(
    /** Tiêu chí xếp theo mức yếu giảm dần. Không bao giờ rỗng -- xem resolveFocus. */
    List<String> orderedCriteria,
    /**
     * Sub-attribute của từng tiêu chí, XẾP THEO mức ưu tiên giảm dần. Thiếu khoá hoặc danh
     * sách rỗng nghĩa là "luyện tiêu chí này nói chung".
     */
    Map<String, List<String>> subAttributesByCriterion
) {

    /**
     * Tiêu chí cho ô thứ {@code slotIndex}, theo chu kỳ 4: yếu nhất, yếu nhất, yếu nhì, xoay
     * vòng phần còn lại.
     *
     * Tỉ lệ ra 50% cho tiêu chí yếu nhất, 25% cho yếu nhì, 25% rải đều các tiêu chí khác --
     * tập trung vào chỗ yếu nhưng không đóng khung ở đúng một thứ. Luyện mãi một tiêu chí thì
     * ba việc hỏng cùng lúc: học sinh chán, các tiêu chí khác không có quan sát mới nên hồ sơ
     * điểm yếu đứng yên, và ước lượng bậc mất nguồn dữ liệu ở những tiêu chí đó.
     *
     * Chu kỳ CỐ ĐỊNH chứ không ngẫu nhiên: cùng một học sinh ở cùng một ô luôn ra cùng tiêu
     * chí, nên lỗi tái hiện được và "thỉnh thoảng xen lẫn" là điều chắc chắn xảy ra chứ không
     * phải may rủi.
     */
    public String criterionForSlot(int slotIndex) {
        var safeIndex = Math.max(0, slotIndex);
        return switch (safeIndex % 4) {
            case 0, 1 -> at(0);
            case 2 -> at(1);
            // Ô xen lẫn: xoay vòng qua các tiêu chí TỪ THỨ BA trở đi, để mỗi vòng lại đụng một
            // tiêu chí khác thay vì lặp lại đúng tiêu chí thứ ba mãi.
            default -> orderedCriteria.size() <= 2
                ? at(1)
                : at(2 + (safeIndex / 4) % (orderedCriteria.size() - 2));
        };
    }

    /**
     * Sub-attribute cho ô đó; null nghĩa là luyện tiêu chí nói chung.
     *
     * Cùng nguyên tắc với {@link #criterionForSlot}, một tầng sâu hơn: nhãn yếu nhất chiếm
     * phần lớn nhưng vẫn xoay vòng sang các nhãn còn lại. Bản trước lấy {@code findFirst()}
     * nên trong GRAMMAR mọi câu đều nhắm đúng một nhãn (ví dụ tense_control), còn article_use
     * hay word_form không bao giờ được luyện -- và vì không được luyện nên không có quan sát
     * mới, nên thứ hạng của chúng đứng yên vĩnh viễn.
     *
     * Chu kỳ 3: hai lượt cho nhãn yếu nhất, một lượt xoay vòng phần còn lại (~67/33).
     *
     * Đếm theo số lần TIÊU CHÍ NÀY đã xuất hiện, không theo slotIndex thô: tiêu chí xen lẫn
     * chỉ rơi vào ô 3, 7, 11... nên nếu lấy slotIndex % 3 thì nó luôn ở cùng một pha và nhãn
     * phụ không bao giờ tới lượt.
     */
    public String subAttributeForSlot(int slotIndex) {
        var criterion = criterionForSlot(slotIndex);
        var candidates = subAttributesByCriterion.get(criterion);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        var occurrence = occurrenceOfCriterion(criterion, slotIndex);
        if (candidates.size() == 1 || occurrence % 3 != 2) {
            return candidates.get(0);
        }
        return candidates.get(1 + (occurrence / 3) % (candidates.size() - 1));
    }

    /** Đây là lần thứ mấy tiêu chí này được hỏi, tính từ ô 0. */
    private int occurrenceOfCriterion(String criterion, int slotIndex) {
        var count = 0;
        for (var index = 0; index < slotIndex; index++) {
            if (criterion.equals(criterionForSlot(index))) {
                count++;
            }
        }
        return count;
    }

    private String at(int index) {
        return orderedCriteria.get(Math.min(index, orderedCriteria.size() - 1));
    }
}
