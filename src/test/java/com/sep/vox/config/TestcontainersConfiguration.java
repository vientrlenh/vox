package com.sep.vox.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Provides the one real Postgres shared by every {@link com.sep.vox.support.RepositoryTest} and
 * {@link com.sep.vox.support.IntegrationTest}. The image is pinned to the same major version as CI and
 * docker-compose.
 *
 * <p>The container is started once in a static initializer and is deliberately not a Spring bean. Spring
 * Boot closes every container bean when its context is destroyed, and cached contexts are destroyed at JVM
 * shutdown, so the first one to close would stop the database under all the others and make their
 * shutdown hang. Testcontainers' Ryuk removes the container when the JVM exits.
 *
 * <p>Because the database is shared, a test that commits data must clean up after itself.
 * {@code SchemaMigrationTests} keeps its own container because it needs an empty database.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    static {
        POSTGRES.start();
    }

    @Bean
    DynamicPropertyRegistrar postgresConnectionProperties() {
        return registry -> {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        };
    }
}
