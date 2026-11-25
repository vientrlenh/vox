package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.sep.vox.domain.repository.DimensionInterestScoreRepository;

/**
 * Needs its own container with Flyway on: the native upsert leaves {@code id} to the table default, and the
 * create-drop schema of a {@code @RepositoryTest} has no column default.
 */
@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class DimensionInterestScoreUpsertTests {

    @Container
    static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DB::getJdbcUrl);
        registry.add("spring.datasource.username", DB::getUsername);
        registry.add("spring.datasource.password", DB::getPassword);
    }

    @Autowired
    private DimensionInterestScoreRepository scoreRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void should_insert_a_version_7_id_then_update_the_score_and_keep_the_id() {
        UUID profileId = UUID.randomUUID();

        scoreRepository.upsertScore(profileId, "SCIENCE", 0.2);
        UUID firstId = storedId(profileId);
        scoreRepository.upsertScore(profileId, "SCIENCE", 0.9);

        assertThat(firstId.version()).isEqualTo(7);
        assertThat(storedId(profileId)).isEqualTo(firstId);
        assertThat(storedScore(profileId)).isEqualByComparingTo(new BigDecimal("0.9"));
    }

    private UUID storedId(UUID profileId) {
        return jdbcTemplate.queryForObject(
            "SELECT id FROM dimension_interest_scores WHERE learner_profile_id = ?", UUID.class, profileId);
    }

    private BigDecimal storedScore(UUID profileId) {
        return jdbcTemplate.queryForObject(
            "SELECT score FROM dimension_interest_scores WHERE learner_profile_id = ?", BigDecimal.class, profileId);
    }
}
