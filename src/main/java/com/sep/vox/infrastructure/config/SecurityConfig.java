package com.sep.vox.infrastructure.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.sep.vox.application.port.output.PasswordEncoderPort;
import com.sep.vox.infrastructure.filter.JwtAuthenticationFilter;
import com.sep.vox.infrastructure.properties.AdminConsoleProperties;
import com.sep.vox.infrastructure.properties.CorsProperties;
import com.sep.vox.infrastructure.security.Argon2PasswordEncodeProvider;
import com.sep.vox.infrastructure.security.CustomOidcUserService;

import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationSuccessHandler authenticationSuccessHandler;
    private final AuthenticationFailureHandler authenticationFailureHandler;
    private final CustomOidcUserService customOidcUserService;
    private final CorsProperties corsProperties;
    private final AdminConsoleProperties adminConsoleProperties;
    private final String cookieDomain;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, AuthenticationSuccessHandler authenticationSuccessHandler, AuthenticationFailureHandler authenticationFailureHandler, CustomOidcUserService customOidcUserService, CorsProperties corsProperties, AdminConsoleProperties adminConsoleProperties, @Value("${app.cookie.domain:}") String cookieDomain) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.customOidcUserService = customOidcUserService;
        this.corsProperties = corsProperties;
        this.adminConsoleProperties = adminConsoleProperties;
        this.cookieDomain = cookieDomain;
    }
    
    private static final long HSTS_MAX_AGE_IN_SECONDS = 31536000;
    private static final boolean HSTS_INCLUDE_SUB_DOMAINS = true;
    private static final boolean HSTS_PRELOAD = true;

    private static final String[] PERMITTED_PATTERNS = {
        "/swagger-ui/**",
        "/v3/api-spec/**",
        "/error",
        "/api/v1/status",
        "/api/v1/auth/**",
        "/api/v1/callback/**",
        "/graphql",
        "/graphiql/**",
        "/internal/practice-sessions/**",
        "/internal/assessment-turns/**",
        "/actuator/**"
    };

    // Only the endpoints on the API chain that READ A COOKIE need CSRF, and this is the complete
    // list of them: the only two @CookieValue usages in the whole of src/main/java.
    //
    // Reason: every other route authenticates with the JWT in the Authorization header, and the
    // browser does NOT attach that header to a request caused by another page -- with no hidden
    // credential there is no CSRF to defend against. Only POST /api/v1/auth/refresh and
    // POST /api/v1/auth/logout read the refresh_token cookie (RestAuthController, @CookieValue),
    // and the browser attaches cookies automatically. Any endpoint added later that reads a cookie
    // must be added to this array, otherwise it is not protected.
    //
    // /logout is here not because its consequence is severe -- forcing a logout is only an
    // annoyance, not an account takeover -- but for exactly one reason: another page must not
    // decide when a student leaves the session. Kicking a student out in the middle of an oral
    // assessment ruins that assessment.
    //
    // This place used to be csrf().disable() for the whole chain. Turning it back on for the WHOLE
    // chain broke everything (it happened on 2026-08-31: every POST /graphql and
    // /api/v1/auth/login returned 403), so it had to be narrowed with
    // requireCsrfProtectionMatcher instead of being enabled everywhere.
    //
    // The frontend MUST set withXSRFToken: true (shared/api/apiClient.ts): since 1.6 axios does not
    // attach the X-XSRF-TOKEN header to a CROSS-ORIGIN request without that flag, and
    // voxenta.net -> api.voxenta.net is cross-origin. Without it every token refresh gets a 403
    // -- and because it only breaks when the access token expires, it looks like "sometimes I get
    // logged out".
    private static final String[] CSRF_PROTECTED_API_PATHS = {
        "/api/v1/auth/refresh",
        "/api/v1/auth/logout"
    };

    // Everything under /admin/** is handled by adminConsoleFilterChain, which declares its own securityMatcher, so
    // requests to it NEVER fall through to the main chain -- do not add /admin/** to
    // PERMITTED_PATTERNS above, it would have no effect other than causing confusion.
    private static final String ADMIN_CONSOLE_PATTERN = "/admin/**";
    private static final String ADMIN_CONSOLE_HOME = "/admin/";
    private static final String ADMIN_CONSOLE_LOGIN_PATH = "/admin/login";
    private static final String ADMIN_CONSOLE_LOGOUT_PATH = "/admin/logout";

    // The parameter names are set by the SBA UI, not chosen by us -- see the login button in the bundle.
    private static final String ADMIN_CONSOLE_REDIRECT_PARAMETER = "redirectTo";

    private static final String[] ADMIN_CONSOLE_PUBLIC_PATTERNS = {
        ADMIN_CONSOLE_LOGIN_PATH,
        "/admin/assets/**",
        "/admin/sba-settings.js",
        "/admin/variables.css"
    };

    // Registration path of the SBA client. ONLY this exact path, and only with POST, goes into the Basic chain --
    // see adminRegistrationFilterChain. The UI also calls GET on this same path to fetch the list of
    // instances, but GET is a browser request so it must fall through to the login-session chain.
    private static final String ADMIN_CONSOLE_REGISTRATION_PATH = "/admin/instances";

    // A role that exists only for this chain, not in the business role table. Intentionally kept apart
    // from the role system in the DB: an account that can see the heapdump/env of every service is an
    // operations matter, not a user level in the product.
    private static final String ADMIN_CONSOLE_ROLE = "ADMIN_CONSOLE";

    private static final List<String> CORS_ALLOWED_METHODS = List.of(
        HttpMethod.GET.name(),
        HttpMethod.POST.name(),
        HttpMethod.PUT.name(),
        HttpMethod.DELETE.name(),
        HttpMethod.PATCH.name(),
        HttpMethod.OPTIONS.name()
    );

    private static final boolean CORS_ALLOW_CREDENTIALS = true;
    private static final List<String> CORS_ALLOWED_HEADERS = List.of("*");

    // Content-Disposition is not in the default safe-header list of CORS, so
    // if it is not declared here the file export endpoints (CSV/Excel score sheets, question templates)
    // still download but JavaScript reads the file name as null and has to invent one.
    private static final List<String> CORS_EXPOSED_HEADERS = List.of(HttpHeaders.CONTENT_DISPOSITION);

    // If not set, Spring leaves Access-Control-Max-Age empty and the browser sends a preflight again
    // before ALMOST EVERY request -- for a GraphQL client that doubles the network round trips per query.
    private static final Duration CORS_MAX_AGE = Duration.ofMinutes(30);

    // Origin in the browser's standard form: scheme://host[:port], no path, no trailing '/'.
    private static final Pattern ORIGIN_PATTERN = Pattern.compile("^https?://[^/\\s]+$");

    /**
     * Chain ONLY for the registration call of the SBA client: {@code POST /admin/instances}.
     *
     * <p>Separated from the UI chain below so that logout still means something, not for tidiness.
     * After a browser has been asked for HTTP Basic once it REMEMBERS and re-attaches the
     * {@code Authorization} header to every following request, and there is no standard way to make it forget.
     * If BasicAuthenticationFilter shared a chain with the UI, then after clicking logout the session is really destroyed,
     * but the next request still carries the Basic header and is authenticated again immediately -- the user
     * "logs out" yet can still see the whole page.
     *
     * <p>Restricted to exactly METHOD + PATH and not all of {@code /admin/instances}: the UI also calls
     * GET on that same path to fetch the instance list, and GET is what the browser issues and
     * must go through the login session.
     *
     * <p>Note when changing the configuration later: the client also calls {@code DELETE /admin/instances/{id}} if
     * {@code auto-deregistration} is on. By default it is OFF outside cloud environments
     * (ClientProperties.isAutoDeregistration) so it is not opened here. If turned on, it must be opened for
     * that method too, and consider that the deregister button in the UI uses the same path.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminRegistrationFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.POST, ADMIN_CONSOLE_REGISTRATION_PATH))
            .authenticationManager(new ProviderManager(adminConsoleAuthenticationProvider()))
            .authorizeHttpRequests(auth -> auth
                .anyRequest()
                .hasRole(ADMIN_CONSOLE_ROLE))
            .httpBasic(Customizer.withDefaults())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // A server-to-server call, with no cookie and nowhere to hold a CSRF token.
            .csrf(csrf -> csrf.disable());
        return http.build();
    }

    /**
     * The chain for the Spring Boot Admin page that a real PERSON opens in a browser.
     *
     * <p>Deliberately NO httpBasic -- see {@link #adminRegistrationFilterChain(HttpSecurity)}.
     * Only formLogin here, because the SBA UI page brings its own login screen:
     * {@code UiController} only declares {@code @GetMapping("/login")} to RENDER that page, and the part
     * that receives the POST is provided by Spring Security's formLogin. Without formLogin the form shows up
     * normally but pressing login gives 405 Method Not Supported.
     *
     * <p>Because there is formLogin, STATELESS is NOT set here: logging in needs a session to remember. The main
     * chain is not affected, it stays STATELESS as before.
     *
     * <p>With a session and a cookie, CSRF makes sense again, and the UI itself is already written to
     * use it -- the logout button is a POST form that reads the {@code XSRF-TOKEN} cookie by itself, and every
     * XHR call goes through axios with {@code xsrfCookieName: 'XSRF-TOKEN'}. The two choices below must be
     * right, getting either one wrong breaks things in a hard-to-trace way:
     * <ul>
     *   <li>{@link CookieCsrfTokenRepository} and not the default one stored in the session --
     *       the UI reads the token from the cookie, and the session repository never writes any cookie.</li>
     *   <li>The PLAIN {@link CsrfTokenRequestAttributeHandler}, not the default Xor one. The Xor one
     *       waits to receive a token masked against BREACH and then unmasks it; the UI sends back exactly the raw value
     *       from the cookie so unmasking produces garbage and every write returns 403.</li>
     * </ul>
     *
     * <p>{@code setCsrfRequestAttributeName(null)} is not a superfluous detail: it forces
     * {@code handle()} to call {@code getParameterName()} inside CsrfFilter itself, i.e. the token is
     * generated and the cookie written BEFORE the controller runs. Left at the default the token is generated lazily, and the
     * sba-settings.js template reads {@code ${_csrf.parameterName}} midway through rendering -- by then the
     * response is already committed and writing the cookie no longer has any effect (previously with the session repository it was even
     * worse: it threw "Cannot create a session after the response has been committed" outright).
     */
    @Bean
    @Order(2)
    public SecurityFilterChain adminConsoleFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        csrfRequestHandler.setCsrfRequestAttributeName(null);

        // The UI attaches the return target to the "redirectTo" parameter when it kicks the user to the login
        // page. Without declaring this parameter name, logging in always falls back to the home page, losing exactly
        // the instance page the person had open.
        SavedRequestAwareAuthenticationSuccessHandler loginSuccessHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        loginSuccessHandler.setTargetUrlParameter(ADMIN_CONSOLE_REDIRECT_PARAMETER);
        loginSuccessHandler.setDefaultTargetUrl(ADMIN_CONSOLE_HOME);

        http
            .securityMatcher(ADMIN_CONSOLE_PATTERN)
            .authenticationManager(new ProviderManager(adminConsoleAuthenticationProvider()))
            .authorizeHttpRequests(auth -> auth
                // The login page and what it needs in order to render. sba-settings.js is in the
                // list because the login page itself also reads SBA.csrf.parameterName from it -- blocking
                // it means the login form cannot carry a token and nobody can get in.
                .requestMatchers(ADMIN_CONSOLE_PUBLIC_PATTERNS)
                .permitAll()
                .anyRequest()
                .hasRole(ADMIN_CONSOLE_ROLE))
            .formLogin(form -> form
                .loginPage(ADMIN_CONSOLE_LOGIN_PATH)
                .loginProcessingUrl(ADMIN_CONSOLE_LOGIN_PATH)
                .successHandler(loginSuccessHandler))
            .logout(logout -> logout
                .logoutUrl(ADMIN_CONSOLE_LOGOUT_PATH)
                .logoutSuccessUrl(ADMIN_CONSOLE_LOGIN_PATH))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(csrfRequestHandler));
        return http.build();
    }

    /**
     * The XSRF-TOKEN cookie for the API chain, with a Domain when {@code app.cookie.domain} is set.
     *
     * <p>Without Domain the cookie is HOST-ONLY, i.e. it belongs only to the exact host that returned it. On a
     * deployment where the backend is api.voxenta.net and the frontend is voxenta.net,
     * {@code document.cookie} on the frontend can NOT see the cookie of api.voxenta.net -- axios
     * has nothing to attach to the X-XSRF-TOKEN header and /api/v1/auth/refresh returns 403 forever. This really
     * happened on 2026-09-01, confirmed by the response header:
     * {@code Set-Cookie: XSRF-TOKEN=...; Path=/; Secure} -- no Domain.
     *
     * <p>Setting {@code app.cookie.domain=voxenta.net} lets voxenta.net, www.voxenta.net and
     * api.voxenta.net all read it. Trade-off: EVERY subdomain of voxenta.net can read this token.
     * Acceptable because the CSRF token is not a login secret -- it exists to prove that a
     * request was issued by our own page, and our own page must be able to read it.
     *
     * <p>NO leading dot. The ".voxenta.net" form is old RFC 2109 syntax; RFC 6265 dropped it entirely
     * and specifies that Domain=voxenta.net already covers every subdomain. Tomcat follows RFC 6265 and throws
     * {@code IllegalArgumentException: An invalid domain [.voxenta.net] was specified for this
     * cookie} -- not at startup but on EVERY request, so every endpoint returns 500. That is why
     * this is blocked right when the filter chain is built instead of letting the app start and then break completely.
     *
     * <p>Leave it EMPTY when running locally: localhost:5173 and localhost:8081 share the same host "localhost" (cookies
     * do not distinguish ports) so host-only is enough. Setting "localhost" here actually breaks it, because a cookie with
     * a Domain does not apply to a bare host in some browsers.
     */
    private CookieCsrfTokenRepository apiCsrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        if (StringUtils.hasText(cookieDomain)) {
            if (cookieDomain.startsWith(".")) {
                throw new IllegalStateException(
                    "app.cookie.domain = '" + cookieDomain + "' -- remove the leading dot. Tomcat, following "
                        + "RFC 6265, rejects a domain of this form and throws an error on EVERY request, making every "
                        + "endpoint return 500. Use '" + cookieDomain.substring(1)
                        + "', it already covers every subdomain.");
            }
            repository.setCookieCustomizer(cookie -> cookie.domain(cookieDomain));
        }
        return repository;
    }

    /**
     * The account shared by the two admin chains above. Built fresh on each call instead of being a bean:
     * JwtAuthenticationFilter and JwtGrpcServerInterceptor both inject UserDetailsService BY
     * TYPE, so adding an InMemoryUserDetailsManager to the context means those two places can no longer resolve
     * it and the app dies right at startup.
     */
    private DaoAuthenticationProvider adminConsoleAuthenticationProvider() {
        String username = requireConfigured(adminConsoleProperties.username(), "app.admin.username", "SBA_ADMIN_USERNAME");
        String password = requireConfigured(adminConsoleProperties.password(), "app.admin.password", "SBA_ADMIN_PASSWORD");

        // BCrypt and not the app's Argon2PasswordEncoder bean: the SBA client re-registers every
        // 10 seconds (ClientProperties.period), so this hash function runs for the whole life of the process.
        // Argon2 with the parameters set below eats 4MB of memory per call, in exchange for exactly zero
        // security value -- the raw password is already sitting in the configuration for the client to use.
        BCryptPasswordEncoder passwordHasher = new BCryptPasswordEncoder();

        UserDetails adminUser = User.withUsername(username)
            .password(passwordHasher.encode(password))
            .roles(ADMIN_CONSOLE_ROLE)
            .build();
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(new InMemoryUserDetailsManager(adminUser));
        authenticationProvider.setPasswordEncoder(passwordHasher);
        return authenticationProvider;
    }

    @Bean
    @Order(3)
    public SecurityFilterChain configure(HttpSecurity http) {
        // See the comment block at CSRF_PROTECTED_API_PATHS. Reading LAZILY means the cookie is only written on exactly the
        // /refresh request -- which is one beat later than when the client needs it.
        CsrfTokenRequestAttributeHandler apiCsrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        apiCsrfRequestHandler.setCsrfRequestAttributeName(null);

        List<RequestMatcher> csrfProtectedMatchers = Arrays.stream(CSRF_PROTECTED_API_PATHS)
            .map(path -> (RequestMatcher) PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.POST, path))
            .toList();

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(apiCsrfTokenRepository())
                .csrfTokenRequestHandler(apiCsrfRequestHandler)
                // REVERSES the default: the default is "protect everything except the skip list", here it is
                // "protect only exactly these paths". requireCsrfProtectionMatcher replaces the default matcher
                // DIRECTLY, including the rule that skips GET/HEAD/TRACE/OPTIONS -- so the METHOD must be pinned by hand,
                // leaving it empty means a GET on those paths is also asked for a token.
                .requireCsrfProtectionMatcher(new OrRequestMatcher(csrfProtectedMatchers)))
            .formLogin(fl -> fl.disable())
            .httpBasic(hb -> hb.disable())
            .cors(cors -> cors
                .configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .xssProtection(xss -> xss
                    .headerValue(HeaderValue.ENABLED_MODE_BLOCK))
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(HSTS_MAX_AGE_IN_SECONDS)
                    .includeSubDomains(HSTS_INCLUDE_SUB_DOMAINS)
                    .preload(HSTS_PRELOAD))
                .contentTypeOptions(_ -> {}))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PERMITTED_PATTERNS)
                .permitAll()
                .anyRequest()
                .authenticated())
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    PathPatternRequestMatcher.withDefaults().matcher("/api/**")
                )
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(u -> u.oidcUserService(customOidcUserService))
                .successHandler(authenticationSuccessHandler)
                .failureHandler(authenticationFailureHandler)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> allowedOrigins = validatedAllowedOrigins();

        CorsConfiguration config = new CorsConfiguration();
        // setAllowedOrigins and not setAllowedOriginPatterns: the pattern version matches
        // by wildcard, so a stray '*' in the configuration opens the API to the whole internet
        // together with the login cookie, while the app still starts normally without any warning.
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(CORS_ALLOWED_METHODS);
        config.setAllowCredentials(CORS_ALLOW_CREDENTIALS);
        config.setAllowedHeaders(CORS_ALLOWED_HEADERS);
        config.setExposedHeaders(CORS_EXPOSED_HEADERS);
        config.setMaxAge(CORS_MAX_AGE);

        UrlBasedCorsConfigurationSource urlBasedSource = new UrlBasedCorsConfigurationSource();
        urlBasedSource.registerCorsConfiguration("/**", config);
        return urlBasedSource;
    }

    /**
     * Blocks at startup instead of letting the app run with an empty account. Without this,
     * {@code User.withUsername(null)} throws a bare NullPointerException while the filter chain is being built,
     * and an empty password is even worse: the app comes up normally and {@code /admin/**} accepts exactly one
     * empty string as the password.
     */
    private String requireConfigured(String value, String property, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                property + " is empty -- /admin/** opens the whole actuator of every instance that has "
                    + "registered, so there is no default password. Set the environment variable "
                    + environmentVariable + ".");
        }
        return value;
    }

    /**
     * Block wrong origin configurations right at startup -- once running, every error here
     * shows up on the browser side as "missing Access-Control-Allow-Origin header",
     * with not a single clue in the server log.
     */
    private List<String> validatedAllowedOrigins() {
        List<String> allowedOrigins = corsProperties.allowedOrigins();

        if (allowedOrigins.isEmpty()) {
            throw new IllegalStateException(
                "app.cors.allowed-origins is empty -- every cross-origin request will be blocked. "
                    + "Set the environment variable CORS_ALLOWED_ORIGINS.");
        }

        List<String> malformed = allowedOrigins.stream()
            .filter(origin -> !ORIGIN_PATTERN.matcher(origin).matches())
            .toList();
        if (!malformed.isEmpty()) {
            throw new IllegalStateException(
                "app.cors.allowed-origins only accepts origins in the form scheme://host[:port], "
                    + "no path, no trailing '/', no wildcard. Invalid: " + malformed);
        }

        return allowedOrigins;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }


    @Bean
    public PasswordEncoderPort passwordEncoderPort() {
        return new Argon2PasswordEncodeProvider(passwordEncoder());
    }
}
