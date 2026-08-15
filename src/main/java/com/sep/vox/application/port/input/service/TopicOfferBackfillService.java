package com.sep.vox.application.port.input.service;

import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.domain.repository.LearnerProfileRepository;
import com.sep.vox.domain.repository.PracticeTopicRepository;

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

    /**
     * Chống chạy trùng trên TOÀN CỤM, không phải trong RAM một pod.
     *
     * <p>Bản trước là {@code ConcurrentHashMap.newKeySet()} -- chỉ thấy được các lượt chạy của
     * chính pod đó. Với 3 replica sau ALB chia tải luân phiên thì ba lời gọi
     * {@code practiceTopicOffers} liên tiếp rơi vào ba pod là ba lượt sinh chủ đề chạy song song
     * cho cùng một học sinh: tiền LLM nhân ba, và tệ hơn là cả ba đều nạp {@code activeNameCards()}
     * TRƯỚC khi hai lượt kia kịp ghi nên không thấy nhau -- phép chống trùng theo tên vô hiệu, chỉ
     * còn Chroma bên Python đỡ.
     *
     * <p>(Đường gọi từ {@code PracticeSessionClosedHandler.afterClosed} vốn đã an toàn: job dọn
     * phiên dùng {@code FOR UPDATE SKIP LOCKED} nên chỉ một pod cầm được phiên đó.)
     */
    private static final String IN_FLIGHT_KEY_PREFIX = "practice:backfill:";

    /**
     * Dài hơn hẳn một lượt sinh để khoá không hết hạn giữa chừng: HTTP timeout sang agents là 45
     * giây, cộng thời gian ghi chủ đề và index sang vector store.
     *
     * <p>Đây chỉ là lưới an toàn cho trường hợp pod chết giữa chừng -- đường bình thường luôn xoá
     * khoá trong {@code finally}.
     */
    private static final Duration IN_FLIGHT_TTL = Duration.ofMinutes(2);

    private final TopicSuggestionService topicSuggestionService;
    private final PracticeTopicRepository practiceTopicRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final CacheManagerPort cacheManagerPort;

    public TopicOfferBackfillService(
            TopicSuggestionService topicSuggestionService,
            PracticeTopicRepository practiceTopicRepository,
            LearnerProfileRepository learnerProfileRepository,
            CacheManagerPort cacheManagerPort) {
        this.topicSuggestionService = topicSuggestionService;
        this.practiceTopicRepository = practiceTopicRepository;
        this.learnerProfileRepository = learnerProfileRepository;
        this.cacheManagerPort = cacheManagerPort;
    }


    @Async("practiceGenerationExecutor")
    public void backfillAsync(UUID studentId) {
        if (studentId == null) {
            return;
        }
        // Chưa nộp quiz sở thích thì KHÔNG sinh gì cả -- đây là nửa còn lại của cổng đặt ở
        // ViewPracticeTopicOffersUseCase, và là nửa QUAN TRỌNG HƠN.
        //
        // Chỉ chặn bên use case là không đủ: nó trả rỗng, mà rỗng thì
        // PracticePlanningController thấy `offers.size() < 3` và gọi thẳng vào đây. Kết quả
        // còn tệ hơn trước -- lô chào bị chặn nhưng kho vẫn bị lấp.
        //
        // Lượt sinh lúc đó gửi `interestScores` RỖNG sang LLM (xem
        // TopicSuggestionService.synchronousOffers), nên chủ đề nhận về là loại chung chung
        // không bám vào ai. Mà practice_topic là kho DÙNG CHUNG toàn hệ và những dòng đó ở
        // lại vĩnh viễn: một lần đăng nhập của một tài khoản chưa làm quiz là đủ định hình
        // kho cho mọi học sinh sau đó.
        //
        // Đường gọi từ PracticeSessionClosedHandler về lý thuyết luôn có quiz rồi (phải luyện
        // xong một phiên mới tới đó), nên với nó điều kiện này là vô hại.
        if (!hasCompletedInterestQuiz(studentId)) {
            LOGGER.debug("Bỏ lượt bổ sung chủ đề: học sinh {} chưa nộp quiz sở thích.", studentId);
            return;
        }
        var key = IN_FLIGHT_KEY_PREFIX + studentId;
        // Token riêng cho mỗi lượt: nhả khoá phải chứng minh được mình là chủ, không thì một lượt
        // chạy quá hạn có thể xoá nhầm khoá của lượt sau.
        var token = UUID.randomUUID().toString();
        if (!token.equals(cacheManagerPort.saveIfAbsentAndGet(key, token, IN_FLIGHT_TTL))) {
            // Pod nào đó (kể cả chính pod này) đang chạy rồi -- bỏ lượt, đúng như bản cũ.
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
            cacheManagerPort.deleteIfValueMatches(key, token);
        }
    }

    /** Xem chú thích tại chỗ gọi trong {@link #backfillAsync(UUID)}. */
    private boolean hasCompletedInterestQuiz(UUID studentId) {
        return learnerProfileRepository.findCurrent(studentId)
            .map(profile -> profile.getQuizCompletedAt() != null)
            .orElse(false);
    }
}
