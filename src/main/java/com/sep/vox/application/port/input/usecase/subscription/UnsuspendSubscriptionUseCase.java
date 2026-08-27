package com.sep.vox.application.port.input.usecase.subscription;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UnsuspendSubscriptionCommand;
import com.sep.vox.application.port.input.service.SchoolSubscriptionSuspensionNotificationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionEvent;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.domain.repository.SchoolSubscriptionEventRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Gỡ đình chỉ, trả gói về ACTIVE.
 *
 * <p>KHÔNG kéo dài endDate để bù cho quãng bị đình chỉ. Đây là quyết định nghiệp vụ, không phải
 * thiếu sót: đình chỉ là chế tài với trường vi phạm, cộng bù thời gian sẽ biến nó thành hình phạt
 * không mất gì. Trường hợp đình chỉ nhầm thì đường xử lý là System Admin điều chỉnh tay, có actor và
 * lý do, chứ không phải một quy tắc tự động áp cho cả trường vi phạm thật.
 *
 * <p>Ba cột suspended_* bị xóa về null ở đây -- và chính vì thế lịch sử phải được ghi ở
 * school_subscription_events trước, nếu không sau lệnh này không còn dấu vết nào cho thấy trường
 * từng bị đình chỉ.
 */
@Service
public class UnsuspendSubscriptionUseCase implements IUseCase<UnsuspendSubscriptionCommand, UUID> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionEventRepository schoolSubscriptionEventRepository;
    private final SchoolSubscriptionSuspensionNotificationService suspensionNotificationService;
    private final UserContextPort userContextPort;

    public UnsuspendSubscriptionUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SchoolSubscriptionEventRepository schoolSubscriptionEventRepository,
            SchoolSubscriptionSuspensionNotificationService suspensionNotificationService,
            UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.schoolSubscriptionEventRepository = schoolSubscriptionEventRepository;
        this.suspensionNotificationService = suspensionNotificationService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UnsuspendSubscriptionCommand input) {
        var subscription = schoolSubscriptionRepository.findById(input.subscriptionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký"));
        if (subscription.getStatus() != SchoolSubscriptionStatus.SUSPENDED) {
            throw new IllegalStateException("Gói đăng ký này hiện không bị đình chỉ");
        }

        var now = Instant.now();
        var actorId = userContextPort.getCurrentAuthenticatedUserId();
        // Ghi chú không bắt buộc: gỡ đình chỉ là trả lại quyền, không phải tước đi, nên không cần
        // biện minh như lúc đình chỉ. CHECK ở DB cũng chỉ ép reason với SUSPENDED.
        var note = StringNormalization.trimAndCollapseSpaces(input.note());

        // Ghi sổ TRƯỚC khi xóa ba cột suspended_*: sau lệnh save bên dưới thì lý do đình chỉ không
        // còn ở đâu để mà chép lại nữa.
        schoolSubscriptionEventRepository.save(SchoolSubscriptionEvent.unsuspended(
            subscription.getSchoolId(), subscription.getId(), actorId, note, now));

        subscription.setStatus(SchoolSubscriptionStatus.ACTIVE);
        subscription.setSuspendedAt(null);
        subscription.setSuspendedReason(null);
        subscription.setSuspendedBy(null);
        var saved = schoolSubscriptionRepository.save(subscription);

        suspensionNotificationService.publishUnsuspended(saved.getId(), saved.getSchoolId(), now);

        return saved.getId();
    }
}
