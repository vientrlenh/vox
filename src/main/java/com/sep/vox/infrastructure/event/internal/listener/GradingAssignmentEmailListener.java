package com.sep.vox.infrastructure.event.internal.listener;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.event.GradingAssignmentDeclinedEvent;
import com.sep.vox.application.event.GradingDeadlineReminderEvent;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.UserRepository;

/**
 * Mail cho phía <em>vận hành</em>: nhắc giáo viên sắp tới hạn, và báo admin khi có
 * người trả lại phân công.
 *
 * <p>Tách khỏi {@link ExamResultLifecycleEmailListener} vì người nhận khác hẳn — ở
 * đây là nhân sự của trường, không phải học sinh; trộn chung dễ dẫn tới gửi nhầm nhóm.
 */
@Component
public class GradingAssignmentEmailListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradingAssignmentEmailListener.class);

    /** {@code withZone} là phần bắt buộc: thiếu nó thì mail ghi giờ UTC của container. */
    private static final DateTimeFormatter DEADLINE_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(DateMapper.DEFAULT_INPUT_ZONE);

    private final UserRepository userRepository;
    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;

    public GradingAssignmentEmailListener(
            UserRepository userRepository,
            MailSendingPort mailSendingPort,
            MailTemplatePort mailTemplatePort) {
        this.userRepository = userRepository;
        this.mailSendingPort = mailSendingPort;
        this.mailTemplatePort = mailTemplatePort;
    }

    /**
     * Lỗi mail chỉ được log: {@code GradingDeadlineReminderBatch} đánh dấu
     * {@code reminded_at} và commit TRƯỚC khi listener này chạy, nên ném ra ngoài sẽ chặn
     * luôn các phân công còn lại trong lô — và chúng đã bị đánh dấu là đã nhắc, không bao
     * giờ được gửi lại.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeadlineReminder(GradingDeadlineReminderEvent event) {
        if (event.teacherId() == null) {
            return;
        }
        try {
            var teacher = userRepository.findById(event.teacherId()).orElse(null);
            if (teacher == null) {
                return;
            }
            var html = mailTemplatePort.renderGradingDeadlineReminderEmail(
                event.examName() == null ? "-" : event.examName(),
                roundLabel(event.roundType()),
                format(event.deadlineAt()));
            mailSendingPort.sendHtml(teacher.getEmail().value(), "Nhắc hạn chấm bài", html);
        } catch (Exception e) {
            LOGGER.error("Không gửi được mail nhắc hạn chấm bài cho phân công {}: {}",
                event.assignmentId(), e.getMessage());
        }
    }

    /** Người nhận là admin ĐÃ GIAO bài, không phải học sinh: bài đang chờ giao lại. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeclined(GradingAssignmentDeclinedEvent event) throws Exception {
        if (event.assignedBy() == null) {
            return;
        }
        var admin = userRepository.findById(event.assignedBy()).orElse(null);
        if (admin == null) {
            return;
        }
        var teacherName = event.teacherId() == null ? "-" : userRepository.findById(event.teacherId())
            .map(user -> user.getFullName().value())
            .orElse("-");
        var html = mailTemplatePort.renderGradingDeclinedEmail(
            event.examName() == null ? "-" : event.examName(),
            teacherName,
            event.reason() == null ? "-" : event.reason());
        mailSendingPort.sendHtml(
            admin.getEmail().value(), "Có giáo viên trả lại phân công chấm bài", html);
    }

    private String roundLabel(String roundType) {
        if (roundType == null) {
            return "-";
        }
        return switch (GradingRoundType.valueOf(roundType)) {
            case INITIAL -> "Chấm lần đầu";
            case SPOT_CHECK -> "Hậu kiểm";
            case REMEDIATION -> "Xem xét bài bị vô hiệu";
            case APPEAL -> "Phúc khảo";
        };
    }

    private String format(Instant deadline) {
        return deadline == null ? "-" : DEADLINE_FORMAT.format(deadline);
    }
}
