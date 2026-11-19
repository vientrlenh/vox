package com.sep.vox.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import com.sep.vox.config.WebMvcTestSecurityConfiguration;
import com.sep.vox.infrastructure.filter.JwtAuthenticationFilter;
import com.sep.vox.infrastructure.filter.PracticeInternalSecretFilter;

/**
 * A web slice for one REST controller: routing, JSON binding, validation and error mapping through
 * {@code GlobalExceptionHandler}, with use cases and ports supplied as {@code @MockitoBean}.
 * No database and no Docker, so it runs in {@code test}.
 *
 * <p>The two production filters are excluded because they are {@code @Component}s that the slice would
 * otherwise scan and fail to wire; {@link WebMvcTestSecurityConfiguration} supplies the security instead.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest(excludeFilters = @Filter(
    type = FilterType.ASSIGNABLE_TYPE,
    classes = { JwtAuthenticationFilter.class, PracticeInternalSecretFilter.class }))
@Import(WebMvcTestSecurityConfiguration.class)
public @interface RestControllerTest {

    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] value();
}
