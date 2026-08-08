package com.sep.vox.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;

import com.sep.vox.application.port.output.PushNotificationPort;
import com.sep.vox.infrastructure.service.FcmNotificationService;
import com.sep.vox.infrastructure.service.NoOpPushNotificationService;

/**
 * Hai adapter của PushNotificationPort được chọn bằng hai @ConditionalOnProperty bù nhau.
 * Nếu điều kiện lệch, hậu quả là một trong hai lỗi im lặng: không có bean nào (app không
 * khởi động được) hoặc có hai bean (NoUniqueBeanDefinitionException lúc inject).
 */
class PushNotificationWiringTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
        .withUserConfiguration(FcmNotificationService.class, NoOpPushNotificationService.class);

    @Test
    void usesNoOpAdapterWhenFirebaseIsDisabled() {
        contextRunner
            .withPropertyValues("firebase.enabled=false")
            .run(context -> {
                assertThat(context).hasSingleBean(PushNotificationPort.class);
                assertThat(context).hasSingleBean(NoOpPushNotificationService.class);
                assertThat(context).doesNotHaveBean(FcmNotificationService.class);
                assertThat(context.getBean(PushNotificationPort.class).isEnabled()).isFalse();
            });
    }

    @Test
    void usesNoOpAdapterWhenFirebasePropertyIsAbsent() {
        contextRunner
            .withInitializer(context -> {
                var source = context.getEnvironment().getPropertySources();
                source.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                source.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
            })
            .run(context -> {
                assertThat(context).hasSingleBean(PushNotificationPort.class);
                assertThat(context).hasSingleBean(NoOpPushNotificationService.class);
            });
    }

    @Test
    void doesNotRegisterNoOpAdapterWhenFirebaseIsEnabled() {
        // Không dựng FcmNotificationService ở đây vì nó cần bean FirebaseMessaging thật;
        // điều cần khẳng định là bản NoOp phải rút lui, để không có hai bean cùng lúc.
        new ApplicationContextRunner()
            .withUserConfiguration(NoOpPushNotificationService.class)
            .withPropertyValues("firebase.enabled=true")
            .run(context -> assertThat(context).doesNotHaveBean(NoOpPushNotificationService.class));
    }
}
