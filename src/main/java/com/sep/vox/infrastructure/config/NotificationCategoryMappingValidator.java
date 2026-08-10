package com.sep.vox.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.notification.NotificationCategory;
import com.sep.vox.infrastructure.exception.InfrastructureException;

/**
 * Chặn từ lúc khởi động hai kiểu lệch giữa {@link EventTypeConstant} và
 * {@link NotificationCategory}.
 *
 * <p>Không kiểm tra chiều "eventType nào cũng phải có category": {@code UserCreated} và
 * {@code RegisterFormRejected} cố tình không có, vì người nhận chưa từng đăng nhập nên
 * không có thiết bị nào để đẩy.
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

            LOGGER.info("Notification category mapping: {} eventType đã khai báo, {} eventType không tạo notification",
                NotificationCategory.mappedEventTypes().size(),
                knownEventTypes.size() - NotificationCategory.mappedEventTypes().size());
        };
    }
}
