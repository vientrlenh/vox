package com.sep.vox.infrastructure.migration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5433/vox-schema-test",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class SchemaMigrationTests {

    @Container
    static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", DB::getJdbcUrl);
        r.add("spring.datasource.username", DB::getUsername);
        r.add("spring.datasource.password", DB::getPassword);
    }

    /**
     * Flyway runs V1..Vn, then Hibernate {@code validate} compares every table and column with the
     * entities. A mismatch anywhere stops the context from loading, so the test goes red. The
     * assertion is that the context starts successfully, which is why an empty body is enough.
     */
    @Test
    void flyway_schema_should_match_entity_mappings() {
    }
}
