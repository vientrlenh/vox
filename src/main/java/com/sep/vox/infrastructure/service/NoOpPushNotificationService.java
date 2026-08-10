package com.sep.vox.infrastructure.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.PushNotificationPort;
import com.sep.vox.application.response.output.PushDispatchResult;
import com.sep.vox.application.response.output.PushMessage;

/**
 * Bản thay thế khi {@code firebase.enabled} không bật: máy dev chưa có service account,
 * CI, và mọi @SpringBootTest.
 *
 * <p>Điều kiện ở đây là phần bù chính xác của FcmNotificationService (havingValue="false"
 * + matchIfMissing=true) chứ không dùng @ConditionalOnMissingBean, vì thứ tự đăng ký bean
 * khi component scan không được đảm bảo -- @ConditionalOnMissingBean giữa hai bean cùng
 * được scan có thể cho ra kết quả khác nhau tuỳ thứ tự duyệt.
 */
@Service
@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpPushNotificationService implements PushNotificationPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpPushNotificationService.class);

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public PushDispatchResult send(PushMessage message, List<String> installationIds) {
        LOGGER.debug("Push notification disabled (firebase.enabled=false), skipping '{}' for {} device(s)",
            message == null ? null : message.title(),
            installationIds == null ? 0 : installationIds.size());
        return PushDispatchResult.empty();
    }
}
