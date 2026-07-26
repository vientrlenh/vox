package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sep.vox.application.query.dto.GradingRiskInfo;
import com.sep.vox.domain.model.exam.GradingSampleSelectionMode;

/**
 * Chọn tập bài để giao tự động cho một vòng chấm.
 *
 * <p>Tách khỏi use case vì đây là phần <em>duy nhất</em> có luật riêng đáng test kỹ:
 * bốc ngẫu nhiên phải lặp lại được khi có seed, và xếp hạng rủi ro phải giải thích
 * được cho nhà trường ("vì sao bài này bị chọn hậu kiểm").
 */
@Service
public class GradingSampleSelector {

    /** Nửa dải điểm quanh ngưỡng đạt được coi là "sát ngưỡng". */
    private static final BigDecimal BORDERLINE_BAND = new BigDecimal("0.50");

    private final Random random;

    public GradingSampleSelector() {
        this(new Random());
    }

    /** Cho test bơm seed cố định — bốc ngẫu nhiên phải kiểm chứng được. */
    public GradingSampleSelector(Random random) {
        this.random = random;
    }

    /**
     * @param candidates   toàn bộ bài đủ điều kiện, chưa có phân công mở
     * @param percent      chỉ dùng cho {@code RANDOM_PERCENT}; 1..100
     * @param manualIds    chỉ dùng cho {@code MANUAL_LIST}; phải là tập con của
     *                     {@code candidates} — id lạ bị từ chối chứ không bỏ qua lặng lẽ
     * @param riskInfos    chỉ dùng cho {@code RISK_BASED}
     */
    public List<UUID> select(GradingSampleSelectionMode mode, List<UUID> candidates, Integer percent,
            List<UUID> manualIds, List<GradingRiskInfo> riskInfos) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        return switch (mode) {
            case ALL -> List.copyOf(candidates);
            case RANDOM_PERCENT -> randomPercent(candidates, percent);
            case RISK_BASED -> riskBased(candidates, percent, riskInfos);
            case MANUAL_LIST -> manualList(candidates, manualIds);
        };
    }

    /**
     * Bốc {@code percent}% bài, làm tròn LÊN: chọn 10% của 5 bài phải ra 1 bài chứ
     * không phải 0 — hậu kiểm mà trả về rỗng thì admin tưởng hệ thống hỏng.
     */
    private List<UUID> randomPercent(List<UUID> candidates, Integer percent) {
        var size = sampleSize(candidates.size(), percent);
        var shuffled = new ArrayList<>(candidates);
        java.util.Collections.shuffle(shuffled, random);
        return List.copyOf(shuffled.subList(0, size));
    }

    /**
     * Xếp hạng rủi ro rồi lấy phần đầu. Không có {@code percent} thì lấy hết theo thứ
     * tự rủi ro giảm dần — admin vẫn thấy bài đáng ngờ nằm trên.
     *
     * <p>Hai tín hiệu, cộng lại:
     * <ul>
     *   <li>AI kém tự tin — {@code 1 - minConfidence}, thiếu dữ liệu thì coi như trung tính;
     *   <li>điểm sát ngưỡng đạt — trong dải {@value #BORDERLINE_BAND} quanh
     *       {@code passingScore} thì cộng thêm, vì đây là chỗ chấm sai gây hậu quả nặng nhất.
     * </ul>
     */
    private List<UUID> riskBased(List<UUID> candidates, Integer percent, List<GradingRiskInfo> riskInfos) {
        var infoById = riskInfos == null ? java.util.Map.<UUID, GradingRiskInfo>of()
            : riskInfos.stream().collect(Collectors.toMap(
                GradingRiskInfo::candidateResultId, Function.identity(), (left, right) -> left));

        var ranked = new ArrayList<>(candidates);
        ranked.sort(Comparator
            .comparing((UUID id) -> riskScore(infoById.get(id))).reversed()
            // Hoà thì theo id để kết quả ổn định giữa hai lần gọi.
            .thenComparing(Function.identity()));

        var size = percent == null ? ranked.size() : sampleSize(ranked.size(), percent);
        return List.copyOf(ranked.subList(0, size));
    }

    /** Điểm rủi ro trong [0, 2]; cao hơn = đáng soi hơn. */
    private BigDecimal riskScore(GradingRiskInfo info) {
        if (info == null) {
            return BigDecimal.ZERO;
        }
        var score = BigDecimal.ZERO;
        if (info.minConfidence() != null) {
            score = score.add(BigDecimal.ONE.subtract(info.minConfidence()));
        }
        if (info.passingScore() != null && info.totalScore() != null) {
            var distance = info.totalScore().subtract(info.passingScore()).abs();
            if (distance.compareTo(BORDERLINE_BAND) <= 0) {
                // Càng sát ngưỡng càng nặng: đúng ngưỡng = 1.0, mép dải = 0.
                score = score.add(BigDecimal.ONE.subtract(
                    distance.divide(BORDERLINE_BAND, 4, RoundingMode.HALF_UP)));
            }
        }
        return score;
    }

    private List<UUID> manualList(List<UUID> candidates, List<UUID> manualIds) {
        if (manualIds == null || manualIds.isEmpty()) {
            throw new IllegalArgumentException("Phải chọn ít nhất một bài thi để phân công.");
        }
        var allowed = new LinkedHashSet<>(candidates);
        var picked = new LinkedHashSet<UUID>();
        for (var id : manualIds) {
            if (!allowed.contains(id)) {
                throw new IllegalArgumentException(
                    "Có bài thi không đủ điều kiện cho vòng chấm này hoặc đã được phân công.");
            }
            picked.add(id);
        }
        return List.copyOf(picked);
    }

    private int sampleSize(int total, Integer percent) {
        if (percent == null || percent < 1 || percent > 100) {
            throw new IllegalArgumentException("Tỉ lệ chọn mẫu phải nằm trong khoảng 1 - 100.");
        }
        return Math.max(1, (int) Math.ceil(total * percent / 100.0));
    }
}
