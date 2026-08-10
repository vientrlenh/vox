package com.sep.vox.infrastructure.event.internal.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sep.vox.application.event.DeviceSessionRevokedEvent;
import com.sep.vox.domain.repository.NotificationDeviceRepository;

/**
 * Token FCM / FID không đổi khi người dùng đăng xuất -- nó gắn với bản cài đặt app chứ
 * không gắn với tài khoản. Trên máy phòng lab dùng chung (ExamDeliveryMode.LAB), nếu
 * không gỡ ở đây thì giữa lúc học sinh A rời máy và lúc học sinh B đăng nhập, thiết bị
 * vẫn thuộc về A và thông báo điểm thi của A sẽ hiện lên màn hình A đã rời đi.
 *
 * <p>Chạy AFTER_COMMIT: thu hồi phiên là việc bắt buộc phải thành công, còn dọn thiết bị
 * chỉ là hệ quả. Nếu chạy đồng bộ trong cùng transaction, một lỗi ở đây sẽ cuộn ngược
 * việc thu hồi phiên -- đánh đổi sai hoàn toàn về mặt bảo mật.
 */
@Component
public class NotificationDeviceRevokeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationDeviceRevokeListener.class);

    private final NotificationDeviceRepository notificationDeviceRepository;

    public NotificationDeviceRevokeListener(NotificationDeviceRepository notificationDeviceRepository) {
        this.notificationDeviceRepository = notificationDeviceRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(DeviceSessionRevokedEvent event) {
        var removed = notificationDeviceRepository.deleteByUserIdAndDeviceId(event.userId(), event.deviceId());
        if (removed > 0) {
            LOGGER.info("Đã gỡ {} thiết bị nhận thông báo theo phiên bị thu hồi: sessionId={}, userId={}",
                removed, event.sessionId(), event.userId());
        }
    }
}
