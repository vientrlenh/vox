package com.sep.vox.application.port.input.usecase.practicesession;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.PracticeSessionEndedEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.practicesession.PracticeSessionResponseMapper;
import com.sep.vox.application.mapper.practicesession.SessionRowMapper;
import com.sep.vox.application.port.input.command.EndPracticeSessionCommand;
import com.sep.vox.application.port.input.service.InterestVectorService;
import com.sep.vox.application.port.input.service.PracticeGradingFlushService;
import com.sep.vox.application.port.input.service.TopicOfferBackfillService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.PracticeSession;
import com.sep.vox.domain.repository.personalization.PracticeItemEvaluationRepository;
import com.sep.vox.domain.repository.personalization.PracticeSessionRepository;
import com.sep.vox.domain.service.personalization.SessionDiagnosisPolicy;

@Service
public class EndPracticeSessionUseCase implements IUseCase<EndPracticeSessionCommand, PracticeSession> {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final PracticeItemEvaluationRepository practiceItemEvaluationRepository;
    private final PracticeGradingFlushService gradingFlushService;
    private final InterestVectorService interestVectorService;
    private final TopicOfferBackfillService topicOfferBackfillService;
    private final JsonSerializationPort jsonSerializationPort;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final UserContextPort userContextPort;

    public EndPracticeSessionUseCase(
            PracticeSessionRepository practiceSessionRepository,
            PracticeSessionQueryRepository practiceSessionQueryRepository,
            PracticeItemEvaluationRepository practiceItemEvaluationRepository,
            PracticeGradingFlushService gradingFlushService,
            InterestVectorService interestVectorService,
            TopicOfferBackfillService topicOfferBackfillService,
            JsonSerializationPort jsonSerializationPort,
            ApplicationEventPublisher applicationEventPublisher,
            UserContextPort userContextPort) {
        this.practiceSessionRepository = practiceSessionRepository;
        this.practiceSessionQueryRepository = practiceSessionQueryRepository;
        this.practiceItemEvaluationRepository = practiceItemEvaluationRepository;
        this.gradingFlushService = gradingFlushService;
        this.interestVectorService = interestVectorService;
        this.topicOfferBackfillService = topicOfferBackfillService;
        this.jsonSerializationPort = jsonSerializationPort;
        this.applicationEventPublisher = applicationEventPublisher;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public PracticeSession execute(EndPracticeSessionCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var sessionId = input.sessionId();
        requireOwnedInProgress(sessionId, studentId);
        // Xả chấm TRƯỚC khi đọc điểm: câu học sinh bỏ dở chưa từng được chấm, và nếu không
        // bắn ở đây thì không bao giờ có ai bắn nữa. Kết quả về sau vài chục giây --
        // RecordPracticeAttemptEvaluationUseCase sẽ tự cập nhật lại overall_score lúc đó.
        gradingFlushService.flush(sessionId);
        var lastScore = practiceItemEvaluationRepository.findLastValidNormalizedScore(sessionId);
        var session = practiceSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên luyện."));
        // COMPLETED = học sinh CÓ NÓI, không phải "đã chấm xong".
        //
        // Bản trước đếm countCompletedBySessionId, tức đếm bản chấm ĐÃ VỀ DB. Nhưng chấm là
        // bất đồng bộ: đo trên dữ liệu thật, ended_at 11:03:25 còn evaluated_at 11:04:10 --
        // về SAU 45 giây. Nên phiên nào cũng thành ABANDONED kể cả khi học sinh nói đủ lượt
        // rồi tự bấm kết thúc.
        //
        // Cái giá không chỉ là một cái nhãn sai: recordSessionOutcome thoát ngay nếu không
        // COMPLETED (điểm quan tâm không bao giờ được ghi), findDashboardCounts lọc
        // status='COMPLETED' (số buổi và điểm trung bình luôn 0), và findLastAbandonDiagnosis
        // gán BORED/TOO_HARD cho những phiên hoàn toàn bình thường rồi dùng nó chọn đề.
        //
        // Trạng thái phải mô tả HÀNH VI NGƯỜI HỌC, không mô tả độ trễ hạ tầng. Kết quả chấm
        // về sau vẫn ghi vào practice_item_evaluation và overall_score tính lại từ đó -- học
        // sinh vào Lịch sử xem lại là thấy.
        //
        // gradedSeconds là giây VAD nghe thấy tiếng, cùng con số quota trừ. Mở phiên rồi
        // thoát mà chưa nói gì thì vẫn ABANDONED -- đúng nghĩa bỏ dở.
        var status = session.getGradedSeconds() > 0 ? "COMPLETED" : "ABANDONED";
        var diagnosis = "ABANDONED".equals(status)
            ? SessionDiagnosisPolicy.diagnose(lastScore, input.helpRequestCount(), input.longPauseCount())
            : null;
        practiceSessionRepository.save(session.ended(
            status,
            diagnosis,
            input.helpRequestCount(),
            input.longPauseCount(),
            Instant.now(),
            practiceItemEvaluationRepository.findAverageItemScoreBySessionId(sessionId)
        ));
        var row = practiceSessionQueryRepository.findSessionRowById(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên luyện."));
        interestVectorService.recordSessionOutcome(
            studentId,
            row.getChosenPracticeTopicId(),
            sessionId,
            row.getOrigin(),
            diagnosis,
            "COMPLETED".equals(status),
            offeredTopicIds(row.getOfferedTopicIdsJson(), "current"),
            offeredTopicIds(row.getOfferedTopicIdsJson(), "previous")
        );
        // Bổ sung chủ đề chạy NỀN sau MỖI phiên, không đợi danh sách tụt xuống dưới 3.
        //
        // Vì sao: điều kiện cũ "thưa thì mới sinh" buộc học sinh phải luyện CẠN một chủ đề
        // chán trước khi thấy chủ đề khác -- chủ đề chỉ rời danh sách khi hết câu hỏi. Với
        // một buổi vừa bị chấm là BORED thì đó đúng là điều tệ nhất có thể làm.
        //
        // Chạy sau recordSessionOutcome là cố ý: điểm quan tâm vừa cập nhật xong (chủ đề vừa
        // luyện lên hoặc xuống, chủ đề bị bỏ qua bị hạ), nên lượt đề xuất này nhìn thấy bức
        // tranh mới nhất thay vì bức tranh trước buổi học.
        //
        // KHÔNG xoá chủ đề nào: practice_topic dùng chung cho mọi học sinh, xoá là làm hỏng
        // của người khác. Việc "bớt" đã có cơ chế riêng theo từng học sinh -- điểm quan tâm
        // thấp thì tụt hạng, và chủ đề đã lưu vẫn giữ nguyên trong mySavedTopics.
        topicOfferBackfillService.backfillAsync(studentId);
        applicationEventPublisher.publishEvent(new PracticeSessionEndedEvent(studentId, sessionId));
        return PracticeSessionResponseMapper.toResponse(
            SessionRowMapper.toDto(
                practiceSessionQueryRepository.findSessionRow(sessionId, studentId).orElse(null)
            )
        );
    }

    private void requireOwnedInProgress(UUID sessionId, UUID studentId) {
        if (!practiceSessionRepository.existsByIdAndStudentIdAndStatus(sessionId, studentId, "IN_PROGRESS")) {
            throw new NotFoundException("Phiên luyện không còn hoạt động.");
        }
    }

    private List<UUID> offeredTopicIds(String json, String field) {
        return jsonSerializationPort.toStringListField(json, field).stream()
            .map(UUID::fromString)
            .toList();
    }
}
