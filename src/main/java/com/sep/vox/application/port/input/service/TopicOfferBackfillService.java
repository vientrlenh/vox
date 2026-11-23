package com.sep.vox.application.port.input.service;

import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.PracticeGenerationExecutorPort;
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
@RequiredArgsConstructor
@Slf4j
public class TopicOfferBackfillService {

    /**
     * Once the bank is large enough, slow generation down. Below the threshold is the build-up phase -- a new student or freshly
     * rebuilt data -- and the bank must be wide enough for ranking to have something to choose from (ε-greedy even requires
     * {@code ranked.size() > 10} before it runs). Above the threshold each session consumes only one topic,
     * so generating a lot just bloats the bank: measured after 6 sessions at 8 per session, that gave 56 topics, 37 of which
     * were never offered even once.
     */
    private static final long POOL_TARGET = 40;

    /** Generation pace while the bank is thin -- prioritize filling it quickly. */
    private static final int BACKFILL_COUNT_BUILDING = 8;

    /**
     * Generation pace once the bank has reached {@link #POOL_TARGET} -- only enough to cover wear.
     *
     * <p>Still faster than the consumption rate (1 topic/session), so the bank does not shrink, yet it does not dilute into
     * near-identical variants: every batch is generated from the same six interest scores, which barely change, so
     * the denser the generation, the more a batch resembles the previous one and grinds itself against its own duplicate filter.
     */
    private static final int BACKFILL_COUNT_STEADY = 2;

    /**
     * Prevents duplicate runs across the WHOLE CLUSTER, not only in the RAM of one pod.
     *
     * <p>The previous version was {@code ConcurrentHashMap.newKeySet()} -- it only saw the runs of
     * its own pod. With 3 replicas behind an ALB that balances round-robin, three consecutive
     * {@code practiceTopicOffers} calls landing on three pods are three topic-generation runs in parallel
     * for the same student: the LLM cost triples, and worse, all three load {@code activeNameCards()}
     * BEFORE the other two have written, so they do not see each other -- the duplicate check by name is ineffective and only
     * Chroma on the Python side helps.
     *
     * <p>(The call path from {@code PracticeSessionClosedHandler.afterClosed} was already safe: the session cleanup
     * job uses {@code FOR UPDATE SKIP LOCKED}, so only one pod can hold that session.)
     */
    private static final String IN_FLIGHT_KEY_PREFIX = "practice:backfill:";

    /**
     * Clearly longer than one generation run so the lock does not expire midway: the HTTP timeout to the agents is 45
     * seconds, plus the time to write the topics and index them into the vector store.
     *
     * <p>This is only a safety net for the case where a pod dies midway -- the normal path always removes
     * the lock in {@code finally}.
     */
    private static final Duration IN_FLIGHT_TTL = Duration.ofMinutes(2);

    private final TopicSuggestionService topicSuggestionService;
    private final PracticeTopicRepository practiceTopicRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final CacheManagerPort cacheManagerPort;
    private final PracticeGenerationExecutorPort practiceGenerationExecutorPort;

    public void backfillAsync(UUID studentId) {
        practiceGenerationExecutorPort.runAsync(() -> backfill(studentId));
    }

    private void backfill(UUID studentId) {
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
        // không bám vào ai. Mà practice_topics là kho DÙNG CHUNG toàn hệ và những dòng đó ở
        // lại vĩnh viễn: một lần đăng nhập của một tài khoản chưa làm quiz là đủ định hình
        // kho cho mọi học sinh sau đó.
        //
        // Đường gọi từ PracticeSessionClosedHandler về lý thuyết luôn có quiz rồi (phải luyện
        // xong một phiên mới tới đó), nên với nó điều kiện này là vô hại.
        if (!hasCompletedInterestQuiz(studentId)) {
            log.debug("Bỏ lượt bổ sung chủ đề: học sinh {} chưa nộp quiz sở thích.", studentId);
            return;
        }
        String key = IN_FLIGHT_KEY_PREFIX + studentId;
        // Token riêng cho mỗi lượt: nhả khoá phải chứng minh được mình là chủ, không thì một lượt
        // chạy quá hạn có thể xoá nhầm khoá của lượt sau.
        String token = UUID.randomUUID().toString();
        if (!token.equals(cacheManagerPort.saveIfAbsentAndGet(key, token, IN_FLIGHT_TTL))) {
            // Pod nào đó (kể cả chính pod này) đang chạy rồi -- bỏ lượt, đúng như bản cũ.
            return;
        }
        try {
            // Đếm TRƯỚC mỗi lượt chứ không cache: kho là của chung, nhiều học sinh cùng đẩy nó
            // lên, nên một giá trị đọc lúc khởi động sẽ sai ngay sau vài phiên.
            long pool = practiceTopicRepository.countOfferablePool();
            int requested = pool < POOL_TARGET ? BACKFILL_COUNT_BUILDING : BACKFILL_COUNT_STEADY;
            List<PracticePlanningResponses.PracticeTopicOffer> created = topicSuggestionService.synchronousOffers(studentId, requested);
            log.info(
                "Đã bổ sung {}/{} chủ đề cho học sinh {} (kho hiện có {}).",
                created.size(), requested, studentId, pool
            );
        } catch (RuntimeException exception) {
            // Nuốt lỗi có chủ đích: đây là việc nền làm giàu kho. Hỏng thì màn chọn chủ đề vẫn
            // hiện được những gì đang có, và lần mở sau sẽ thử lại.
            log.warn("Bổ sung chủ đề thất bại cho học sinh {}", studentId, exception);
        } finally {
            cacheManagerPort.deleteIfValueMatches(key, token);
        }
    }

    /** See the comment at the call site in {@link #backfill(UUID)}. */
    private boolean hasCompletedInterestQuiz(UUID studentId) {
        return learnerProfileRepository.findCurrent(studentId)
            .map(profile -> profile.getQuizCompletedAt() != null)
            .orElse(false);
    }
}
