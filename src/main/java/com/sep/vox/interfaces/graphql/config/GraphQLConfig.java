package com.sep.vox.interfaces.graphql.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import com.sep.vox.interfaces.graphql.config.scalar.GraphQLDateTimeCoercing;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;

@Configuration 
public class GraphQLConfig {
    
    @Bean 
    RuntimeWiringConfigurer runtimeWiringConfigurer() {
        GraphQLScalarType dateTimeScalarType = GraphQLScalarType.newScalar()
                .name("DateTime")
                .description("ISO-8601 UTC timestamp")
                .coercing(new GraphQLDateTimeCoercing())
                .build();
        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.Date)
                .scalar(dateTimeScalarType);
    }
}
