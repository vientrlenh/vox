package com.sep.vox.interfaces.graphql.config;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.scalars.ExtendedScalars;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

/**
 * Registers the {@code DateTime} scalar (Java {@link Instant}) and the {@code Date} scalar (Java
 * {@link java.time.LocalDate}, from graphql-java-extended-scalars).
 *
 * <p>{@code DateTime} output is ISO-8601 in UTC (for example {@code 2026-07-28T02:30:00Z}). Input is
 * strict: it must be ISO-8601 with an offset ({@code Z} or {@code +07:00}). Date-only and offset-less
 * values are rejected with a coercion error and never reach {@code DateMapper}'s compatibility branch.
 */
@Configuration
public class DateScalarsGraphQLConfig {

    private static final GraphQLScalarType DATE_TIME = GraphQLScalarType.newScalar()
        .name("DateTime")
        .description("ISO-8601 timestamp with an offset, for example 2026-07-28T09:30:00+07:00. Output is always UTC (Z).")
        .coercing(new InstantCoercing())
        .build();

    @Bean
    public RuntimeWiringConfigurer dateScalarsRuntimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(DATE_TIME).scalar(ExtendedScalars.Date);
    }

    private static final class InstantCoercing implements Coercing<Instant, String> {

        @Override
        public String serialize(Object dataFetcherResult, GraphQLContext graphQlContext, Locale locale)
            throws CoercingSerializeException {
            if (dataFetcherResult instanceof Instant instant) {
                return instant.toString();
            }
            throw new CoercingSerializeException("Expected an Instant but was " + typeName(dataFetcherResult));
        }

        @Override
        public Instant parseValue(Object input, GraphQLContext graphQlContext, Locale locale)
            throws CoercingParseValueException {
            if (!(input instanceof String text)) {
                throw new CoercingParseValueException("Expected a String but was " + typeName(input));
            }
            try {
                return parseStrictInstant(text);
            } catch (DateTimeParseException exception) {
                throw new CoercingParseValueException(invalidMessage(text), exception);
            }
        }

        @Override
        public Instant parseLiteral(
            Value<?> input, CoercedVariables variables, GraphQLContext graphQlContext, Locale locale
        ) throws CoercingParseLiteralException {
            if (!(input instanceof StringValue stringValue)) {
                throw new CoercingParseLiteralException("Expected a StringValue but was " + typeName(input));
            }
            try {
                return parseStrictInstant(stringValue.getValue());
            } catch (DateTimeParseException exception) {
                throw new CoercingParseLiteralException(invalidMessage(stringValue.getValue()), exception);
            }
        }

        @Override
        public Value<?> valueToLiteral(Object input, GraphQLContext graphQlContext, Locale locale) {
            return StringValue.newStringValue(serialize(input, graphQlContext, locale)).build();
        }

        private static Instant parseStrictInstant(String text) {
            return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
        }

        private static String invalidMessage(String text) {
            return "Invalid DateTime '" + text + "'. Expected ISO-8601 with an offset, for example 2026-07-28T09:30:00+07:00";
        }

        private static String typeName(Object value) {
            return value == null ? "null" : value.getClass().getSimpleName();
        }
    }
}
