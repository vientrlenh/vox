package com.sep.vox.application.port.input.usecase.subscription;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Trường báo sẽ KHÔNG mua tiếp sau khi kỳ hiện tại kết thúc.
 *
 * <p>Hành động này KHÔNG cắt quyền dùng và KHÔNG hoàn tiền: trường đã trả cho trọn kỳ nên dùng bình
 * thường tới hết endDate, rồi SubscriptionExpiryJob tự chuyển sang EXPIRED như mọi kỳ khác. Muốn cắt
 * ngay thì đó là đình chỉ (ForceSuspendSubscriptionUseCase), và đó là quyền của System Admin.
 *
 * <p>QUAN TRỌNG -- {@code cancelledAt} ở đây là một GHI NHẬN Ý ĐỊNH, không phải một công tắc điều
 * khiển thứ gì. Hệ thống KHÔNG có gia hạn tự động: muốn có kỳ mới thì trường phải tự đặt đơn
 * (RenewSchoolSubscriptionUseCase). Nên "hủy" ở đây không dừng được khoản thu nào cả -- vốn dĩ chẳng
 * có khoản thu nào sắp tự xảy ra. Giá trị của nó là tín hiệu: cho FE hiện đúng trạng thái, và cho
 * bên kinh doanh biết trường nào định rời đi mà còn kịp giữ. Đừng viết logic nào dựa vào cột này với
 * giả định nó chặn được gia hạn.
 *
 * <p>Không có đường gỡ hủy. Nếu sau này thêm, {@code cancelledAt} sẽ bị xóa về null và lúc đó nó
 * không còn là lịch sử nữa -- phải ghi thêm một dòng school_subscription_events như đình chỉ đang
 * làm. Hiện tại cột này chỉ được ghi một lần và không bao giờ xóa, nên bản thân nó đã là lịch sử.
 */
@Service
@RequiredArgsConstructor
public class CancelSchoolSubscriptionUseCase implements IUseCase<Void, UUID> {

    private static final DateTimeFormatter DISPLAY_DATE =
        DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneConstant.BUSINESS_ZONE);

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(Void input) {
        // Does NOT accept subscriptionId: a school has exactly one period in force, and accepting an id from outside means
        // hasRole('SCHOOL_ADMIN') cannot answer "does this period belong to that school" -- yet another ownership-check
        // layer would have to be built. With no id there is nothing to check.
        UUID schoolId = userContextPort.getCurrentSchoolId();

        SchoolSubscription subscription = schoolSubscriptionRepository.findActiveBySchoolId(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("Trường không có gói nào đang hoạt động để hủy."));

        if (subscription.getCancelledAt() != null) {
            throw new IllegalArgumentException("Gói đăng ký đã được hủy trước đó.");
        }

        Instant now = Instant.now();
        requireNoQueuedPeriod(schoolId, now, subscription.getId());

        subscription.setCancelledAt(now);
        return schoolSubscriptionRepository.save(subscription).getId();
    }

    // Refuse when a paid period is queued. Marking "will not renew" while a renewal HAS been paid and is waiting to run is
    // a self-contradicting state: a subscription carrying the cancelled flag would still quietly open a new period. The
    // system has no refund flow either, so it cannot cancel that period on the school's behalf.
    private void requireNoQueuedPeriod(UUID schoolId, Instant now, UUID inForceId) {
        SchoolSubscription queued = queuedPeriod(schoolId, now, inForceId);
        if (queued != null) {
            throw new IllegalArgumentException(
                "Trường đã thanh toán cho một kỳ tiếp theo (chạy tới " + DISPLAY_DATE.format(queued.getEndDate())
                    + "), không thể đánh dấu hủy. Vui lòng liên hệ hỗ trợ nếu muốn dừng sớm.");
        }
    }

    /** Kỳ chưa kết thúc mà KHÔNG phải kỳ đang chạy -- tức kỳ đã trả tiền và đang chờ tới ngày. */
    private SchoolSubscription queuedPeriod(UUID schoolId, Instant now, UUID inForceId) {
        return schoolSubscriptionRepository.findUnfinishedBySchoolId(schoolId, now).stream()
            .filter(period -> !period.getId().equals(inForceId))
            .findFirst()
            .orElse(null);
    }
}
