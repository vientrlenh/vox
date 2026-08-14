package com.sep.vox.application.port.input.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.ExternalEventPublisherPort;
import com.sep.vox.domain.repository.PracticeItemResponseRepository;

/**
 * Xả chấm những câu học sinh đã nói nhưng bỏ dở, lúc đóng phiên.
 *
 * Vì sao cần: bài chấm chỉ được kích hoạt ở lượt CUỐI của một câu
 * ({@code SubmitPracticeTurnUseCase} bắn sự kiện khi {@code questionComplete}). Học sinh nói
 * một lượt rồi rớt mạng hoặc đóng app thì chuỗi follow-up không bao giờ kết thúc, nên câu đó
 * không bao giờ được chấm -- trong khi quota ĐÃ TRỪ, lượt ĐÃ GHI, lỗi sai ĐÃ hiện lên màn
 * hình. Công sức có thật mà hệ thống coi như không có.
 *
 * Hệ quả dây chuyền còn xa hơn điểm số: không có bản chấm thì không có
 * {@code weakness_observation}, nên hồ sơ điểm yếu đứng yên, nên vòng xoay chọn tiêu chí
 * không có gì mới để xoay.
 *
 * Điểm phiên KHÔNG tính những câu này -- xem {@code question_complete} trong
 * {@code SpringDataPracticeItemEvaluationRepository}. Chấm một câu dở dang theo rubric của câu
 * đầy đủ thì chắc chắn thấp; lấy tín hiệu thì được, tính điểm thì không công bằng.
 */
@Service
public class PracticeGradingFlushService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PracticeGradingFlushService.class);

    private final PracticeItemResponseRepository practiceItemResponseRepository;
    private final PracticeEvaluationRequestFactory evaluationRequestFactory;
    private final ExternalEventPublisherPort eventPublisher;

    public PracticeGradingFlushService(
            PracticeItemResponseRepository practiceItemResponseRepository,
            PracticeEvaluationRequestFactory evaluationRequestFactory,
            ExternalEventPublisherPort eventPublisher) {
        this.practiceItemResponseRepository = practiceItemResponseRepository;
        this.evaluationRequestFactory = evaluationRequestFactory;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Quét các phiên ĐÃ ĐÓNG còn lượt chưa chấm và xả chấm cho chúng.
     *
     * Xả lúc đóng phiên không phủ được hai nhóm: phiên đóng trước khi có cơ chế đó, và phiên
     * mà lần bắn sự kiện bị hỏng. Không quét lại thì chúng nằm mồ côi vĩnh viễn -- điểm phiên
     * thiếu, hồ sơ điểm yếu thiếu, và màn tổng kết báo "đang chấm" không bao giờ dứt.
     *
     * @return số câu đã bắn lại.
     */
    public int sweepEndedSessions(Instant since) {
        var sessionIds = practiceItemResponseRepository.findEndedSessionsWithUngradedResponses(since);
        var total = 0;
        for (var sessionId : sessionIds) {
            total += flush(sessionId);
        }
        if (total > 0) {
            LOGGER.info("Quét lại: xả chấm {} câu mồ côi ở {} phiên đã đóng.", total, sessionIds.size());
        }
        return total;
    }

    /**
     * Cửa sổ NGUỘI: đã gửi yêu cầu chấm trong khoảng này thì coi như đang chạy, không gửi lại.
     *
     * 15 phút chọn theo thời gian chấm đo được (~3,5 phút/câu) cộng thời gian xếp hàng khi nhiều
     * câu về cùng lúc. Ngắn hơn thì lại sinh trùng lặp -- đúng thứ cột này sinh ra để chặn; dài
     * hơn thì yêu cầu thất lạc thật phải nằm chờ lâu hơn mức cần thiết.
     */
    private static final Duration REQUEST_COOLDOWN = Duration.ofMinutes(15);

    /**
     * Trần số lần thử chấm một câu. Hỏng do DỮ LIỆU (audio vỡ, transcript rỗng, câu quá ngắn tới
     * mức không dựng nổi prompt) thì thử bao nhiêu lần cũng hỏng y hệt -- bắn lại vô hạn chỉ đốt
     * tiền LLM và giữ màn tổng kết quay mãi. Ba lần đủ để vượt qua lỗi thoáng qua (nghẽn mạng,
     * rate limit) mà không biến lỗi cố định thành vòng lặp vĩnh viễn.
     */
    public static final int MAX_GRADING_ATTEMPTS = 3;

    /** @return số câu đã bắn yêu cầu chấm bổ sung. */
    public int flush(UUID sessionId) {
        var pending = practiceItemResponseRepository.findResponsesAwaitingFlush(
            sessionId, Instant.now().minus(REQUEST_COOLDOWN), MAX_GRADING_ATTEMPTS
        );
        var flushed = 0;
        for (var row : pending) {
            try {
                eventPublisher.publish(evaluationRequestFactory.build(
                    sessionId, row.getResponseId(), row.getQuestionId()
                ));
                // Đóng dấu SAU khi publish thành công: đánh dấu trước rồi publish hỏng thì câu
                // đó bị khoá ngoài diện quét suốt cả cửa sổ nguội, tức mất hẳn một chu kỳ cứu.
                practiceItemResponseRepository.markGradingRequested(
                    row.getResponseId(), Instant.now()
                );
                flushed++;
            } catch (RuntimeException exception) {
                // Một câu hỏng không được kéo theo cả việc đóng phiên: đóng phiên là hành động
                // của học sinh, còn xả chấm là việc dọn dẹp của hệ thống.
                LOGGER.warn(
                    "Không xả được chấm cho câu {} của phiên {}.",
                    row.getQuestionId(), sessionId, exception
                );
            }
        }
        if (flushed > 0) {
            LOGGER.info("Đã xả chấm {} câu dở dang của phiên {}.", flushed, sessionId);
        }
        return flushed;
    }
}
