package com.sep.vox.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Turns on {@code @PreAuthorize} for {@link com.sep.vox.support.GraphQlControllerTest} slices. GraphQL requests in these tests
 * run directly against the GraphQL engine, so there is no HTTP security filter chain; the caller comes
 * from {@code @WithMockUser} / {@code @WithAnonymousUser}.
 */
@TestConfiguration(proxyBeanMethods = false)
@EnableMethodSecurity
public class GraphQlTestSecurityConfiguration {
}
