package com.sep.vox.infrastructure.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The list of origins allowed to call the API, declared separately for each environment.
 *
 * <p>This is the ONLY part of the CORS configuration that changes between local/staging/production, so
 * only it is exposed outside the configuration file; method, header, credentials and max-age stay
 * fixed in SecurityConfig as constants. Exposing those too would only create more
 * environment variables nobody ever sets, and more places to deploy wrongly.
 *
 * <p>Intentionally does NOT accept a wildcard: the CORS bean enables {@code allowCredentials} so the refresh
 * token cookie travels with the request, and an origin of {@code *} plus credentials means any
 * web page the user opens can call the API with their own login session. To allow several
 * domains, list them all, do not shorten them into a pattern.
 *
 * <p>The accepted value is an origin in the browser's exact sense -- {@code scheme://host[:port]},
 * with no path and no trailing {@code /}. One wrong character makes Spring's comparison miss and
 * return 403 with no header to trace the cause, so this is validated at startup
 * instead of surfacing in production.
 *
 * @param allowedOrigins the allowed origins, comma-separated when passed through the environment
 *                       variable {@code CORS_ALLOWED_ORIGINS}
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null
            ? List.of()
            : allowedOrigins.stream()
                .map(origin -> origin.trim())
                .filter(origin -> !origin.isEmpty())
                .toList();
    }
}
