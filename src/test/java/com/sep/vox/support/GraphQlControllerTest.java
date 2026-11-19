package com.sep.vox.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import com.sep.vox.config.GraphQlTestSecurityConfiguration;
import com.sep.vox.interfaces.graphql.exception.GlobalExceptionResolver;

/**
 * A GraphQL slice for one controller: the real schema from {@code src/main/resources/graphql}, the real
 * {@code GlobalExceptionResolver} and {@code @PreAuthorize}, with use cases supplied as
 * {@code @MockitoBean}. No database and no Docker, so it runs in {@code test}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@GraphQlTest
@Import({ GraphQlTestSecurityConfiguration.class, GlobalExceptionResolver.class })
public @interface GraphQlControllerTest {

    @AliasFor(annotation = GraphQlTest.class, attribute = "controllers")
    Class<?>[] value();
}
