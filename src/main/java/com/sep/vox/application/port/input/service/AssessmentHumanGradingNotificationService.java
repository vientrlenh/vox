package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.RoleConstant;
import com.sep.vox.application.event.AssessmentHumanGradingRequiredPayloadV1;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Tells the person in charge that an assessment has just closed with attempts still in "Waiting for AI score review"
 * ({@code PENDING_REVIEW}) that need manual grading.
 *
 * <p>Reminds RIGHT DURING THE ASSESSMENT, without waiting for it to close: a session that ends early already has results, and
 * making the grader wait until closing wastes most of the time available for grading. The periodic
 * sweep ({@code AssessmentHumanGradingNoticeBatch}) is the main path; the two closing branches still call it
 * again as a safety net for an assessment that closes before the sweep gets to run.
 *
 * <p>That is why {@code humanGradingNotifiedAt} is the part that CANNOT be dropped: the trigger is no longer a
 * single status transition but a sweep every minute, while the number of attempts waiting for grading
 * only keeps growing throughout the session. Without that column every assessment would be reminded again every minute until it closes.
 *
 * <p>In return, the number in the notification is the number AT THE TIME OF THE REMINDER, not the final total: attempts graded
 * afterwards do not produce a new notification. This is a deliberate choice -- the recipient needs to know "there is work, go
 * in", and the exact number is always shown by the grading queue anyway.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentHumanGradingNotificationService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    /**
     * {@code REQUIRED} and not {@code MANDATORY}: the two callers are different in nature --
     * {@code UpdateAssessmentStatusUseCase} is already inside its own transaction, while
     * {@code AssessmentStatusAutoTransitionJob} only has {@code @Scheduled} and so carries no
     * transaction. MANDATORY would throw straight away on the second path, which is the path that closes most
     * class tests.
     */
    @Transactional
    public void publishIfPendingReview(Assessment assessment, Instant now) {
        if (assessment == null) {
            return;
        }

        // Đã nhắc rồi thì thôi. Đây là chốt chống trùng cho CẢ hai đường vào: lượt quét định kỳ
        // gặp lại đúng bài thi đó ở tick sau, và nhánh đóng bài gọi lại lần nữa cho cùng bài.
        if (assessment.getHumanGradingNotifiedAt() != null) {
            return;
        }

        long pendingCount = assessmentCandidateResultRepository.findByAssessmentId(assessment.getId()).stream()
            .filter(result -> result.getStatus() == AssessmentCandidateResultStatus.PENDING_REVIEW)
            .count();

        // Không có bài nào chờ chấm là kết cục BÌNH THƯỜNG, không phải lỗi: AI chấm sạch cả
        // kỳ thì không ai phải làm gì, và một thông báo "0 bài cần chấm" chỉ gây nhiễu.
        if (pendingCount == 0) {
            return;
        }

        List<UUID> recipientIds = recipientsOf(assessment);
        if (recipientIds.isEmpty()) {
            // Bài trên lớp không có CHAIR, hoặc trường không còn admin nào đang hoạt động.
            // Bài vẫn nằm trong hàng đợi chấm, chỉ là không có ai để đánh động.
            log.warn("Bài thi {} còn {} bài chờ chấm tay nhưng không tìm được người nhận thông báo",
                assessment.getId(), pendingCount);
            return;
        }

        String payload = jsonSerializationPort.toJson(new AssessmentHumanGradingRequiredPayloadV1(
            recipientIds,
            assessment.getId(),
            assessment.getName(),
            assessment.getKind(),
            (int) pendingCount
        ));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.ASSESSMENT, assessment.getId(),
            EventTypeConstant.ASSESSMENT_HUMAN_GRADING_REQUIRED, payload, now
        ));

        // Đóng dấu trong CÙNG transaction với outbox: hai thứ này phải cùng sống hoặc cùng chết.
        // Ghi dấu mà không phát là im lặng mãi mãi; phát mà không ghi dấu là nhắc lại mỗi phút.
        assessment.setHumanGradingNotifiedAt(now);
        assessmentRepository.save(assessment);

        log.info("Bài thi {} có {} bài chờ chấm tay -- báo cho {} người",
            assessment.getId(), pendingCount, recipientIds.size());
    }

    /**
     * Bài trên lớp: CHAIR chính là giáo viên ra đề, và cũng là người duy nhất chấm nó -- xem
     * {@code ClassTestGradingAssignmentService.findClassTestChairId}, nơi mọi phân công đều
     * được mở dưới tên họ.
     *
     * <p>Kỳ thi tập trung: CHAIR (chủ tịch hội đồng, người chịu trách nhiệm về kỳ thi) CỘNG
     * school admin (người thực sự đi giao việc chấm qua AssignGradingUseCase). Gộp cả hai vì
     * thiếu bên nào cũng có ca không ai được báo: kỳ thi chưa gán chair, hoặc chair nghỉ.
     * {@code distinct} vì một người có thể vừa là chair vừa là school admin.
     */
    private List<UUID> recipientsOf(Assessment assessment) {
        Stream<UUID> chairIds = assessmentMemberRepository.findByAssessmentId(assessment.getId()).stream()
            .filter(member -> member.getRole() == AssessmentMemberRole.CHAIR)
            .map(member -> member.getUserId());

        if (assessment.getKind() == AssessmentKind.CLASS_TEST) {
            return chairIds.filter(userId -> userId != null).distinct().toList();
        }

        Stream<UUID> schoolAdminIds = schoolUserRepository
            .findBySchoolIdWithRole(assessment.getSchoolId(), RoleConstant.SCHOOL_ADMIN_ROLE)
            .stream()
            .map(schoolUser -> schoolUser.getUserId());

        return Stream.concat(chairIds, schoolAdminIds)
            .filter(userId -> userId != null)
            .distinct()
            .toList();
    }
}
