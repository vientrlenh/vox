package com.sep.vox.application.port.input.service;

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;
import com.sep.vox.domain.repository.personalization.InterestQuizItemRepository;
import com.sep.vox.infrastructure.service.InterestQuizGenerationClient;

/**
 * Bổ sung kho quiz sở thích khi có chiều chưa được phủ đủ.
 *
 * Vì sao cần: thêm một chiều mới vào danh mục KHÔNG tự làm nó xuất hiện trong quiz -- mọi
 * triplet trong kho đều đã gắn sẵn 3 chiều cũ. Không sinh bổ sung thì chiều mới vĩnh viễn
 * không bao giờ được hỏi, tức điểm của nó luôn rỗng: đúng kiểu "thêm vào nhưng im lặng vô
 * hiệu" mà việc đưa danh mục ra thành dữ liệu nhằm tránh.
 *
 * Chạy nền (@Async) vì gọi LLM chậm; admin không phải ngồi chờ lúc bấm tạo chiều mới.
 */
@Service
public class InterestQuizBankTopUpService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterestQuizBankTopUpService.class);

    /** Số lượt xuất hiện tối thiểu cho mỗi chiều trong kho dùng chung. Dưới ngưỡng này thì
     * bộ chọn cân bằng không có gì để chọn, chiều mới sẽ hiếm khi lọt vào 7 câu được hỏi. */
    private static final int MIN_COVERAGE_PER_DIMENSION = 3;
    private static final int MAX_ITEMS_PER_RUN = 7;

    private final InterestQuizItemRepository quizItemRepository;
    private final InterestQuizGenerationClient generationClient;
    private final InterestQuizScorer interestQuizScorer;

    public InterestQuizBankTopUpService(
            InterestQuizItemRepository quizItemRepository,
            InterestQuizGenerationClient generationClient,
            InterestQuizScorer interestQuizScorer) {
        this.quizItemRepository = quizItemRepository;
        this.generationClient = generationClient;
        this.interestQuizScorer = interestQuizScorer;
    }

    @Async("practiceGenerationExecutor")
    public void topUpAsync() {
        try {
            topUp();
        } catch (RuntimeException exception) {
            // Nuốt lỗi có chủ đích: đây là việc nền làm giàu kho, hỏng thì quiz vẫn chạy
            // bằng item cũ. Không được để nó làm hỏng luồng tạo chiều của admin.
            LOGGER.warn("Bổ sung kho quiz sở thích thất bại", exception);
        }
    }

    public int topUp() {
        var dimensions = interestQuizScorer.quizDimensionCodes();
        if (dimensions.isEmpty()) {
            return 0;
        }
        var pool = quizItemRepository.findAllActiveQuizItems();
        var coverage = new HashMap<String, Integer>();
        for (var item : pool) {
            for (var dimension : item.getDimensionPerStatement()) {
                coverage.merge(dimension, 1, Integer::sum);
            }
        }
        var underCovered = dimensions.stream()
            .filter(dimension -> coverage.getOrDefault(dimension, 0) < MIN_COVERAGE_PER_DIMENSION)
            .toList();
        if (underCovered.isEmpty()) {
            return 0;
        }

        LOGGER.info(
            "Kho quiz sở thích thiếu phủ cho {} -- sinh bổ sung",
            underCovered
        );
        var existingStatements = pool.stream()
            .flatMap(item -> item.getStatements().stream())
            .toList();
        // Vẫn gửi TOÀN BỘ danh mục chứ không chỉ chiều thiếu: mỗi triplet bắt buộc dùng 3
        // chiều KHÁC NHAU, nên nếu chỉ đưa 1 chiều thiếu thì không dựng nổi triplet nào.
        var generated = generationClient.generate(
            MAX_ITEMS_PER_RUN,
            existingStatements,
            dimensions
        );
        var usable = generated.stream()
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
