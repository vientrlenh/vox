package com.sep.vox.infrastructure.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Proves V14__rename_exam_to_assessment.sql on a real Postgres: the schema is migrated to V13, rows that carry
 * the old values are written, V14 is applied, and the same rows are read back with the new values.
 *
 * <p>Foreign keys are skipped on the seeding connection ({@code session_replication_role = replica}) so a row can
 * be written without building its whole parent chain; CHECK and NOT NULL constraints still apply.
 */
@Testcontainers
@Tag("integration")
class RenameExamToAssessmentMigrationTests {

    private static final String PRE_RENAME_VERSION = "13";
    private static final String RENAME_VERSION = "14";
    private static final String NOTIFICATION_USER_ID = "'00000000-0000-0000-0000-000000000001'";
    private static final String UNRELATED_NOTIFICATION_USER_ID = "'00000000-0000-0000-0000-000000000002'";
    private static final String NEW_UUID = "gen_random_uuid()";
    private static final Map<String, String> VALID_QUESTION_COLUMNS = Map.of(
        "type", "'READ_ALOUD'", "status", "'DRAFT'");
    private static final Map<String, String> ADJUSTMENT_BALANCE_ENTRY = Map.of(
        "entry_type", "'ADJUSTMENT'", "actor_id", NEW_UUID, "reason", "'x'");

    @Container
    static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18");

    private static JdbcTemplate jdbc;

    private record StoredValue(String oldTable, String newTable, String column, String oldValue, String newValue,
                               boolean hasCheckConstraint, Map<String, String> oldExtras,
                               Map<String, String> newExtras) {

        static StoredValue checked(String oldTable, String newTable, String column, String oldValue,
                                   String newValue) {
            return new StoredValue(oldTable, newTable, column, oldValue, newValue, true, Map.of(), Map.of());
        }

        static StoredValue unchecked(String table, String column, String oldValue, String newValue) {
            return new StoredValue(table, table, column, oldValue, newValue, false, Map.of(), Map.of());
        }

        StoredValue withExtras(Map<String, String> oldColumns, Map<String, String> newColumns) {
            return new StoredValue(oldTable, newTable, column, oldValue, newValue, hasCheckConstraint,
                oldColumns, newColumns);
        }

        Map<String, String> oldRow(String value) {
            return rowWith(column, value, oldExtras);
        }

        Map<String, String> newRow(String value) {
            return rowWith(column, value, newExtras);
        }
    }

    private static Map<String, String> rowWith(String column, String value, Map<String, String> extras) {
        Map<String, String> row = new LinkedHashMap<>(extras);
        row.put(column, "'" + value + "'");
        return row;
    }

    static Stream<StoredValue> storedValues() {
        return Stream.of(
            Stream.of(
                StoredValue.checked("exam_result_status_histories", "assessment_result_status_histories", "source",
                    "EXAM_PUBLISH", "ASSESSMENT_PUBLISH"),
                StoredValue.checked("questions", "questions", "confidentiality",
                    "EXAM_RESTRICTED", "ASSESSMENT_RESTRICTED")
                    .withExtras(VALID_QUESTION_COLUMNS, VALID_QUESTION_COLUMNS)),
            notificationCategoryValues(),
            quotaTypeValues(),
            personalizationValues()
        ).flatMap(values -> values);
    }

    private static Stream<StoredValue> notificationCategoryValues() {
        return Stream.of("RESULT", "APPEAL", "SCHEDULE", "BLUEPRINT").map(category -> StoredValue.checked(
            "notification_preferences", "notification_preferences", "category",
            "EXAM_" + category, "ASSESSMENT_" + category));
    }

    private static Stream<StoredValue> quotaTypeValues() {
        Map<String, String> oldSpendSource = Map.of("exam_session_id", NEW_UUID, "amount_vnd", "1");
        Map<String, String> newSpendSource = Map.of("assessment_session_id", NEW_UUID, "amount_vnd", "1");
        return Stream.of(
            StoredValue.checked("school_balance_entries", "school_balance_entries", "quota_type", "EXAM", "ASSESSMENT")
                .withExtras(ADJUSTMENT_BALANCE_ENTRY, ADJUSTMENT_BALANCE_ENTRY),
            StoredValue.checked("subscription_plan_quotas", "subscription_plan_quotas", "quota_type", "EXAM",
                "ASSESSMENT"),
            StoredValue.checked("school_subscription_quota_records", "school_subscription_quota_records",
                "quota_type", "EXAM", "ASSESSMENT"),
            StoredValue.checked("school_subscription_quota_user_allocations",
                "school_subscription_quota_user_allocations", "quota_type", "EXAM", "ASSESSMENT"),
            StoredValue.checked("token_usage_events", "token_usage_events", "quota_type", "EXAM", "ASSESSMENT"),
            StoredValue.checked("school_debt_events", "school_debt_events", "quota_type", "EXAM", "ASSESSMENT")
                .withExtras(lockedDebtEvent("trigger_exam_session_id"),
                    lockedDebtEvent("trigger_assessment_session_id")),
            StoredValue.checked("school_quota_policies", "school_quota_policies", "quota_type", "EXAM",
                "ASSESSMENT"),
            StoredValue.checked("school_ai_spend_entries", "school_ai_spend_entries", "quota_type", "EXAM",
                "ASSESSMENT").withExtras(oldSpendSource, newSpendSource));
    }

    private static Map<String, String> lockedDebtEvent(String triggerSessionColumn) {
        return Map.of(
            "event_type", "'LOCKED'",
            "total_allocated_vnd", "0",
            "used_amount_vnd", "0",
            triggerSessionColumn, NEW_UUID);
    }

    private static Stream<StoredValue> personalizationValues() {
        return Stream.of(
            StoredValue.unchecked("quota_pricing_calibrations", "pricing_source", "EXAM", "ASSESSMENT"),
            StoredValue.unchecked("interest_dimensions", "code", "ACADEMIC_EXAM", "ACADEMIC_ASSESSMENT"),
            StoredValue.unchecked("dimension_interest_scores", "dimension", "ACADEMIC_EXAM", "ACADEMIC_ASSESSMENT"),
            StoredValue.unchecked("practice_topics", "interest_dimensions", "ACADEMIC_EXAM", "ACADEMIC_ASSESSMENT"),
            StoredValue.unchecked("topic_suggestions", "interest_dimensions", "ACADEMIC_EXAM",
                "ACADEMIC_ASSESSMENT"),
            StoredValue.unchecked("practice_topics", "curriculum_group", "EXAM_BANK", "ASSESSMENT_BANK"),
            StoredValue.unchecked("topic_suggestions", "curriculum_group", "EXAM_BANK", "ASSESSMENT_BANK"),
            StoredValue.unchecked("practice_topics", "source", "EXAM_QUESTION_BANK", "ASSESSMENT_QUESTION_BANK"),
            StoredValue.unchecked("learner_profiles", "goal_type", "EXAM_PREP", "ASSESSMENT_PREP"),
            StoredValue.unchecked("practice_papers", "goal_type", "EXAM_PREP", "ASSESSMENT_PREP"),
            StoredValue.unchecked("topic_suggestions", "reason_text", "OUT_OF_EXAM_SCOPE", "OUT_OF_ASSESSMENT_SCOPE"));
    }

    static Stream<StoredValue> checkedValues() {
        return storedValues().filter(value -> value.hasCheckConstraint);
    }

    @BeforeAll
    static void migrateWithSeededHistory() {
        migrateTo(PRE_RENAME_VERSION);
        jdbc = new JdbcTemplate(new SingleConnectionDataSource(
            DB.getJdbcUrl(), DB.getUsername(), DB.getPassword(), true));
        jdbc.execute("set session_replication_role = replica");
        storedValues().forEach(value -> insertRow(value.oldTable(), value.oldRow(value.oldValue())));
        seedNotifications();
        migrateTo(RENAME_VERSION);
    }

    private static void migrateTo(String version) {
        Flyway.configure()
            .dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword())
            .locations("classpath:db/migration")
            .target(version)
            .load()
            .migrate();
    }

    private static void seedNotifications() {
        insertRow("notifications", Map.of(
            "user_id", NOTIFICATION_USER_ID,
            "event_type", "'ExamResultReleased'",
            "payload", "'{\"eventType\":\"ExamResultReleased\",\"target\":\"EXAM_RESULT_DETAIL\","
                + "\"examId\":\"e1\",\"examKind\":\"CLASS_TEST\",\"examName\":\"Exam EXAM_1 examId\","
                + "\"sessionId\":\"s1\"}'::jsonb"));
        insertRow("notifications", Map.of(
            "user_id", UNRELATED_NOTIFICATION_USER_ID,
            "event_type", "'InvoicePaid'",
            "payload", "'{\"invoiceNumber\":\"EXAM-1\",\"note\":\"Exam text\"}'::jsonb"));
    }

    private static void insertRow(String table, Map<String, String> explicitExpressions) {
        Map<String, String> columns = new LinkedHashMap<>(explicitExpressions);
        jdbc.queryForList("""
            select column_name, data_type from information_schema.columns
            where table_schema = 'public' and table_name = ? and is_nullable = 'NO' and column_default is null
            order by ordinal_position""", table)
            .forEach(row -> columns.putIfAbsent(
                (String) row.get("column_name"), placeholderExpression((String) row.get("data_type"))));
        jdbc.execute("insert into " + table + " (" + String.join(", ", columns.keySet()) + ") values ("
            + String.join(", ", columns.values()) + ")");
    }

    private static String placeholderExpression(String dataType) {
        return switch (dataType) {
            case "uuid" -> NEW_UUID;
            case "character varying", "text" -> "'x' || substr(md5(random()::text), 1, 6)";
            case "numeric", "integer", "bigint", "smallint", "double precision" -> "0";
            case "boolean" -> "false";
            case "timestamp with time zone", "timestamp without time zone" -> "now()";
            case "date" -> "current_date";
            case "jsonb", "json" -> "'{}'";
            default -> throw new IllegalStateException("No placeholder for column type " + dataType);
        };
    }

    private static int countRows(String table, String column, String value) {
        Integer count = jdbc.queryForObject(
            "select count(*) from " + table + " where " + column + " = ?", Integer.class, value);
        return count == null ? 0 : count;
    }

    @ParameterizedTest
    @MethodSource("storedValues")
    void should_rewrite_the_stored_value_of_existing_rows(StoredValue value) {
        assertThat(countRows(value.newTable(), value.column(), value.oldValue())).isZero();
        assertThat(countRows(value.newTable(), value.column(), value.newValue())).isPositive();
    }

    @ParameterizedTest
    @MethodSource("checkedValues")
    void should_accept_the_new_value_and_reject_the_old_value_in_new_rows(StoredValue value) {
        insertRow(value.newTable(), value.newRow(value.newValue()));

        assertThatThrownBy(() -> insertRow(value.newTable(), value.newRow(value.oldValue())))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_leave_no_database_object_named_after_exam() {
        List<String> remaining = jdbc.queryForList("""
            select 'relation ' || relname from pg_class
              where relnamespace = 'public'::regnamespace and relname ~ 'exam(?!ple)'
            union all
            select 'column ' || table_name || '.' || column_name from information_schema.columns
              where table_schema = 'public' and column_name ~ 'exam(?!ple)'
            union all
            select 'constraint ' || conname from pg_constraint
              where connamespace = 'public'::regnamespace and conname ~ 'exam(?!ple)'
            """, String.class);

        assertThat(remaining).isEmpty();
    }

    @Test
    void should_rename_the_event_type_and_the_three_payload_keys_of_notification_history() {
        Map<String, Object> notification = jdbc.queryForMap("""
            select event_type,
                   payload ->> 'eventType' as payload_event_type,
                   payload ->> 'target' as target,
                   payload ->> 'assessmentId' as assessment_id,
                   payload ->> 'assessmentKind' as assessment_kind,
                   payload ->> 'assessmentName' as assessment_name,
                   payload ->> 'sessionId' as session_id,
                   jsonb_exists(payload, 'examId') as has_old_key
            from notifications where user_id = """ + NOTIFICATION_USER_ID + "::uuid");

        assertThat(notification)
            .containsEntry("event_type", "AssessmentResultReleased")
            .containsEntry("payload_event_type", "AssessmentResultReleased")
            .containsEntry("target", "ASSESSMENT_RESULT_DETAIL")
            .containsEntry("assessment_id", "e1")
            .containsEntry("assessment_kind", "CLASS_TEST")
            .containsEntry("session_id", "s1")
            .containsEntry("has_old_key", false);
    }

    @Test
    void should_not_touch_user_text_inside_notification_payloads() {
        String assessmentName = jdbc.queryForObject(
            "select payload ->> 'assessmentName' from notifications where user_id = "
                + NOTIFICATION_USER_ID + "::uuid", String.class);
        String unrelatedPayload = jdbc.queryForObject(
            "select payload::text from notifications where user_id = "
                + UNRELATED_NOTIFICATION_USER_ID + "::uuid", String.class);

        assertThat(assessmentName).isEqualTo("Exam EXAM_1 examId");
        assertThat(unrelatedPayload).contains("EXAM-1").contains("Exam text");
    }

    @Test
    void should_default_the_pricing_source_of_new_calibrations_to_assessment() {
        String columnDefault = jdbc.queryForObject("""
            select column_default from information_schema.columns
            where table_name = 'quota_pricing_calibrations' and column_name = 'pricing_source'""", String.class);

        assertThat(columnDefault).contains("ASSESSMENT");
    }
}
