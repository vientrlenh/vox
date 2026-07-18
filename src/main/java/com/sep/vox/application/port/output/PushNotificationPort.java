package com.sep.vox.application.port.output;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PushNotificationPort {
    void sendToUser(UUID userId, String title, String body, Map<String, String> data);
    void sendToUsers(List<UUID> userIds, String title, String body, Map<String, String> data);
}
