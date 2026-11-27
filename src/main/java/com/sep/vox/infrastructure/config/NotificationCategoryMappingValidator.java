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
 * Blocks, at startup, the kinds of mismatch between {@link EventTypeConstant},
 * {@link NotificationCategory} and {@link NotificationTarget}.
 *
 * <p>Does not check the direction "every eventType must have a category": {@code UserCreated} and
 * {@code RegisterFormRejected} intentionally have none, because the recipient has never logged in so
 * there is no device to push to.
 *
 * <p>Conversely, category and target MUST cover exactly the same set: the category decides which
 * event produces a notification, the target decides where that notification opens.
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

        log.info("Notification mapping: {} eventTypes declared (leading to {} targets), "
            + "{} eventTypes that create no notification",
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
                "NotificationCategory maps an eventType that does not exist in EventTypeConstant: " + unknown);
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
                "eventType has a NotificationCategory but no NotificationTarget: " + missingTarget);
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
                "eventType has a NotificationTarget but no NotificationCategory, it will never produce a "
                    + "notification: " + targetWithoutCategory);
        }
    }
}
