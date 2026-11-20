package com.sep.vox.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.TestcontainersConfiguration;

/**
 * A full application context test against the real Postgres from {@link TestcontainersConfiguration}.
 * Tagged "integration", so it runs in {@code integrationTest} and not in {@code test}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("integration")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public @interface IntegrationTest {
}
