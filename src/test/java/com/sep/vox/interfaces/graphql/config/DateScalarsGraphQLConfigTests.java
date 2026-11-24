package com.sep.vox.interfaces.graphql.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;

class DateScalarsGraphQLConfigTests {

    private static final String SCHEMA = """
        scalar DateTime
        scalar Date
        type Query {
            echoDateTime(value: DateTime!): DateTime!
            echoDate(value: Date!): Date!
            fixedDateTime: DateTime!
            fixedDate: Date!
        }
        """;

    private GraphQL graphQl;

    @BeforeEach
    void buildGraphQlWithTheConfiguredScalars() {
        RuntimeWiring.Builder wiring = RuntimeWiring.newRuntimeWiring();
        new DateScalarsGraphQLConfig().dateScalarsRuntimeWiringConfigurer().configure(wiring);
        wiring.type("Query", query -> query
            .dataFetcher("echoDateTime", environment -> environment.getArgument("value"))
            .dataFetcher("echoDate", environment -> environment.getArgument("value"))
            .dataFetcher("fixedDateTime", environment -> Instant.parse("2026-07-28T02:30:00Z"))
            .dataFetcher("fixedDate", environment -> LocalDate.of(2026, 7, 28)));
        GraphQLSchema schema = new SchemaGenerator()
            .makeExecutableSchema(new SchemaParser().parse(SCHEMA), wiring.build());
        graphQl = GraphQL.newGraphQL(schema).build();
    }

    @Test
    void dateTimeIsSerializedAsIsoInstantInUtc() {
        ExecutionResult result = graphQl.execute("{ fixedDateTime }");

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.<Map<String, Object>>getData()).containsEntry("fixedDateTime", "2026-07-28T02:30:00Z");
    }

    @Test
    void dateTimeWithPositiveOffsetIsNormalizedToUtc() {
        ExecutionResult result = executeEchoDateTime("2026-07-28T09:30:00+07:00");

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.<Map<String, Object>>getData()).containsEntry("echoDateTime", "2026-07-28T02:30:00Z");
    }

    @Test
    void dateTimeWithZuluOffsetIsAccepted() {
        ExecutionResult result = executeEchoDateTime("2026-07-28T02:30:00Z");

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.<Map<String, Object>>getData()).containsEntry("echoDateTime", "2026-07-28T02:30:00Z");
    }

    @ParameterizedTest
    @ValueSource(strings = { "2026-07-28", "2026-07-28T09:30:00", "28/07/2026", "not a date", "" })
    void dateTimeWithoutOffsetOrWithWrongFormatIsRejectedAsCoercionError(String value) {
        ExecutionResult result = executeEchoDateTime(value);

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().getFirst().getMessage()).contains("DateTime");
    }

    @Test
    void dateTimeLiteralWithoutOffsetIsRejectedAsCoercionError() {
        ExecutionResult result = graphQl.execute("{ echoDateTime(value: \"2026-07-28\") }");

        assertThat(result.getErrors()).hasSize(1);
    }

    @Test
    void dateTimeRejectsNonStringVariable() {
        ExecutionResult result = executeWithVariable("query($v: DateTime!) { echoDateTime(value: $v) }", 1785205800);

        assertThat(result.getErrors()).hasSize(1);
    }

    @Test
    void dateIsSerializedAsIsoLocalDate() {
        ExecutionResult result = graphQl.execute("{ fixedDate }");

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.<Map<String, Object>>getData()).containsEntry("fixedDate", "2026-07-28");
    }

    @Test
    void dateIsAcceptedInIsoFormat() {
        ExecutionResult result = executeWithVariable("query($v: Date!) { echoDate(value: $v) }", "2026-07-28");

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.<Map<String, Object>>getData()).containsEntry("echoDate", "2026-07-28");
    }

    @ParameterizedTest
    @ValueSource(strings = { "28/07/2026", "28-07-2026", "2026/07/28", "2026-07-28T00:00:00Z" })
    void dateWithAnyOtherFormatIsRejectedAsCoercionError(String value) {
        ExecutionResult result = executeWithVariable("query($v: Date!) { echoDate(value: $v) }", value);

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().getFirst().getMessage()).contains("Variable 'v' has an invalid value");
    }

    @Test
    void bothDateScalarsAreRegisteredAndTheLibraryDateTimeIsNot() {
        RuntimeWiring.Builder wiring = RuntimeWiring.newRuntimeWiring();
        new DateScalarsGraphQLConfig().dateScalarsRuntimeWiringConfigurer().configure(wiring);

        assertThat(wiring.build().getScalars()).containsKeys("DateTime", "Date");
        assertThat(wiring.build().getScalars().get("DateTime").getCoercing().getClass().getName())
            .startsWith(DateScalarsGraphQLConfig.class.getName());
    }

    private ExecutionResult executeEchoDateTime(String value) {
        return executeWithVariable("query($v: DateTime!) { echoDateTime(value: $v) }", value);
    }

    private ExecutionResult executeWithVariable(String query, Object value) {
        ExecutionInput input = ExecutionInput.newExecutionInput()
            .query(query)
            .variables(Map.of("v", value))
            .build();
        return graphQl.execute(input);
    }
}
