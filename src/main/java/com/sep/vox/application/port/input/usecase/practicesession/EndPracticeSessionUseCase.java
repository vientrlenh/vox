package com.sep.vox.application.port.input.usecase.practicesession;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.PracticeSessionEndedEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.practicesession.PracticeSessionResponseMapper;
import com.sep.vox.application.mapper.practicesession.SessionRowMapper;
import com.sep.vox.application.port.input.command.EndPracticeSessionCommand;
import com.sep.vox.application.port.input.service.InterestVectorService;
import com.sep.vox.application.port.input.service.PracticeGradingFlushService;
import com.sep.vox.application.port.input.service.PracticeSessionClosedHandler;
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

    private static final Logger LOGGER =
        LoggerFactory.getLogger(EndPracticeSessionUseCase.class);

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final PracticeItemEvaluationRepository practiceItemEvaluationRepository;
    private final PracticeGradingFlushService gradingFlushService;
    private final PracticeSessionClosedHandler sessionClosedHandler;
    private final UserContextPort userContextPort;

    public EndPracticeSessionUseCase(
            PracticeSessionRepository practiceSessionRepository,
            PracticeSessionQueryRepository practiceSessionQueryRepository,
            PracticeItemEvaluationRepository practiceItemEvaluationRepository,
            PracticeGradingFlushService gradingFlushService,
            PracticeSessionClosedHandler sessionClosedHandler,
            UserContextPort userContextPort) {
        this.practiceSessionRepository = practiceSessionRepository;
        this.practiceSessionQueryRepository = practiceSessionQueryRepository;
        this.practiceItemEvaluationRepository = practiceItemEvaluationRepository;
        this.gradingFlushService = gradingFlushService;
        this.sessionClosedHandler = sessionClosedHandler;
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
        // Phần "học từ buổi vừa rồi" nằm chung với đường job dọn -- xem
        // PracticeSessionClosedHandler. Để riêng ở đây thì phiên bị job đóng (đa số, vì học
        // sinh thường chỉ đóng app) sẽ không cập nhật điểm quan tâm và không sinh chủ đề mới.
        try {
            sessionClosedHandler.afterClosed(studentId, sessionId, status, diagnosis);
        } catch (RuntimeException exception) {
            // Phiên ĐÃ đóng xong ở trên; phần ghi nhận phía sau hỏng thì không được làm hỏng
            // cả yêu cầu kết thúc phiên của học sinh. Bắt ở đây, ngoài ranh giới REQUIRES_NEW.
            LOGGER.warn("Ghi nhận sau khi kết thúc phiên {} thất bại.", sessionId, exception);
        }
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

}
