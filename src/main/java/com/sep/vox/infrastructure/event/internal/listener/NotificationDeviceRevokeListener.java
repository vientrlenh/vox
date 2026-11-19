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
     * {@code REQUIRES_NEW}, NOT the default {@code REQUIRED} -- and this is the condition for the DELETE statement below
     * to run at all, not a choice about transaction scope.
     *
     * <p>When the AFTER_COMMIT callback runs, the {@code EntityManagerHolder} of the transaction that JUST COMMITTED is still
     * bound to the thread -- Spring only removes it in {@code cleanupAfterCompletion}, which is after the
     * callbacks. So {@code REQUIRED} sees "a transaction already exists" and JOINS it instead of opening a
     * new one, while that transaction has already committed and there is no live
     * {@code EntityTransaction}. Hibernate rejects the DELETE statement with
     * {@code TransactionRequiredException: No active transaction for update or delete query}, and
     * {@code TransactionSynchronizationUtils} SWALLOWS that exception (it only logs ERROR) -- so /logout still
     * returns 200 while the device that receives notifications was never removed. A textbook silent failure: on a
     * lab machine, the grade notification of the person who just left still shows to the person who sits down next.
     *
     * <p>The {@code @Transactional} on {@code NotificationDeviceRepositoryImpl#deleteByUserIdAndDeviceId}
     * does NOT save it: that one is {@code REQUIRED} too. The same annotation works fine on
     * {@code deleteByLastSeenAtBefore} only because {@code @Scheduled} calls it from a clean thread with nothing
     * bound to it.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(DeviceSessionRevokedEvent event) {
        int removed = notificationDeviceRepository.deleteByUserIdAndDeviceId(event.userId(), event.deviceId());
        if (removed > 0) {
            LOGGER.info("Đã gỡ {} thiết bị nhận thông báo theo phiên bị thu hồi: sessionId={}, userId={}",
                removed, event.sessionId(), event.userId());
        }
    }
}
