package com.sep.vox.application.port.input.service;

import java.util.HashMap;
import java.util.List;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.InterestQuizGenerationPort;
import com.sep.vox.application.port.output.PracticeGenerationExecutorPort;
import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;
import com.sep.vox.domain.repository.InterestQuizItemRepository;

/**
 * Bổ sung kho quiz sở thích khi có chiều chưa được phủ đủ.
 *
 * Vì sao cần: thêm một chiều mới vào danh mục KHÔNG tự làm nó xuất hiện trong quiz -- mọi
 * triplet trong kho đều đã gắn sẵn 3 chiều cũ. Không sinh bổ sung thì chiều mới vĩnh viễn
 * không bao giờ được hỏi, tức điểm của nó luôn rỗng: đúng kiểu "thêm vào nhưng im lặng vô
 * hiệu" mà việc đưa danh mục ra thành dữ liệu nhằm tránh.
 *
 * Runs in the background (through PracticeGenerationExecutorPort) because the LLM call is slow; the admin does not
 * have to wait after clicking to create a new dimension.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterestQuizBankTopUpService {

    /** Minimum number of appearances for each dimension in the shared bank. Below this threshold
     * the balancing selector has nothing to choose from, and a new dimension rarely makes it into the 7 questions asked. */
    private static final int MIN_COVERAGE_PER_DIMENSION = 3;
    private static final int MAX_ITEMS_PER_RUN = 7;

    private final InterestQuizItemRepository quizItemRepository;
    private final InterestQuizGenerationPort quizGenerationPort;
    private final InterestQuizScorer interestQuizScorer;
    private final PracticeGenerationExecutorPort practiceGenerationExecutorPort;

    public void topUpAsync() {
        practiceGenerationExecutorPort.runAsync(this::topUpSwallowingFailure);
    }

    private void topUpSwallowingFailure() {
        try {
            topUp();
        } catch (RuntimeException exception) {
            // Nuốt lỗi có chủ đích: đây là việc nền làm giàu kho, hỏng thì quiz vẫn chạy
            // bằng item cũ. Không được để nó làm hỏng luồng tạo chiều của admin.
            log.warn("Bổ sung kho quiz sở thích thất bại", exception);
        }
    }

    public int topUp() {
        List<String> dimensions = interestQuizScorer.quizDimensionCodes();
        if (dimensions.isEmpty()) {
            return 0;
        }
        List<InterestQuizSeedItem> pool = quizItemRepository.findAllActiveQuizItems();
        Map<String, Integer> coverage = new HashMap<String, Integer>();
        for (InterestQuizSeedItem item : pool) {
            for (String dimension : item.getDimensionPerStatement()) {
                coverage.merge(dimension, 1, (current, delta) -> current + delta);
            }
        }
        List<String> underCovered = dimensions.stream()
            .filter(dimension -> coverage.getOrDefault(dimension, 0) < MIN_COVERAGE_PER_DIMENSION)
            .toList();
        if (underCovered.isEmpty()) {
            return 0;
        }

        log.info(
            "Kho quiz sở thích thiếu phủ cho {} -- sinh bổ sung",
            underCovered
        );
        List<String> existingStatements = pool.stream()
            .flatMap(item -> item.getStatements().stream())
            .toList();
        // Vẫn gửi TOÀN BỘ danh mục chứ không chỉ chiều thiếu: mỗi triplet bắt buộc dùng 3
        // chiều KHÁC NHAU, nên nếu chỉ đưa 1 chiều thiếu thì không dựng nổi triplet nào.
        List<InterestQuizSeedItem> generated = quizGenerationPort.generate(
            MAX_ITEMS_PER_RUN,
            existingStatements,
            dimensions
        );
        List<InterestQuizSeedItem> usable = generated.stream()
            .filter(item -> item.getDimensionPerStatement().stream().anyMatch(underCovered::contains))
            .toList();
        if (usable.isEmpty()) {
            return 0;
        }
        saveShared(usable);
        return usable.size();
    }

    /** Lưu vào kho DÙNG CHUNG (student_id = null) chứ không gắn cho học sinh nào. */
    private void saveShared(List<InterestQuizSeedItem> items) {
        quizItemRepository.saveGeneratedForStudent(null, items);
    }
}
