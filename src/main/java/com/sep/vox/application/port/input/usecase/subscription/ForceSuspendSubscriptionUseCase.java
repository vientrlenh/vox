package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.subscription.SchoolSubscription;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ForceSuspendSubscriptionCommand;
import com.sep.vox.application.port.input.service.SchoolSubscriptionSuspensionNotificationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionEvent;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionEventRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * System Admin cưỡng chế cắt quyền dùng NGAY LẬP TỨC -- dùng khi phát hiện trường vi phạm (gian
 * lận...), luôn kèm lý do.
 *
 * <p>Khác CancelSubscriptionUseCase: hủy chỉ tắt gia hạn, gói vẫn ACTIVE và dùng được tới hết
 * endDate. Đình chỉ thì cắt ngay.
 *
 * <p>Mỗi lần đình chỉ ghi một dòng school_subscription_events. Ba cột suspended_* trên gói chỉ nói
 * "HIỆN có đang bị đình chỉ không" và bị xóa sạch khi gỡ -- không phải lịch sử. Xem
 * SchoolSubscriptionEvent.
 */
@Service
public class ForceSuspendSubscriptionUseCase implements IUseCase<ForceSuspendSubscriptionCommand, UUID> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionEventRepository schoolSubscriptionEventRepository;
    private final SchoolSubscriptionSuspensionNotificationService suspensionNotificationService;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final UserContextPort userContextPort;

    public ForceSuspendSubscriptionUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SchoolSubscriptionEventRepository schoolSubscriptionEventRepository,
            SchoolSubscriptionSuspensionNotificationService suspensionNotificationService,
            AssessmentScheduleRepository assessmentScheduleRepository,
            UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.schoolSubscriptionEventRepository = schoolSubscriptionEventRepository;
        this.suspensionNotificationService = suspensionNotificationService;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(ForceSuspendSubscriptionCommand input) {
        // Quyền đã chặn ở @PreAuthorize("hasRole('SYSTEM_ADMIN')"). Không kiểm lại isSystemAdmin() ở
        // đây: System Admin không thuộc trường nào nên không có "trường của mình" để đối chiếu, và
        // vai trò thì controller đã trả lời xong.
        String reason = StringNormalization.trimAndCollapseSpaces(input.reason());
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Phải nêu lý do đình chỉ gói đăng ký.");
        }

        SchoolSubscription subscription = schoolSubscriptionRepository.findById(input.subscriptionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký"));
        if (subscription.getStatus() != SchoolSubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ đình chỉ được gói đang ở trạng thái đang hoạt động");
        }

        Instant now = Instant.now();
        // Cắt quyền giữa ca thi là làm hỏng bài của học sinh đang ngồi trong phòng -- lỗi không thuộc
        // về các em. Bắt đợi hết ca rồi đình chỉ: chậm vài tiếng, đổi lại không ai mất bài.
        boolean hasOngoingAssessment = assessmentScheduleRepository.findBySchoolId(subscription.getSchoolId()).stream()
            .anyMatch(schedule -> schedule.isOngoingAt(now));
        if (hasOngoingAssessment) {
            throw new IllegalStateException(
                "Trường đang có ca thi diễn ra, không thể đình chỉ ngay bây giờ. Vui lòng thử lại sau khi ca thi kết thúc.");
        }

        UUID actorId = userContextPort.getCurrentAuthenticatedUserId();
        subscription.setStatus(SchoolSubscriptionStatus.SUSPENDED);
        subscription.setSuspendedAt(now);
        subscription.setSuspendedReason(reason);
        subscription.setSuspendedBy(actorId);
        SchoolSubscription saved = schoolSubscriptionRepository.save(subscription);

        schoolSubscriptionEventRepository.save(SchoolSubscriptionEvent.suspended(
            saved.getSchoolId(), saved.getId(), actorId, reason, now));

        suspensionNotificationService.publishSuspended(saved.getId(), saved.getSchoolId(), reason, now);

        return saved.getId();
    }
}
