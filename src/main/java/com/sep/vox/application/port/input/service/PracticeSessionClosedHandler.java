package com.sep.vox.application.port.input.service;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.PracticeSessionEndedEvent;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;

/**
 * Việc phải làm SAU KHI một phiên luyện đóng lại, bất kể nó đóng bằng cách nào.
 *
 * Vì sao tách ra: phiên đóng theo HAI đường -- học sinh bấm "Hoàn tất"
 * ({@code EndPracticeSessionUseCase}) hoặc job dọn tự đóng phiên treo
 * ({@code PracticeSessionCleanupService}). Trước đây toàn bộ phần "học từ buổi vừa rồi" chỉ
 * nằm ở đường thứ nhất.
 *
 * Đo trên dữ liệu thật: 4 trong 5 phiên gần nhất đóng bằng job dọn (nhận ra qua
 * {@code ended_at = last_heartbeat_at}) -- vì học sinh thường chỉ đóng app chứ không bấm nút.
 * Với 4 phiên đó thì điểm quan tâm không cập nhật, không sinh chủ đề mới, listener cũng không
 * chạy. Học sinh nói 4 buổi mà danh sách chủ đề đứng yên, và trông như hệ thống hỏng.
 *
 * Gộp vào một chỗ thay vì chép sang job dọn: chép thì hai bản sẽ trôi lệch ngay lần sửa sau.
 */
@Service
public class PracticeSessionClosedHandler {

    private static final org.slf4j.Logger LOGGER =
        org.slf4j.LoggerFactory.getLogger(PracticeSessionClosedHandler.class);

    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final InterestVectorService interestVectorService;
    private final TopicOfferBackfillService topicOfferBackfillService;
    private final UndeliveredQuestionCleanupService undeliveredQuestionCleanupService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final JsonSerializationPort jsonSerializationPort;

    public PracticeSessionClosedHandler(
            PracticeSessionQueryRepository practiceSessionQueryRepository,
            InterestVectorService interestVectorService,
            TopicOfferBackfillService topicOfferBackfillService,
            UndeliveredQuestionCleanupService undeliveredQuestionCleanupService,
            ApplicationEventPublisher applicationEventPublisher,
            JsonSerializationPort jsonSerializationPort) {
        this.practiceSessionQueryRepository = practiceSessionQueryRepository;
        this.interestVectorService = interestVectorService;
        this.topicOfferBackfillService = topicOfferBackfillService;
        this.undeliveredQuestionCleanupService = undeliveredQuestionCleanupService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    /**
     * Chạy trong TRANSACTION RIÊNG của từng phiên, không dùng chung với transaction gọi tới.
     *
     * Hai lý do, cả hai đều đã hỏng thật khi dùng chung:
     *
     * 1. {@code InterestVectorService.recordSessionOutcome} -> {@code replaceForStudent} làm
     *    XOÁ rồi CHÈN. Chỉ đúng khi mỗi transaction gọi một lần. Job dọn xử lý nhiều phiên
     *    trong một vòng lặp, nên lần gọi thứ hai xoá không tới nơi (bản ghi lần đầu còn chờ
     *    flush) rồi chèn đè lên chính nó -> duplicate key trên
     *    {@code uq_topic_interest_score_student_topic}.
     *
     * 2. Postgres ABORT cả transaction ngay khi một câu lệnh lỗi. Bắt exception phía Java
     *    KHÔNG gỡ được trạng thái đó -- mọi câu lệnh sau đều trả "current transaction is
     *    aborted". Nên try/catch bên dưới một mình không đủ để "một phiên hỏng không kéo theo
     *    cả loạt"; phải tách transaction thì mới thật sự cô lập.
     *
     * Đã đo: một lỗi duy nhất làm cả job dọn ném exception, và KHÔNG phiên nào được đóng --
     * chúng nằm IN_PROGRESS vô thời hạn qua nhiều lượt chạy.
     *
     * @param diagnosis chẩn đoán bỏ dở (null nếu học sinh có nói) -- xem SessionDiagnosisPolicy.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void afterClosed(UUID studentId, UUID sessionId, String status, String diagnosis) {
        var row = practiceSessionQueryRepository.findSessionRowById(sessionId).orElse(null);
        if (row == null) {
            return;
        }
        // KHÔNG try/catch ở đây. Bắt exception BÊN TRONG một @Transactional không gỡ được cờ
        // rollback-only mà Spring đã đóng lên transaction lúc exception đi qua ranh giới
        // @Transactional bên trong (recordSessionOutcome). Nuốt xong thì method trả về bình
        // thường, rồi tới lúc commit Spring ném UnexpectedRollbackException -- "Transaction
        // silently rolled back because it has been marked as rollback-only". Đo được thật:
        // chốt bắt lỗi đặt ở đây vẫn làm cả job dọn chết.
        //
        // Để nó ném ra: transaction REQUIRES_NEW này rollback gọn gàng, và NƠI GỌI (ngoài ranh
        // giới transaction) mới là chỗ bắt được thật.
        // Trả về kho câu đã CHỌN mà học sinh chưa bao giờ trả lời.
        //
        // Đặt TRƯỚC recordSessionOutcome và trong transaction riêng (REQUIRES_NEW) là cố ý: đây
        // là việc dọn tài nguyên, không phải việc học từ buổi vừa rồi. Nó hỏng thì không được
        // kéo theo điểm quan tâm; ngược lại điểm quan tâm hỏng thì câu vẫn phải được trả về.
        //
        // Hai nguồn sinh ra câu thừa: học sinh đóng app ngay khi vừa nhận câu, và -- từ khi
        // Python nạp trước câu tiếp theo trong lúc còn follow-up -- một câu sinh sẵn chưa kịp
        // dùng. Không dọn thì `student_question_exposures` đánh dấu "đã gặp" vĩnh viễn và câu đó
        // biến mất khỏi kho của em ấy dù chưa từng nhìn thấy.
        try {
            undeliveredQuestionCleanupService.releaseUndeliveredQuestion(
                studentId, sessionId, row.getPracticePaperId()
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Không trả được câu chưa dùng về kho cho phiên {}.", sessionId, exception);
        }
        interestVectorService.recordSessionOutcome(
            studentId,
            row.getChosenPracticeTopicId(),
            sessionId,
            row.getOrigin(),
            diagnosis,
            "COMPLETED".equals(status),
            row.getGradedSeconds(),
            offeredTopicIds(row.getOfferedTopicIdsJson(), "current"),
            offeredTopicIds(row.getOfferedTopicIdsJson(), "previous")
        );
        // Bổ sung chủ đề chạy NỀN sau MỖI phiên, không đợi danh sách tụt xuống dưới 3.
        //
        // Vì sao: điều kiện cũ "thưa thì mới sinh" buộc học sinh phải luyện CẠN một chủ đề
        // chán trước khi thấy chủ đề khác -- chủ đề chỉ rời danh sách khi hết câu hỏi. Với
        // một buổi vừa bị chấm là BORED thì đó đúng là điều tệ nhất có thể làm.
        //
        // Chạy SAU recordSessionOutcome là cố ý: điểm quan tâm vừa cập nhật xong, nên lượt
        // đề xuất này nhìn thấy bức tranh mới nhất thay vì bức tranh trước buổi học.
        topicOfferBackfillService.backfillAsync(studentId);
        // ⚠️ Từ 2026-08-06 sự kiện này KHÔNG CÒN NGƯỜI NGHE: hai listener cũ là
        // WeaknessSnapshotRefreshJob (gỡ cùng hồ sơ điểm yếu) và TopicSuggestionSessionListener
        // (gỡ cùng đường đọc transcript). Giữ lại làm điểm nối cho việc sau; nếu đến lúc dọn mà
        // vẫn không ai nghe thì xoá cả PracticeSessionEndedEvent.
        applicationEventPublisher.publishEvent(new PracticeSessionEndedEvent(studentId, sessionId));
    }

    // KHÔNG thêm một method "afterClosedQuietly" bọc try/catch ở đây: gọi afterClosed từ chính
    // bean này là TỰ GỌI, đi thẳng không qua proxy Spring, nên @Transactional(REQUIRES_NEW)
    // mất tác dụng và ta quay lại đúng bài toán ban đầu. Nơi gọi (bean khác) tự bắt lỗi.

    private List<UUID> offeredTopicIds(String json, String field) {
        return jsonSerializationPort.toStringListField(json, field).stream()
            .map(UUID::fromString)
            .toList();
    }
}
