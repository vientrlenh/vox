package com.sep.vox.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.notification.NotificationCategory;
import com.sep.vox.domain.model.notification.NotificationTarget;
import com.sep.vox.infrastructure.exception.InfrastructureException;

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
public class NotificationCategoryMappingValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationCategoryMappingValidator.class);

    // Tên bean phải khác tên class đã decapitalize ("notificationCategoryMappingValidator"),
    // nếu không sẽ đụng với chính bean của @Configuration này khi component scan.
    @Bean
    InitializingBean notificationCategoryMappingChecker() {
        return () -> {
            var knownEventTypes = EventTypeConstant.all();

            // Ánh xạ trỏ tới một eventType không còn tồn tại: thường là dấu vết của một lần
            // đổi tên hằng số mà quên sửa bên này. Notification cho loại đó sẽ không bao giờ
            // được tạo, và không có gì báo lỗi nếu không kiểm ở đây.
            var unknown = NotificationCategory.mappedEventTypes().stream()
                .filter(eventType -> !knownEventTypes.contains(eventType))
                .sorted()
                .toList();

            if (!unknown.isEmpty()) {
                throw new InfrastructureException(
                    "NotificationCategory ánh xạ eventType không tồn tại trong EventTypeConstant: " + unknown);
            }

            // Thiếu target thì renderDrafts ném IllegalArgumentException giữa chừng. Vì nó
            // nằm trong nhánh Kafka, message sẽ chạy hết vòng retry rồi rơi vào DLT: thông
            // báo không bao giờ tới người nhận, và chỗ duy nhất nói ra điều đó là log lỗi.
            var missingTarget = NotificationCategory.mappedEventTypes().stream()
                .filter(eventType -> !NotificationTarget.isMapped(eventType))
                .sorted()
                .toList();

            if (!missingTarget.isEmpty()) {
                throw new InfrastructureException(
                    "eventType có NotificationCategory nhưng thiếu NotificationTarget: " + missingTarget);
            }

            // Chiều ngược lại vô hại lúc chạy nhưng luôn là dấu hiệu của một nửa việc: ai đó
            // khai báo target cho event mới rồi quên category, và event đó không hề sinh
            // notification -- im lặng đúng như lúc chưa làm gì cả.
            var targetWithoutCategory = NotificationTarget.mappedEventTypes().stream()
                .filter(eventType -> !NotificationCategory.isMapped(eventType))
                .sorted()
                .toList();

            if (!targetWithoutCategory.isEmpty()) {
                throw new InfrastructureException(
                    "eventType có NotificationTarget nhưng thiếu NotificationCategory, sẽ không bao giờ sinh "
                        + "notification: " + targetWithoutCategory);
            }

            LOGGER.info("Notification mapping: {} eventType đã khai báo (dẫn về {} target), "
                + "{} eventType không tạo notification",
                NotificationCategory.mappedEventTypes().size(),
                NotificationTarget.mappedEventTypes().stream().map(NotificationTarget::of).distinct().count(),
                knownEventTypes.size() - NotificationCategory.mappedEventTypes().size());
        };
    }
}
