package com.sep.vox.infrastructure.event.internal.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sep.vox.application.event.DeviceSessionRevokedEvent;
import com.sep.vox.domain.repository.NotificationDeviceRepository;

/**
 * The FCM / FID token does not change when the user logs out -- it is tied to the app installation and
 * not to the account. On a shared lab machine (AssessmentDeliveryMode.LAB), if it is not removed here then
 * between the moment student A leaves the machine and student B logs in, the device
 * still belongs to A and A's assessment score notification will show up on the screen A has left.
 *
 * <p>Runs AFTER_COMMIT: revoking the session is something that must succeed, while cleaning up the device
 * is only a consequence. If it ran synchronously in the same transaction, an error here would roll back
 * the session revocation -- a completely wrong trade-off in terms of security.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDeviceRevokeListener {

    private final NotificationDeviceRepository notificationDeviceRepository;

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
            log.info("Đã gỡ {} thiết bị nhận thông báo theo phiên bị thu hồi: sessionId={}, userId={}",
                removed, event.sessionId(), event.userId());
        }
    }
}
