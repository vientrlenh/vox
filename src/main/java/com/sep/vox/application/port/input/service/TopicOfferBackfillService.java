package com.sep.vox.application.port.input.service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Bổ sung kho chủ đề luyện tập khi danh sách đề xuất còn thưa -- chạy NỀN.
 *
 * Vì sao không sinh ngay trong request: {@code TopicSuggestionService.synchronousOffers} gọi
 * LLM đề xuất chủ đề rồi index sang vector store, hàng chục giây. Lúc kho còn trống (học sinh
 * mới, hoặc vừa dựng lại dữ liệu) thì LẦN NÀO mở màn chọn chủ đề cũng rơi vào đường đó, nên
 * request đầu tiên sau khi làm xong quiz luôn timeout -- đúng lúc học sinh hào hứng nhất.
 *
 * Đổi hình dạng thay vì nới trần: trả về ngay những gì đang có (kể cả rỗng), sinh chạy nền,
 * client hiện "đang tổng hợp, quay lại sau 1-2 phút". Chủ đề sinh xong được ghi thẳng vào kho
 * ({@code createTopic} bên trong synchronousOffers), nên lần mở sau đường xếp hạng thông
 * thường tự thấy chúng -- không cần pha hai hay endpoint hỏi lại.
 */
@Service
public class TopicOfferBackfillService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopicOfferBackfillService.class);

    /**
     * Số chủ đề xin sinh mỗi lượt chạy nền. Rộng tay vì đây là việc NỀN -- học sinh không ngồi
     * chờ, nên xin dư còn hơn để kho lớn nhỏ giọt. Đo thật với con số 3: bộ lọc trùng-gần cắt
     * 2, mỗi lượt chỉ ra ĐÚNG MỘT chủ đề, phải tải lại ba lần mới đủ danh sách.
     */
    private static final int BACKFILL_COUNT = 8;

    /**
     * Học sinh đang có lượt sinh chạy dở. Màn chọn chủ đề bị kéo-làm-mới vài lần liên tiếp
     * (đúng thứ học sinh sẽ làm khi thấy "quay lại sau 1-2 phút") mà không chặn thì thành
     * nhiều lượt gọi LLM song song cho cùng một người.
     */
    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();

    private final TopicSuggestionService topicSuggestionService;

    public TopicOfferBackfillService(TopicSuggestionService topicSuggestionService) {
        this.topicSuggestionService = topicSuggestionService;
    }

    /**
     * Chốt chống trùng nằm BÊN TRONG method @Async, không phải ở một method bọc ngoài: gọi
     * method @Async từ chính bean này sẽ đi thẳng, bỏ qua proxy AOP của Spring, và biến việc
     * nền thành việc đồng bộ ngay trong request -- đúng thứ đang cần tránh.
     */
    @Async("practiceGenerationExecutor")
    public void backfillAsync(UUID studentId) {
        if (studentId == null || !inFlight.add(studentId)) {
            return;
        }
        try {
            var created = topicSuggestionService.synchronousOffers(studentId, BACKFILL_COUNT);
            LOGGER.info("Đã bổ sung {} chủ đề cho học sinh {}.", created.size(), studentId);
        } catch (RuntimeException exception) {
            // Nuốt lỗi có chủ đích: đây là việc nền làm giàu kho. Hỏng thì màn chọn chủ đề vẫn
            // hiện được những gì đang có, và lần mở sau sẽ thử lại.
            LOGGER.warn("Bổ sung chủ đề thất bại cho học sinh {}", studentId, exception);
        } finally {
            inFlight.remove(studentId);
        }
    }
}
