package com.sep.vox.domain.service.personalization;

import java.util.List;
import java.util.Map;

/**
 * Thì đích của một câu luyện: chọn ở đâu, và bậc nào thì được dùng thì nào.
 *
 * <p>Đây là thứ thay cho việc nhắm nhãn con điểm yếu (đã gỡ). Trước nói "em yếu
 * tense_control", giờ nói "ô này luyện quá khứ" -- nhắm rõ hơn, và nguồn là CHỦ ĐỀ chứ không
 * phải hồ sơ học sinh.
 *
 * <p><b>Không ép được thì bằng cách ra lệnh.</b> Bảo mô hình "viết câu luyện quá khứ đơn" thì
 * nó viết câu <em>nói về</em> quá khứ mà học sinh vẫn trả lời được bằng hiện tại. Thứ ép được
 * là mốc thời gian trong chính câu hỏi -- "What <b>did</b> you do last weekend?" thì quá khứ
 * là bắt buộc. Ràng buộc đó nằm ở prompt soạn câu bên Python; lớp này chỉ quyết định
 * <em>nhắm thì nào</em>.
 */
public final class TensePolicy {

    public static final String PRESENT = "PRESENT";
    public static final String PAST = "PAST";
    public static final String FUTURE = "FUTURE";
    public static final String PERFECT = "PERFECT";
    public static final String CONDITIONAL = "CONDITIONAL";

    /** Chủ đề nghiêng hẳn về một phía thời gian -- do topicGenerationGraph gắn lúc soạn. */
    public static final String AFFORDANCE_PAST = "PAST";
    public static final String AFFORDANCE_FUTURE = "FUTURE";
    public static final String AFFORDANCE_MIXED = "MIXED";

    /**
     * Thang khó thô của khung Robinson chỉ sinh được 6 mức -- xem {@code raw_difficulty} bên
     * Python. Hai hằng số này phải khớp với nó.
     */
    private static final int RAW_MIN = 1;
    private static final int RAW_MAX = 6;

    /**
     * Mức khó thô THẤP NHẤT mà một câu mang thì này có thể đạt được. Suy thẳng từ công thức
     * {@code raw = 1 + (not here_and_now) + (num_elements >= 4) + reasoning_weight
     * + (abstractness == "abstract")}:
     *
     * <ul>
     *   <li>quá khứ / tương lai / hoàn thành ⇒ câu KHÔNG neo vào hiện tại ⇒ {@code +1}</li>
     *   <li>điều kiện ⇒ {@code reasoning_type = hypothetical} ⇒ {@code +2}, cộng cả
     *       {@code here_and_now = False} nữa</li>
     * </ul>
     *
     * <p>Vì sao phải chặn: xin {@code rank = 1} kèm {@code CONDITIONAL} là yêu cầu TỰ MÂU
     * THUẪN -- không tổ hợp đặc trưng Robinson nào thoả cả hai. Không chặn thì hoặc câu bị loại
     * ở khâu sau (tốn một lượt LLM cho không), hoặc tệ hơn là lọt vào kho với {@code rank} sai.
     */
    private static final Map<String, Integer> RAW_FLOOR = Map.of(
        PRESENT, 1,
        PAST, 2,
        FUTURE, 2,
        PERFECT, 2,
        CONDITIONAL, 4
    );

    /**
     * Thứ tự xoay vòng cho chủ đề {@code MIXED}: dễ trước, khó sau. Chỉ là thứ tự ưu tiên khi
     * rải qua các ô, không phải điểm số.
     */
    private static final List<String> ROTATION = List.of(PRESENT, PAST, FUTURE, PERFECT, CONDITIONAL);

    private TensePolicy() {
    }

    /**
     * Những thì mà bậc này với tới được. Không bao giờ rỗng: {@link #PRESENT} có sàn 1 nên
     * luôn hợp lệ.
     */
    public static List<String> allowedFor(int targetRank, int bandCount) {
        var raw = rawForRank(targetRank, bandCount);
        return ROTATION.stream()
            .filter(tense -> RAW_FLOOR.get(tense) <= raw)
            .toList();
    }

    /**
     * Thì cho ô thứ {@code slotIndex} của phiên.
     *
     * <p>Chủ đề nói trước, xoay vòng nói sau. Chủ đề "Lịch sử trường em" mà ô 2 đòi tương lai
     * thì câu sinh ra gượng -- <em>"What will your school's history be like?"</em> vô nghĩa;
     * xoay vòng mù không biết điều đó.
     *
     * <p>Chu kỳ CỐ ĐỊNH chứ không ngẫu nhiên, cùng nguyên tắc với
     * {@code PracticeFocusInfo.criterionForSlot}: cùng học sinh ở cùng ô luôn ra cùng thì, nên
     * lỗi tái hiện được và "một buổi phủ nhiều khung thời gian" là điều chắc chắn xảy ra chứ
     * không phải may rủi.
     *
     * <p>Chủ đề nghiêng về một thì mà bậc không cho phép (ví dụ chủ đề lịch sử ở bậc 1, nơi chỉ
     * {@link #PRESENT} hợp lệ) thì bậc THẮNG: lùi về xoay vòng trong tập hợp lệ. Ép bằng được
     * thì đích sẽ tạo ra một câu tự mâu thuẫn về độ khó, còn hỏi chủ đề lịch sử bằng thì hiện
     * tại thì vẫn tự nhiên.
     */
    public static String forSlot(
            String temporalAffordance,
            int slotIndex,
            int targetRank,
            int bandCount) {
        var allowed = allowedFor(targetRank, bandCount);
        var preferred = switch (temporalAffordance == null ? AFFORDANCE_MIXED : temporalAffordance) {
            case AFFORDANCE_PAST -> PAST;
            case AFFORDANCE_FUTURE -> FUTURE;
            default -> null;
        };
        if (preferred != null && allowed.contains(preferred)) {
            return preferred;
        }
        return allowed.get(Math.max(0, slotIndex) % allowed.size());
    }

    /**
     * Bậc của khung -> mức khó thô Robinson. Nghịch đảo của {@code difficulty_rank} bên Python
     * ({@code raw_for_rank}); với thang 6 bậc là phép đồng nhất.
     *
     * <p>Không viết cứng theo 6: trường đổi sang IELTS 9 bậc thì hằng số 6 sẽ nói sai sàn của
     * mọi thì mà không báo lỗi.
     */
    private static int rawForRank(int rank, int bandCount) {
        var safeBandCount = Math.max(1, bandCount);
        var safeRank = Math.max(1, Math.min(safeBandCount, rank));
        if (safeBandCount == RAW_MAX) {
            return safeRank;
        }
        if (safeBandCount == 1) {
            return RAW_MIN;
        }
        var normalized = (double) (safeRank - 1) / (safeBandCount - 1);
        var raw = RAW_MIN + (int) Math.round(normalized * (RAW_MAX - RAW_MIN));
        return Math.max(RAW_MIN, Math.min(RAW_MAX, raw));
    }
}
