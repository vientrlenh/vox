package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.RoleConstant;
import com.sep.vox.application.event.ExamHumanGradingRequiredPayloadV1;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Báo cho người phụ trách rằng một bài thi vừa đóng còn bài "Chờ soát điểm AI"
 * ({@code PENDING_REVIEW}) cần chấm tay.
 *
 * <p>Nhắc NGAY TRONG LÚC THI, không chờ đóng bài: ca thi kết thúc sớm đã có kết quả ngay, và
 * bắt người chấm ngồi chờ tới lúc đóng bài là vứt đi phần lớn thời gian chấm được. Lượt quét
 * định kỳ ({@code ExamHumanGradingNoticeBatch}) là đường chính; hai nhánh đóng bài vẫn gọi lại
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
public class ExamHumanGradingNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExamHumanGradingNotificationService.class);

    private final ExamRepository examRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public ExamHumanGradingNotificationService(
            ExamRepository examRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.examRepository = examRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    /**
     * {@code REQUIRED} chứ không {@code MANDATORY}: hai nơi gọi có tính chất khác nhau --
     * {@code UpdateExamStatusUseCase} đã ở trong transaction của nó, còn
     * {@code ExamStatusAutoTransitionJob} chỉ có {@code @Scheduled} nên không mang sẵn
     * transaction nào. MANDATORY sẽ ném ngay ở đường thứ hai, mà đó lại là đường đóng phần
     * lớn bài kiểm tra trên lớp.
     */
    @Transactional
    public void publishIfPendingReview(Exam exam, Instant now) {
        if (exam == null) {
            return;
        }

        // Đã nhắc rồi thì thôi. Đây là chốt chống trùng cho CẢ hai đường vào: lượt quét định kỳ
        // gặp lại đúng bài thi đó ở tick sau, và nhánh đóng bài gọi lại lần nữa cho cùng bài.
        if (exam.getHumanGradingNotifiedAt() != null) {
            return;
        }

        var pendingCount = examCandidateResultRepository.findByExamId(exam.getId()).stream()
            .filter(result -> result.getStatus() == ExamCandidateResultStatus.PENDING_REVIEW)
            .count();

        // Không có bài nào chờ chấm là kết cục BÌNH THƯỜNG, không phải lỗi: AI chấm sạch cả
        // kỳ thì không ai phải làm gì, và một thông báo "0 bài cần chấm" chỉ gây nhiễu.
        if (pendingCount == 0) {
            return;
        }

        var recipientIds = recipientsOf(exam);
        if (recipientIds.isEmpty()) {
            // Bài trên lớp không có CHAIR, hoặc trường không còn admin nào đang hoạt động.
            // Bài vẫn nằm trong hàng đợi chấm, chỉ là không có ai để đánh động.
            LOGGER.warn("Bài thi {} còn {} bài chờ chấm tay nhưng không tìm được người nhận thông báo",
                exam.getId(), pendingCount);
            return;
        }

        var payload = jsonSerializationPort.toJson(new ExamHumanGradingRequiredPayloadV1(
            recipientIds,
            exam.getId(),
            exam.getName(),
            exam.getKind(),
            (int) pendingCount
        ));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.EXAM, exam.getId(),
            EventTypeConstant.EXAM_HUMAN_GRADING_REQUIRED, payload, now
        ));

        // Đóng dấu trong CÙNG transaction với outbox: hai thứ này phải cùng sống hoặc cùng chết.
        // Ghi dấu mà không phát là im lặng mãi mãi; phát mà không ghi dấu là nhắc lại mỗi phút.
        exam.setHumanGradingNotifiedAt(now);
        examRepository.save(exam);

        LOGGER.info("Bài thi {} có {} bài chờ chấm tay -- báo cho {} người",
            exam.getId(), pendingCount, recipientIds.size());
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
    private List<UUID> recipientsOf(Exam exam) {
        var chairIds = examMemberRepository.findByExamId(exam.getId()).stream()
            .filter(member -> member.getRole() == ExamMemberRole.CHAIR)
            .map(member -> member.getUserId());

        if (exam.getKind() == ExamKind.CLASS_TEST) {
            return chairIds.filter(userId -> userId != null).distinct().toList();
        }

        var schoolAdminIds = schoolUserRepository
            .findBySchoolIdWithRole(exam.getSchoolId(), RoleConstant.SCHOOL_ADMIN_ROLE)
            .stream()
            .map(schoolUser -> schoolUser.getUserId());

        return Stream.concat(chairIds, schoolAdminIds)
            .filter(userId -> userId != null)
            .distinct()
            .toList();
    }
}
