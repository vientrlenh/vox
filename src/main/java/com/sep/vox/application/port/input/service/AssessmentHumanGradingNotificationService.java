package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.assessment.AssessmentHumanGradingRequiredPayload;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.shared.RoleConstant;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.shared.AggregateTypeConstant;
import com.sep.vox.domain.shared.EventTypeConstant;

/**
 * Báo cho người phụ trách rằng một bài thi vừa đóng còn bài "Chờ soát điểm AI"
 * ({@code PENDING_REVIEW}) cần chấm tay.
 *
 * <p>Nhắc NGAY TRONG LÚC THI, không chờ đóng bài: ca thi kết thúc sớm đã có kết quả ngay, và
 * bắt người chấm ngồi chờ tới lúc đóng bài là vứt đi phần lớn thời gian chấm được. Lượt quét
 * định kỳ ({@code AssessmentHumanGradingNoticeBatch}) là đường chính; hai nhánh đóng bài vẫn gọi lại
 * như lưới hứng cho bài đóng trước khi lượt quét kịp chạy.
 *
 * <p>Vì vậy {@code humanGradingNotifiedAt} là phần KHÔNG thể bỏ: mốc kích hoạt không còn là một
 * lần chuyển trạng thái duy nhất nữa mà là một lượt quét mỗi phút, trong khi số bài chờ chấm chỉ
 * tăng dần suốt buổi thi. Thiếu cột đó thì mỗi bài thi bị nhắc lại mỗi phút cho tới lúc đóng.
 *
 * <p>Đổi lại, con số trong thông báo là con số TẠI LÚC NHẮC, không phải tổng cuối: bài chấm xong
 * sau đó không sinh thông báo mới. Đó là lựa chọn có chủ ý -- người nhận cần biết "có việc, vào
 * đi", còn con số chính xác thì hàng đợi chấm luôn hiển thị sẵn.
 */
@Service
public class AssessmentHumanGradingNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssessmentHumanGradingNotificationService.class);

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public AssessmentHumanGradingNotificationService(
            AssessmentRepository assessmentRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            SchoolUserRepository schoolUserRepository,
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    /**
     * {@code REQUIRED} chứ không {@code MANDATORY}: hai nơi gọi có tính chất khác nhau --
     * {@code UpdateAssessmentStatusUseCase} đã ở trong transaction của nó, còn
     * {@code AssessmentStatusAutoTransitionJob} chỉ có {@code @Scheduled} nên không mang sẵn
     * transaction nào. MANDATORY sẽ ném ngay ở đường thứ hai, mà đó lại là đường đóng phần
     * lớn bài kiểm tra trên lớp.
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

        var pendingCount = assessmentCandidateResultRepository.findByAssessmentId(assessment.getId()).stream()
            .filter(result -> result.getStatus() == AssessmentCandidateResultStatus.PENDING_REVIEW)
            .count();

        // Không có bài nào chờ chấm là kết cục BÌNH THƯỜNG, không phải lỗi: AI chấm sạch cả
        // kỳ thì không ai phải làm gì, và một thông báo "0 bài cần chấm" chỉ gây nhiễu.
        if (pendingCount == 0) {
            return;
        }

        var recipientIds = recipientsOf(assessment);
        if (recipientIds.isEmpty()) {
            // Bài trên lớp không có CHAIR, hoặc trường không còn admin nào đang hoạt động.
            // Bài vẫn nằm trong hàng đợi chấm, chỉ là không có ai để đánh động.
            LOGGER.warn("Bài thi {} còn {} bài chờ chấm tay nhưng không tìm được người nhận thông báo",
                assessment.getId(), pendingCount);
            return;
        }

        var payload = jsonSerializationPort.toJson(new AssessmentHumanGradingRequiredPayload(
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

        LOGGER.info("Bài thi {} có {} bài chờ chấm tay -- báo cho {} người",
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
        var chairIds = assessmentMemberRepository.findByAssessmentId(assessment.getId()).stream()
            .filter(member -> member.getRole() == AssessmentMemberRole.CHAIR)
            .map(member -> member.getUserId());

        if (assessment.getKind() == AssessmentKind.CLASS_TEST) {
            return chairIds.filter(userId -> userId != null).distinct().toList();
        }

        var schoolAdminIds = schoolUserRepository
            .findBySchoolIdWithRole(assessment.getSchoolId(), RoleConstant.SCHOOL_ADMIN_ROLE)
            .stream()
            .map(schoolUser -> schoolUser.getUserId());

        return Stream.concat(chairIds, schoolAdminIds)
            .filter(userId -> userId != null)
            .distinct()
            .toList();
    }
}
