package com.sep.vox.infrastructure.config;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.notification.NotificationCategory;
import com.sep.vox.domain.model.notification.NotificationTarget;

/**
 * Chặn từ lúc khởi động những kiểu lệch giữa {@link EventTypeConstant},
 * {@link NotificationCategory} và {@link NotificationTarget}.
 *
 * <p>Không kiểm tra chiều "eventType nào cũng phải có category": {@code UserCreated} và
 * {@code RegisterFormRejected} cố tình không có, vì người nhận chưa từng đăng nhập nên
 * không có thiết bị nào để đẩy.
 *
 * <p>Ngược lại, category và target thì PHẢI phủ đúng cùng một tập: category quyết định
 * event nào sinh notification, target quyết định notification đó mở đi đâu.
 */
@Configuration
@Slf4j
public class NotificationCategoryMappingValidator {

    // The bean name must differ from the decapitalized class name ("notificationCategoryMappingValidator"),
    // otherwise it collides with the bean of this @Configuration itself during component scan.
    @Bean
    InitializingBean notificationCategoryMappingChecker() {
        return this::checkNotificationMappings;
    }

    private void checkNotificationMappings() {
        List<String> knownEventTypes = EventTypeConstant.all();
        requireKnownEventTypes(knownEventTypes);
        requireTargetForEveryCategory();
        requireCategoryForEveryTarget();

        log.info("Notification mapping: {} eventType đã khai báo (dẫn về {} target), "
            + "{} eventType không tạo notification",
            NotificationCategory.mappedEventTypes().size(),
            NotificationTarget.mappedEventTypes().stream().map(NotificationTarget::of).distinct().count(),
            knownEventTypes.size() - NotificationCategory.mappedEventTypes().size());
    }

    // The mapping points at an eventType that no longer exists: usually the trace of a
    // constant rename where this side was forgotten. A notification of that kind will never
    // be created, and nothing reports an error unless it is checked here.
    private void requireKnownEventTypes(List<String> knownEventTypes) {
        List<String> unknown = NotificationCategory.mappedEventTypes().stream()
            .filter(eventType -> !knownEventTypes.contains(eventType))
            .sorted()
            .toList();

        if (!unknown.isEmpty()) {
            throw new IllegalStateException(
                "NotificationCategory ánh xạ eventType không tồn tại trong EventTypeConstant: " + unknown);
        }
    }

    // Without a target renderDrafts throws IllegalArgumentException midway. Since it
    // sits on the Kafka branch, the message runs the whole retry cycle and falls into the DLT: the
    // notification never reaches the recipient, and the only place that says so is the error log.
    private void requireTargetForEveryCategory() {
        List<String> missingTarget = NotificationCategory.mappedEventTypes().stream()
            .filter(eventType -> !NotificationTarget.isMapped(eventType))
            .sorted()
            .toList();

        if (!missingTarget.isEmpty()) {
            throw new IllegalStateException(
                "eventType có NotificationCategory nhưng thiếu NotificationTarget: " + missingTarget);
        }
    }

    // The opposite direction is harmless at runtime but is always a sign of half-finished work: someone
    // declared a target for a new event and forgot the category, and that event produces no
    // notification at all -- silent, exactly as if nothing had been done.
    private void requireCategoryForEveryTarget() {
        List<String> targetWithoutCategory = NotificationTarget.mappedEventTypes().stream()
            .filter(eventType -> !NotificationCategory.isMapped(eventType))
            .sorted()
            .toList();

        if (!targetWithoutCategory.isEmpty()) {
            throw new IllegalStateException(
                "eventType có NotificationTarget nhưng thiếu NotificationCategory, sẽ không bao giờ sinh "
                    + "notification: " + targetWithoutCategory);
        }
    }
}
