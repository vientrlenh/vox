package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.notification.NotificationCategory;
import com.sep.vox.domain.model.notification.NotificationPreference;

public interface NotificationPreferenceRepository {
    Optional<NotificationPreference> findById(UUID id);
    NotificationPreference save(NotificationPreference preference);

    /**
     * @return rỗng khi người dùng chưa từng đổi thiết lập cho nhóm này -- phía gọi phải
     *         hiểu đó là "dùng mặc định", xem NotificationPreference#DEFAULT_PUSH_ENABLED
     */
    Optional<NotificationPreference> findByUserIdAndCategory(UUID userId, NotificationCategory category);
}
