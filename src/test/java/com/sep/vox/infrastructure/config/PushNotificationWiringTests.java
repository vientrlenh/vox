package com.sep.vox.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import com.sep.vox.application.port.output.PushNotificationPort;
import com.sep.vox.infrastructure.service.FcmNotificationService;
import com.sep.vox.infrastructure.service.NoOpPushNotificationService;

/**
 * The two adapters of PushNotificationPort are chosen by two complementary @ConditionalOnProperty.
 * If the conditions drift apart, the consequence is one of two silent failures: no bean at all (the app cannot
 * start) or two beans (NoUniqueBeanDefinitionException at injection).
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
                MutablePropertySources source = context.getEnvironment().getPropertySources();
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
        // FcmNotificationService is not built here because it needs a real FirebaseMessaging bean;
        // what must be asserted is that the NoOp version steps back, so there are not two beans at once.
        new ApplicationContextRunner()
            .withUserConfiguration(NoOpPushNotificationService.class)
            .withPropertyValues("firebase.enabled=true")
            .run(context -> assertThat(context).doesNotHaveBean(NoOpPushNotificationService.class));
    }
}
