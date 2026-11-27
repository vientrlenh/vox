package com.sep.vox.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The HTTP Basic account guarding the whole {@code /admin/**} -- both the Spring Boot Admin page
 * and the {@code POST /admin/instances} endpoint that the SBA client calls to register itself.
 *
 * <p>Intentionally has NO default value. Behind {@code /admin/**} is the entire actuator of every
 * registered instance (health with details, env, heapdump) and also the WRITE proxy path down to them --
 * see {@code InstancesProxyController}, which accepts POST/PUT/PATCH/DELETE too. A default password
 * slipping in here opens that whole set to anyone who can reach the port, while the app still starts
 * silently as if nothing were wrong.
 *
 * <p>The password is kept RAW, not as a hash. This very process is both server and client, so the
 * SBA client must have the raw value to attach to the Basic header
 * ({@code spring.boot.admin.client.password}). Keeping an extra hash for the server side hides
 * nothing -- the raw value sits right next to it in the same file -- and just creates two values that
 * must be remembered and changed together. The server side hashes with BCrypt at startup, see
 * {@code SecurityConfig#adminConsoleFilterChain}.
 *
 * @param username login name, set via the environment variable {@code SBA_ADMIN_USERNAME}
 * @param password raw password, set via the environment variable {@code SBA_ADMIN_PASSWORD}
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminConsoleProperties(String username, String password) {
}
