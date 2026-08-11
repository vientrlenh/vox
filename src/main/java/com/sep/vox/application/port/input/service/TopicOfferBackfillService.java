package com.sep.vox.application.port.input.service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.repository.personalization.PracticeTopicRepository;

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
     * Kho đủ lớn thì hạ tốc độ sinh. Dưới ngưỡng là giai đoạn gây dựng -- học sinh mới hoặc vừa
     * dựng lại dữ liệu, cần kho đủ rộng để xếp hạng có gì mà chọn (ε-greedy còn đòi hẳn
     * {@code ranked.size() > 10} mới chạy). Trên ngưỡng thì mỗi phiên chỉ tiêu thụ một chủ đề,
     * nên sinh nhiều là phình ròng: đo sau 6 phiên với nhịp 8/phiên ra 56 chủ đề mà 37 cái chưa
     * từng được chào lấy một lần.
     */
    private static final long POOL_TARGET = 40;

    /** Nhịp sinh lúc kho còn mỏng -- ưu tiên lấp đầy nhanh. */
    private static final int BACKFILL_COUNT_BUILDING = 8;

    /**
     * Nhịp sinh khi kho đã đạt {@link #POOL_TARGET} -- chỉ đủ bù hao.
     *
     * <p>Vẫn nhanh hơn tốc độ tiêu thụ (1 chủ đề/phiên) nên kho không teo, mà không loãng thành
     * các biến thể na ná nhau: mọi lô đều sinh từ cùng sáu điểm sở thích gần như không đổi, nên
     * sinh càng dày thì lô sau càng giống lô trước và tự nghiền vào bộ lọc trùng của chính nó.
     */
    private static final int BACKFILL_COUNT_STEADY = 2;

    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();

    private final TopicSuggestionService topicSuggestionService;
    private final PracticeTopicRepository practiceTopicRepository;

    public TopicOfferBackfillService(
            TopicSuggestionService topicSuggestionService,
            PracticeTopicRepository practiceTopicRepository) {
        this.topicSuggestionService = topicSuggestionService;
        this.practiceTopicRepository = practiceTopicRepository;
    }

   
    @Async("practiceGenerationExecutor")
    public void backfillAsync(UUID studentId) {
        if (studentId == null || !inFlight.add(studentId)) {
            return;
        }
        try {
            // Đếm TRƯỚC mỗi lượt chứ không cache: kho là của chung, nhiều học sinh cùng đẩy nó
            // lên, nên một giá trị đọc lúc khởi động sẽ sai ngay sau vài phiên.
            var pool = practiceTopicRepository.countOfferablePool();
            var requested = pool < POOL_TARGET ? BACKFILL_COUNT_BUILDING : BACKFILL_COUNT_STEADY;
            var created = topicSuggestionService.synchronousOffers(studentId, requested);
            LOGGER.info(
                "Đã bổ sung {}/{} chủ đề cho học sinh {} (kho hiện có {}).",
                created.size(), requested, studentId, pool
            );
        } catch (RuntimeException exception) {
            // Nuốt lỗi có chủ đích: đây là việc nền làm giàu kho. Hỏng thì màn chọn chủ đề vẫn
            // hiện được những gì đang có, và lần mở sau sẽ thử lại.
            LOGGER.warn("Bổ sung chủ đề thất bại cho học sinh {}", studentId, exception);
        } finally {
            inFlight.remove(studentId);
        }
    }
}
