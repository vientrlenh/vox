package com.sep.vox.infrastructure.service;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.PushNotificationPort;
import com.sep.vox.application.response.output.PushDispatchResult;
import com.sep.vox.application.response.output.PushMessage;

/**
 * The stand-in when {@code firebase.enabled} is not on: dev machines without a service account,
 * CI, and every @SpringBootTest.
 *
 * <p>The condition here is the exact complement of FcmNotificationService (havingValue="false"
 * + matchIfMissing=true) rather than @ConditionalOnMissingBean, because the order in which beans are registered
 * during component scanning is not guaranteed -- @ConditionalOnMissingBean between two beans that are both
 * scanned can give different results depending on the traversal order.
 */
@Service
@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class NoOpPushNotificationService implements PushNotificationPort {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public PushDispatchResult send(PushMessage message, List<String> installationIds) {
        log.debug("Push notification disabled (firebase.enabled=false), skipping '{}' for {} device(s)",
            message == null ? null : message.title(),
            installationIds == null ? 0 : installationIds.size());
        return PushDispatchResult.empty();
    }
}
