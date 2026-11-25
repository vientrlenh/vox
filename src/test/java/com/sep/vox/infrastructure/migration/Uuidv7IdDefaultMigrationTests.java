package com.sep.vox.infrastructure.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Proves V23__uuidv7_default_for_entity_ids.sql on a real Postgres: after every migration has run, the id column
 * of each table whose entity gets its id from Hibernate also has {@code DEFAULT uuidv7()} as a safety net.
 */
@Testcontainers
@Tag("integration")
class Uuidv7IdDefaultMigrationTests {

    private static final String UUIDV7_DEFAULT = "uuidv7()";
    private static final List<String> TABLES_WITH_HIBERNATE_GENERATED_IDS = List.of(
        "assessment_item_evaluation_turns", "assessment_item_responses", "assessment_item_response_turns",
        "practice_criterion_scores", "practice_item_evaluations", "practice_item_responses",
        "practice_paper_items", "practice_papers", "practice_response_turns", "practice_sessions",
        "saved_topics", "topic_interest_events", "topic_interest_scores", "topic_suggestions",
        "turn_corrections", "student_question_exposures");

    @Container
    static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18");

    private static JdbcTemplate jdbc;

    @BeforeAll
    static void migrateEverything() {
        Flyway.configure()
            .dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword())
            .locations("classpath:db/migration")
            .load()
            .migrate();
        jdbc = new JdbcTemplate(new SingleConnectionDataSource(
            DB.getJdbcUrl(), DB.getUsername(), DB.getPassword(), true));
    }

    @Test
    void everyTableWithAHibernateGeneratedIdDefaultsToUuidV7() {
        List<String> tablesWithoutDefault = TABLES_WITH_HIBERNATE_GENERATED_IDS.stream()
            .filter(table -> !UUIDV7_DEFAULT.equals(idColumnDefault(table)))
            .toList();

        assertThat(tablesWithoutDefault).isEmpty();
    }

    private static String idColumnDefault(String table) {
        List<String> defaults = jdbc.queryForList("""
            SELECT column_default FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = ? AND column_name = 'id'
            """, String.class, table);
        return defaults.isEmpty() ? null : defaults.getFirst();
    }
}
