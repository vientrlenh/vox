package com.sep.vox.application.port.input.service;

import com.sep.vox.application.query.dto.SessionRowInfo;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.PracticeSessionEndedEvent;
import com.sep.vox.application.port.output.EventPublisherPort;
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
@RequiredArgsConstructor
@Slf4j
public class PracticeSessionClosedHandler {

    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final InterestVectorService interestVectorService;
    private final TopicOfferBackfillService topicOfferBackfillService;
    private final UndeliveredQuestionCleanupService undeliveredQuestionCleanupService;
    private final EventPublisherPort eventPublisherPort;
    private final JsonSerializationPort jsonSerializationPort;

    /**
     * Runs in its OWN TRANSACTION per session, not shared with the calling transaction.
     *
     * <p>Two reasons, both of which really broke when it was shared:
     * <ol>
     *   <li>{@code InterestVectorService.recordSessionOutcome} -> {@code replaceForStudent} does a DELETE and then an
     *       INSERT. That is only right when each transaction calls it once. The cleanup job handles many sessions in
     *       one loop, so the second call's delete does not reach its target (the first call's rows are still waiting
     *       to flush) and the insert then overwrites itself -> duplicate key on
     *       {@code uq_topic_interest_score_student_topic}.</li>
     *   <li>Postgres ABORTS the whole transaction as soon as one statement fails. Catching the exception on the Java side
     *       does NOT clear that state -- every later statement returns "current transaction is aborted". So a
     *       try/catch below on its own is not enough for "one broken session does not drag down the whole batch"; the
     *       transaction must be separated for the isolation to be real.</li>
     * </ol>
     * Measured: a single error made the whole cleanup job throw, and NO session got closed --
     * they stayed IN_PROGRESS indefinitely across many runs.
     *
     * <p>There is NO try/catch around the body here. Catching an exception INSIDE a @Transactional does not clear the
     * rollback-only flag that Spring put on the transaction when the exception crossed the inner
     * @Transactional boundary (recordSessionOutcome). After swallowing it the method returns normally, and at commit
     * time Spring throws UnexpectedRollbackException -- "Transaction silently rolled back because it has been marked
     * as rollback-only". Really measured: a catch placed here still killed the whole cleanup job.
     *
     * <p>Let it throw: this REQUIRES_NEW transaction rolls back cleanly, and the CALLER (outside the transaction
     * boundary) is the place that can really catch it.
     *
     * <p>Releasing the undelivered question comes BEFORE recordSessionOutcome, in its own transaction (REQUIRES_NEW), on
     * purpose: it is resource cleanup, not learning from the session just ended. If it fails it must not drag the interest score
     * down with it; conversely, if the interest score fails the question must still be returned.
     *
     * <p>The topic backfill runs IN THE BACKGROUND after EVERY session, not waiting for the list to drop below 3.
     * Why: the old condition "only generate when sparse" forced the student to practise a bored-of topic until it was
     * EXHAUSTED before seeing another -- a topic only leaves the list when it runs out of questions. For a session
     * just graded as BORED that is the worst thing to do. It runs AFTER recordSessionOutcome on purpose: the interest
     * score has just been updated, so this round of suggestions sees the latest picture instead of the picture before
     * the session.
     *
     * @param diagnosis the abandonment diagnosis (null if the student spoke) -- see SessionDiagnosisPolicy.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void afterClosed(UUID studentId, UUID sessionId, String status, String diagnosis) {
        SessionRowInfo row = practiceSessionQueryRepository.findSessionRowById(sessionId).orElse(null);
        if (row == null) {
            return;
        }
        releaseUndeliveredQuestionQuietly(studentId, sessionId, row);
        recordSessionOutcome(studentId, sessionId, status, diagnosis, row);
        topicOfferBackfillService.backfillAsync(studentId);
        publishSessionEnded(studentId, sessionId);
    }

    /**
     * Returns to the bank the questions that were CHOSEN but the student never answered.
     *
     * <p>Two sources produce a leftover question: the student closes the app right after receiving a question, and -- since
     * Python pre-loads the next question while the follow-up is still running -- a generated question that was not used in time.
     * Without cleanup, `student_question_exposures` marks it "seen" forever and the question
     * disappears from their bank although they never looked at it.
     */
    private void releaseUndeliveredQuestionQuietly(UUID studentId, UUID sessionId, SessionRowInfo row) {
        try {
            undeliveredQuestionCleanupService.releaseUndeliveredQuestion(
                studentId, sessionId, row.getPracticePaperId()
            );
        } catch (RuntimeException exception) {
            log.warn("Không trả được câu chưa dùng về kho cho phiên {}.", sessionId, exception);
        }
    }

    private void recordSessionOutcome(
            UUID studentId, UUID sessionId, String status, String diagnosis, SessionRowInfo row) {
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
    }

    /**
     * WARNING: since 2026-08-06 nobody listens to this event any more. The two old listeners,
     * WeaknessSnapshotRefreshJob (removed together with the weakness profile) and
     * TopicSuggestionSessionListener (removed together with the transcript read path), are gone.
     * It is kept as a hook for later work; if nobody listens by the time of the next cleanup,
     * delete PracticeSessionEndedEvent as well.
     */
    private void publishSessionEnded(UUID studentId, UUID sessionId) {
        eventPublisherPort.publish(new PracticeSessionEndedEvent(studentId, sessionId));
    }

    // Do NOT add an "afterClosedQuietly" method wrapping a try/catch here: calling afterClosed from this very
    // bean is a SELF-INVOCATION, which goes straight through without the Spring proxy, so
    // @Transactional(REQUIRES_NEW) stops working and we are back at the original problem. The caller (another bean)
    // catches the error itself.

    private List<UUID> offeredTopicIds(String json, String field) {
        return jsonSerializationPort.toStringListField(json, field).stream()
            .map(UUID::fromString)
            .toList();
    }
}
