package com.sep.vox.infrastructure.event.internal.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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

    /**
     * {@code REQUIRES_NEW}, KHÔNG phải {@code REQUIRED} mặc định -- và đây là điều kiện để câu
     * DELETE bên dưới chạy được, không phải một lựa chọn về phạm vi giao dịch.
     *
     * <p>Lúc callback AFTER_COMMIT chạy, {@code EntityManagerHolder} của giao dịch VỪA COMMIT vẫn
     * còn gắn vào thread -- Spring chỉ gỡ nó ở {@code cleanupAfterCompletion}, tức là sau các
     * callback. Nên {@code REQUIRED} nhìn thấy "đã có giao dịch" và THAM GIA vào nó thay vì mở
     * giao dịch mới, trong khi giao dịch đó đã commit xong và không còn
     * {@code EntityTransaction} nào sống. Hibernate từ chối câu DELETE với
     * {@code TransactionRequiredException: No active transaction for update or delete query}, và
     * {@code TransactionSynchronizationUtils} NUỐT ngoại lệ đó (chỉ log ERROR) -- nên /logout vẫn
     * trả 200 trong khi thiết bị nhận thông báo không hề bị gỡ. Đúng kiểu hỏng im lặng: trên máy
     * phòng lab, thông báo điểm của người vừa rời đi vẫn hiện cho người ngồi xuống sau.
     *
     * <p>{@code @Transactional} trên {@code NotificationDeviceRepositoryImpl#deleteByUserIdAndDeviceId}
     * KHÔNG cứu được: nó cũng là {@code REQUIRED}. Cùng annotation đó chạy tốt ở
     * {@code deleteByLastSeenAtBefore} chỉ vì {@code @Scheduled} gọi từ một thread sạch, không có
     * gì gắn sẵn.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(DeviceSessionRevokedEvent event) {
        var removed = notificationDeviceRepository.deleteByUserIdAndDeviceId(event.userId(), event.deviceId());
        if (removed > 0) {
            LOGGER.info("Đã gỡ {} thiết bị nhận thông báo theo phiên bị thu hồi: sessionId={}, userId={}",
                removed, event.sessionId(), event.userId());
        }
    }
}
