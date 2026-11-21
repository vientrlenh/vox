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
import com.sep.vox.infrastructure.exception.InfrastructureException;
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

    // Chỉ những endpoint trên chain API ĐỌC COOKIE mới cần CSRF, và đây là danh sách đủ của
    // chúng: hai @CookieValue duy nhất trong toàn bộ src/main/java.
    //
    // Lý do: mọi route khác xác thực bằng JWT trong header Authorization, mà trình duyệt KHÔNG
    // tự gắn header đó vào request do trang khác gây ra -- không có credential đi ngầm thì không
    // có CSRF để chống. Riêng POST /api/v1/auth/refresh và POST /api/v1/auth/logout đọc cookie
    // refresh_token (AuthController, @CookieValue), và cookie thì trình duyệt gắn tự động. Thêm
    // endpoint nào đọc cookie nữa thì phải thêm vào mảng này, không thì nó không được bảo vệ.
    //
    // /logout nằm đây không phải vì hậu quả nặng -- ép đăng xuất chỉ là phiền, không phải chiếm
    // tài khoản -- mà vì đúng một lý do: trang khác không được phép quyết định thời điểm học sinh
    // rời phiên. Đá một học sinh ra giữa bài thi vấn đáp là làm hỏng bài thi đó.
    //
    // Trước đây chỗ này từng là csrf().disable() cho cả chain. Bật lại cho TOÀN BỘ chain thì
    // hỏng hết (đã xảy ra 2026-08-31: mọi POST /graphql và /api/v1/auth/login trả 403), nên
    // phải thu hẹp bằng requireCsrfProtectionMatcher chứ không bật đại trà.
    //
    // Phía frontend BẮT BUỘC phải có withXSRFToken: true (shared/api/apiClient.ts): axios từ
    // 1.6 không tự gắn header X-XSRF-TOKEN cho request KHÁC ORIGIN nếu thiếu cờ đó, và
    // voxenta.net -> api.voxenta.net là khác origin. Thiếu nó thì mọi lần refresh token đều 403
    // -- và vì chỉ hỏng lúc access token hết hạn nên trông như "thỉnh thoảng bị đăng xuất".
    private static final String[] CSRF_PROTECTED_API_PATHS = {
        "/api/v1/auth/refresh",
        "/api/v1/auth/logout"
    };

    // Toàn bộ /admin/** do adminConsoleFilterChain lo, chain đó khai securityMatcher riêng nên
    // request tới đây KHÔNG bao giờ rơi xuống chain chính -- đừng thêm /admin/** vào
    // PERMITTED_PATTERNS ở trên, nó sẽ không có tác dụng gì ngoài việc gây hiểu nhầm.
    private static final String ADMIN_CONSOLE_PATTERN = "/admin/**";
    private static final String ADMIN_CONSOLE_HOME = "/admin/";
    private static final String ADMIN_CONSOLE_LOGIN_PATH = "/admin/login";
    private static final String ADMIN_CONSOLE_LOGOUT_PATH = "/admin/logout";

    // Tên tham số do UI của SBA đặt, không phải mình chọn -- xem nút đăng nhập trong bundle.
    private static final String ADMIN_CONSOLE_REDIRECT_PARAMETER = "redirectTo";

    private static final String[] ADMIN_CONSOLE_PUBLIC_PATTERNS = {
        ADMIN_CONSOLE_LOGIN_PATH,
        "/admin/assets/**",
        "/admin/sba-settings.js",
        "/admin/variables.css"
    };

    // Đường đăng ký của SBA client. Chỉ ĐÚNG path này, và chỉ với POST, đi vào chain Basic --
    // xem adminRegistrationFilterChain. UI cũng gọi GET trên chính path này để lấy danh sách
    // instance, nhưng GET là request của trình duyệt nên phải rơi xuống chain phiên đăng nhập.
    private static final String ADMIN_CONSOLE_REGISTRATION_PATH = "/admin/instances";

    // Vai trò chỉ tồn tại cho chain này, không nằm trong bảng role của nghiệp vụ. Cố ý tách
    // khỏi hệ thống role trong DB: tài khoản xem được heapdump/env của mọi service là chuyện
    // vận hành, không phải một cấp bậc người dùng trong sản phẩm.
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

    // Content-Disposition không nằm trong danh sách header an toàn mặc định của CORS, nên
    // nếu không khai ở đây thì các endpoint xuất file (bảng điểm CSV/Excel, template câu hỏi)
    // vẫn tải được nhưng JavaScript đọc ra tên file là null và phải tự bịa tên.
    private static final List<String> CORS_EXPOSED_HEADERS = List.of(HttpHeaders.CONTENT_DISPOSITION);

    // Không đặt thì Spring bỏ trống Access-Control-Max-Age và trình duyệt preflight lại
    // trước GẦN NHƯ MỌI request -- với client GraphQL là gấp đôi số vòng mạng cho mỗi query.
    private static final Duration CORS_MAX_AGE = Duration.ofMinutes(30);

    // Origin theo chuẩn trình duyệt: scheme://host[:port], không path, không '/' cuối.
    private static final Pattern ORIGIN_PATTERN = Pattern.compile("^https?://[^/\\s]+$");

    /**
     * Chain CHỈ dành cho lời gọi đăng ký của SBA client: {@code POST /admin/instances}.
     *
     * <p>Tách riêng khỏi chain UI bên dưới là để logout còn có nghĩa, không phải để cho gọn.
     * Trình duyệt sau khi bị hỏi HTTP Basic một lần sẽ NHỚ và tự gắn lại header
     * {@code Authorization} vào mọi request tiếp theo, và không có cách chuẩn nào bảo nó quên.
     * Nếu BasicAuthenticationFilter nằm chung chain với UI thì bấm đăng xuất xong session bị huỷ
     * thật, nhưng request kế tiếp vẫn mang Basic header nên được xác thực lại ngay -- người dùng
     * "đăng xuất" mà vẫn xem được toàn bộ trang.
     *
     * <p>Giới hạn đúng METHOD + PATH chứ không phải cả {@code /admin/instances}: UI cũng gọi
     * GET trên chính path đó để lấy danh sách instance, mà GET là thứ trình duyệt phát ra và
     * phải đi bằng phiên đăng nhập.
     *
     * <p>Lưu ý khi đổi cấu hình sau này: client còn gọi {@code DELETE /admin/instances/{id}} nếu
     * bật {@code auto-deregistration}. Mặc định nó TẮT ngoài môi trường cloud
     * (ClientProperties.isAutoDeregistration) nên ở đây không mở. Bật lên thì phải mở thêm cho
     * method đó, và cân nhắc luôn việc nút gỡ đăng ký trên UI dùng chung path.
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
            // Lời gọi server-to-server, không có cookie và không có chỗ nào để giữ CSRF token.
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
                throw new InfrastructureException(
                    "app.cookie.domain = '" + cookieDomain + "' -- bỏ dấu chấm ở đầu. Tomcat theo "
                        + "RFC 6265 từ chối domain dạng này và ném lỗi ở TỪNG request, khiến mọi "
                        + "endpoint trả 500. Dùng '" + cookieDomain.substring(1)
                        + "', nó đã phủ sẵn mọi subdomain.");
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
     * Chặn ngay lúc khởi động thay vì để app chạy với tài khoản rỗng. Không có chỗ này thì
     * {@code User.withUsername(null)} ném NullPointerException trần giữa lúc dựng filter chain,
     * còn mật khẩu rỗng thì tệ hơn nữa: app lên bình thường và {@code /admin/**} nhận đúng một
     * chuỗi rỗng làm mật khẩu.
     */
    private String requireConfigured(String value, String property, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new InfrastructureException(
                property + " đang trống -- /admin/** mở ra toàn bộ actuator của mọi instance đã "
                    + "đăng ký nên không có mật khẩu mặc định. Đặt biến môi trường "
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
            throw new InfrastructureException(
                "app.cors.allowed-origins đang trống -- mọi request cross-origin sẽ bị chặn. "
                    + "Đặt biến môi trường CORS_ALLOWED_ORIGINS.");
        }

        List<String> malformed = allowedOrigins.stream()
            .filter(origin -> !ORIGIN_PATTERN.matcher(origin).matches())
            .toList();
        if (!malformed.isEmpty()) {
            throw new InfrastructureException(
                "app.cors.allowed-origins chỉ nhận origin dạng scheme://host[:port], "
                    + "không path, không '/' ở cuối, không wildcard. Sai: " + malformed);
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
