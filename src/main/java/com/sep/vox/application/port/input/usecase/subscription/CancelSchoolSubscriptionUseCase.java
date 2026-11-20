package com.sep.vox.application.port.input.usecase.subscription;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
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
public class CancelSchoolSubscriptionUseCase implements IUseCase<Void, UUID> {

    private static final DateTimeFormatter DISPLAY_DATE =
        DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneConstant.BUSINESS_ZONE);

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final UserContextPort userContextPort;

    public CancelSchoolSubscriptionUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(Void input) {
        // KHÔNG nhận subscriptionId: trường chỉ có đúng một kỳ đang hiệu lực, và nhận id từ ngoài thì
        // hasRole('SCHOOL_ADMIN') không trả lời được "kỳ này có thuộc trường đó không" -- lại phải
        // dựng thêm một lớp kiểm quyền sở hữu. Không nhận id thì không có gì để kiểm.
        var schoolId = userContextPort.getCurrentSchoolId();

        var subscription = schoolSubscriptionRepository.findActiveBySchoolId(schoolId)
            .orElseThrow(() -> new NotFoundException("Trường không có gói nào đang hoạt động để hủy."));

        if (subscription.getCancelledAt() != null) {
            throw new IllegalStateException("Gói đăng ký đã được hủy trước đó.");
        }

        var now = Instant.now();

        // Có kỳ đã trả tiền đang xếp hàng chờ thì từ chối. Đánh dấu "sẽ không gia hạn" trong khi một
        // lần gia hạn ĐÃ được trả tiền và đang chờ chạy là một trạng thái tự mâu thuẫn: gói mang cờ
        // đã hủy rồi vẫn lặng lẽ mở ra một kỳ mới. Hệ thống chưa có luồng hoàn tiền nên cũng không
        // thể tự hủy kỳ đó thay trường.
        var queued = queuedPeriod(schoolId, now, subscription.getId());
        if (queued != null) {
            throw new IllegalStateException(
                "Trường đã thanh toán cho một kỳ tiếp theo (chạy tới " + DISPLAY_DATE.format(queued.getEndDate())
                    + "), không thể đánh dấu hủy. Vui lòng liên hệ hỗ trợ nếu muốn dừng sớm.");
        }

        subscription.setCancelledAt(now);
        return schoolSubscriptionRepository.save(subscription).getId();
    }

    /** Kỳ chưa kết thúc mà KHÔNG phải kỳ đang chạy -- tức kỳ đã trả tiền và đang chờ tới ngày. */
    private SchoolSubscription queuedPeriod(UUID schoolId, Instant now, UUID inForceId) {
        return schoolSubscriptionRepository.findUnfinishedBySchoolId(schoolId, now).stream()
            .filter(period -> !period.getId().equals(inForceId))
            .findFirst()
            .orElse(null);
    }
}
