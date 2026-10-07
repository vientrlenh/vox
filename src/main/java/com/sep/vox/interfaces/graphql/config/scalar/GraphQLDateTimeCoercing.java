package com.sep.vox.interfaces.graphql.config.scalar;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

public class GraphQLDateTimeCoercing implements Coercing<Instant, String> {
    
    @Override
    public String serialize(Object value, GraphQLContext ctx, Locale locale) {
        if (value instanceof Instant instant) {
            return instant.toString();
        }
        throw new CoercingSerializeException("Expected Instant but got: " + value.getClass());
    }


    @Override 
    public Instant parseValue(Object input, GraphQLContext ctx, Locale locale) {
        try {
            return Instant.parse(input.toString());
        } catch (DateTimeParseException ex) {
            throw new CoercingParseValueException("Invalid DateTime: " + input);
        }
    }


    @Override
    public Instant parseLiteral(Value<?> input, CoercedVariables vars, GraphQLContext ctx, Locale locale) {
        if (input instanceof StringValue s) {
            try {
                return Instant.parse(s.getValue());
            } catch (DateTimeParseException ex) {
                throw new CoercingParseLiteralException("Invalid DateTime: " + s.getValue());
            }
        }
        throw new CoercingParseLiteralException("DateTime must be a string");
    }
}
