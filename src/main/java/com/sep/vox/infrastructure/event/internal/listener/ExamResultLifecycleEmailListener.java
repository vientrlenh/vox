package com.sep.vox.infrastructure.event.internal.listener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sep.vox.application.event.ExamAppealApprovedEvent;
import com.sep.vox.application.event.ExamResultInvalidClearedEvent;
import com.sep.vox.application.event.ExamResultInvalidatedEvent;
import com.sep.vox.application.event.ExamResultOutcomeDecidedEvent;
import com.sep.vox.application.event.ExamResultRegradedEvent;
import com.sep.vox.application.event.ExamResultReleasedEvent;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.UserRepository;

/**
 * Mail báo học sinh mọi mốc trong vòng đời điểm của họ.
 *
 * <p>Gom năm sự kiện vào một listener vì chúng dùng chung đúng một khuôn: giải
 * {@code studentId} thành email rồi render template. Tách thành năm lớp chỉ nhân bản
 * cùng một constructor năm lần.
 *
 * <p>{@code AFTER_COMMIT} là bắt buộc: mail đã gửi thì không rút lại được, nên chỉ
 * gửi khi thay đổi trong DB đã chắc chắn.
 */
@Component
public class ExamResultLifecycleEmailListener {

    private static final DateTimeFormatter DEADLINE_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final UserRepository userRepository;
    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;

    public ExamResultLifecycleEmailListener(
            UserRepository userRepository,
            MailSendingPort mailSendingPort,
            MailTemplatePort mailTemplatePort) {
        this.userRepository = userRepository;
        this.mailSendingPort = mailSendingPort;
        this.mailTemplatePort = mailTemplatePort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReleased(ExamResultReleasedEvent event) throws Exception {
        send(event.studentId(), "Điểm thi của bạn đã có",
            mailTemplatePort.renderResultReleasedEmail(event.examName(), format(event.totalScore())));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegraded(ExamResultRegradedEvent event) throws Exception {
        send(event.studentId(), "Điểm thi của bạn đã thay đổi",
            mailTemplatePort.renderResultRegradedEmail(
                event.examName(),
                roundLabel(event.roundType()),
                format(event.scoreBefore()),
                format(event.scoreAfter())));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvalidated(ExamResultInvalidatedEvent event) throws Exception {
        send(event.studentId(), "Bài thi của bạn đã bị vô hiệu",
            mailTemplatePort.renderResultInvalidatedEmail(event.examName(), event.reason()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvalidCleared(ExamResultInvalidClearedEvent event) throws Exception {
        send(event.studentId(), "Bài thi của bạn đã được khôi phục",
            mailTemplatePort.renderResultInvalidClearedEmail(event.examName(), event.reason()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutcomeDecided(ExamResultOutcomeDecidedEvent event) throws Exception {
        send(event.studentId(), "Kết quả cuối cùng của bạn",
            mailTemplatePort.renderResultOutcomeDecidedEmail(
                event.examName(), outcomeLabel(event.outcome()), format(event.totalScore())));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppealApproved(ExamAppealApprovedEvent event) throws Exception {
        send(event.studentId(), "Đơn phúc khảo của bạn đã được duyệt",
            mailTemplatePort.renderAppealApprovedEmail(event.examName(), formatDeadline(event.deadline())));
    }

    private void send(UUID recipientId, String subject, String html) throws Exception {
        if (recipientId == null) {
            return;
        }
        var recipient = userRepository.findById(recipientId).orElse(null);
        if (recipient == null) {
            return;
        }
        mailSendingPort.sendHtml(recipient.getEmail().value(), subject, html);
    }

    /** Học sinh không cần biết tên kỹ thuật của vòng chấm. */
    private String roundLabel(String roundType) {
        if (roundType == null) {
            return "chấm lại";
        }
        return switch (GradingRoundType.valueOf(roundType)) {
            case INITIAL -> "chấm lại";
            case SPOT_CHECK -> "hậu kiểm và chấm lại";
            case REMEDIATION -> "xem xét lại";
            case APPEAL -> "chấm lại theo đơn phúc khảo";
        };
    }

    private String outcomeLabel(String outcome) {
        return "PASSED".equals(outcome) ? "Đạt" : "Chưa đạt";
    }

    private String format(BigDecimal score) {
        return score == null ? "-" : score.toPlainString();
    }

    private String formatDeadline(OffsetDateTime deadline) {
        return deadline == null ? "chưa đặt" : DEADLINE_FORMAT.format(deadline);
    }
}
