package com.sep.vox.application.port.input.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;

/**
 * Chọn đúng N triplet sẽ đem ra hỏi, từ kho item đang có.
 *
 * Giải quyết hai việc mà việc "lấy N item đầu tiên" không làm được:
 *
 * 1. LOẠI item chứa chiều không còn hỏi được (đã tắt, hoặc quiz_eligible=false). Nếu không
 *    lọc, học sinh vẫn bị hỏi triplet đó và lựa chọn của họ rơi vào chiều mà
 *    {@link InterestQuizScorer#normalize} không duyệt -> điểm bị VỨT ÂM THẦM, câu hỏi coi
 *    như phí mà không ai biết. Đây đúng loại lỗi "im lặng vô hiệu" mà việc đưa danh mục
 *    chiều ra thành dữ liệu nhằm diệt.
 *
 * 2. PHỦ ĐỀU các chiều trong đúng ngân sách câu hỏi cố định. Số câu quiz giữ nguyên 5-7
 *    (không nới), nên khi admin thêm chiều thì mỗi chiều tự nhiên được ít lượt xuất hiện
 *    hơn. Chọn tham lam theo "chiều nào đang ít lượt nhất thì ưu tiên" giúp tiêu ngân sách
 *    đó đều nhất có thể, thay vì để vài chiều được hỏi 3 lần còn chiều khác 0 lần -- chiều
 *    0 lượt thì điểm của nó hoàn toàn vô nghĩa.
 */
@Service
public class InterestQuizItemSelector {

    private final InterestQuizScorer interestQuizScorer;

    public InterestQuizItemSelector(InterestQuizScorer interestQuizScorer) {
        this.interestQuizScorer = interestQuizScorer;
    }

    public List<InterestQuizSeedItem> select(List<InterestQuizSeedItem> pool, int count) {
        var eligible = Set.copyOf(interestQuizScorer.quizDimensionCodes());
        var usable = pool.stream()
            .filter(item -> item.getDimensionPerStatement() != null
                && item.getDimensionPerStatement().size() == 3
                && eligible.containsAll(item.getDimensionPerStatement()))
            .toList();
        if (usable.size() <= count) {
            return usable;
        }

        var coverage = new HashMap<String, Integer>();
        var remaining = new ArrayList<>(usable);
        var selected = new ArrayList<InterestQuizSeedItem>();
        while (selected.size() < count && !remaining.isEmpty()) {
            var best = remaining.get(0);
            var bestScore = coverageScore(best, coverage);
            for (var candidate : remaining) {
                var score = coverageScore(candidate, coverage);
                if (score > bestScore) {
                    best = candidate;
                    bestScore = score;
                }
            }
            remaining.remove(best);
            selected.add(best);
            for (var dimension : best.getDimensionPerStatement()) {
                coverage.merge(dimension, 1, (current, delta) -> current + delta);
            }
        }
        return selected;
    }

    /** Càng nhiều chiều đang ít lượt thì điểm càng cao. 1/(1+n) giảm dần nên item phủ chiều
     * chưa xuất hiện lần nào luôn thắng item phủ chiều đã có sẵn vài lượt. */
    private static double coverageScore(
            InterestQuizSeedItem item,
            java.util.Map<String, Integer> coverage) {
        var score = 0.0;
        for (var dimension : item.getDimensionPerStatement()) {
            score += 1.0 / (1 + coverage.getOrDefault(dimension, 0));
        }
        return score;
    }
}
