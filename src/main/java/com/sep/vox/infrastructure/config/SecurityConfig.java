package com.sep.vox.infrastructure.config;

import java.time.Duration;
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
import com.sep.vox.infrastructure.security.OAuth2AuthenticationFailureHandler;
import com.sep.vox.infrastructure.security.OAuth2AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final CustomOidcUserService customOidcUserService;
    private final CorsProperties corsProperties;
    private final AdminConsoleProperties adminConsoleProperties;
    private final String cookieDomain;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler, OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler, CustomOidcUserService customOidcUserService, CorsProperties corsProperties, AdminConsoleProperties adminConsoleProperties, @Value("${app.cookie.domain:}") String cookieDomain) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.oAuth2AuthenticationFailureHandler = oAuth2AuthenticationFailureHandler;
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
        "/internal/exam-turns/**",
        "/actuator/**"
    };

    // ĐÚNG MỘT endpoint trên chain API cần CSRF, và đây là nó.
    //
    // Lý do: mọi route khác xác thực bằng JWT trong header Authorization, mà trình duyệt KHÔNG
    // tự gắn header đó vào request do trang khác gây ra -- không có credential đi ngầm thì không
    // có CSRF để chống. Riêng POST /api/v1/auth/refresh đọc cookie refresh_token
    // (AuthController, @CookieValue), và cookie thì trình duyệt gắn tự động. Đây cũng là
    // @CookieValue DUY NHẤT trong toàn bộ src/main/java -- nếu sau này thêm endpoint nào đọc
    // cookie nữa thì phải thêm vào đây, không thì nó không được bảo vệ.
    //
    // Trước đây chỗ này từng là csrf().disable() cho cả chain. Bật lại cho TOÀN BỘ chain thì
    // hỏng hết (đã xảy ra 2026-08-31: mọi POST /graphql và /api/v1/auth/login trả 403), nên
    // phải thu hẹp bằng requireCsrfProtectionMatcher chứ không bật đại trà.
    //
    // Phía frontend BẮT BUỘC phải có withXSRFToken: true (shared/api/apiClient.ts): axios từ
    // 1.6 không tự gắn header X-XSRF-TOKEN cho request KHÁC ORIGIN nếu thiếu cờ đó, và
    // voxenta.net -> api.voxenta.net là khác origin. Thiếu nó thì mọi lần refresh token đều 403
    // -- và vì chỉ hỏng lúc access token hết hạn nên trông như "thỉnh thoảng bị đăng xuất".
    private static final String CSRF_PROTECTED_API_PATH = "/api/v1/auth/refresh";

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

    private static final int ARGON2_SALT_LENGTH = 16;
    private static final int ARGON2_HASH_LENGTH = 32;
    private static final int ARGON2_PARALLELISM = 1;
    private static final int ARGON2_MEMORY = 1 << 12;
    private static final int ARGON2_ITERATION = 3;

    
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
     * Chain cho trang Spring Boot Admin mà NGƯỜI thật mở bằng trình duyệt.
     *
     * <p>Cố ý KHÔNG có httpBasic -- xem {@link #adminRegistrationFilterChain(HttpSecurity)}.
     * Ở đây chỉ có formLogin, vì trang UI của SBA tự mang sẵn màn đăng nhập riêng:
     * {@code UiController} chỉ khai {@code @GetMapping("/login")} để RENDER trang đó, còn phần
     * nhận POST là do formLogin của Spring Security cung cấp. Thiếu formLogin thì form hiện ra
     * bình thường nhưng bấm đăng nhập là 405 Method Not Supported.
     *
     * <p>Vì có formLogin nên KHÔNG đặt STATELESS ở đây: đăng nhập cần session để nhớ. Chain
     * chính không bị ảnh hưởng, nó vẫn STATELESS như cũ.
     *
     * <p>Có session và cookie thì CSRF trở lại có nghĩa, và bản thân UI cũng đã được viết sẵn để
     * dùng nó -- nút đăng xuất là một form POST tự đọc cookie {@code XSRF-TOKEN}, còn mọi lời gọi
     * XHR đi qua axios với {@code xsrfCookieName: 'XSRF-TOKEN'}. Hai lựa chọn dưới đây bắt buộc
     * phải đúng, sai cái nào cũng hỏng theo kiểu khó lần:
     * <ul>
     *   <li>{@link CookieCsrfTokenRepository} chứ không phải bản mặc định lưu trong session --
     *       UI đọc token từ cookie, session repository không bao giờ ghi ra cookie nào.</li>
     *   <li>{@link CsrfTokenRequestAttributeHandler} TRẦN, không phải bản Xor mặc định. Bản Xor
     *       chờ nhận token đã che theo BREACH rồi giải mã ngược; UI gửi lại đúng giá trị thô
     *       trong cookie nên giải mã ra rác và mọi thao tác ghi đều 403.</li>
     * </ul>
     *
     * <p>{@code setCsrfRequestAttributeName(null)} không phải chi tiết thừa: nó ép
     * {@code handle()} gọi {@code getParameterName()} ngay trong CsrfFilter, tức là token được
     * sinh và ghi cookie TRƯỚC khi controller chạy. Để mặc định thì token sinh kiểu lười, và
     * template sba-settings.js đọc {@code ${_csrf.parameterName}} giữa chừng lúc render -- lúc đó
     * response đã commit, ghi cookie không còn tác dụng (trước đây với session repository thì còn
     * tệ hơn: ném thẳng "Cannot create a session after the response has been committed").
     */
    @Bean
    @Order(2)
    public SecurityFilterChain adminConsoleFilterChain(HttpSecurity http) throws Exception {
        var csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        csrfRequestHandler.setCsrfRequestAttributeName(null);

        // UI gắn sẵn đích quay lại vào tham số "redirectTo" khi đá người dùng sang trang đăng
        // nhập. Không khai tên tham số này thì đăng nhập xong luôn rơi về trang chủ, mất đúng
        // trang instance mà người ta đang mở dở.
        var loginSuccessHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        loginSuccessHandler.setTargetUrlParameter(ADMIN_CONSOLE_REDIRECT_PARAMETER);
        loginSuccessHandler.setDefaultTargetUrl(ADMIN_CONSOLE_HOME);

        http
            .securityMatcher(ADMIN_CONSOLE_PATTERN)
            .authenticationManager(new ProviderManager(adminConsoleAuthenticationProvider()))
            .authorizeHttpRequests(auth -> auth
                // Trang đăng nhập và những gì nó cần để hiện ra được. sba-settings.js nằm trong
                // danh sách vì chính trang login cũng đọc SBA.csrf.parameterName từ đó -- chặn
                // nó lại là form đăng nhập không kèm được token và không ai vào nổi.
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
     * Cookie XSRF-TOKEN cho chain API, có khai Domain khi {@code app.cookie.domain} được đặt.
     *
     * <p>Không có Domain thì cookie là HOST-ONLY, tức chỉ thuộc về đúng host đã trả nó. Trên
     * deployment backend là api.voxenta.net còn frontend là voxenta.net, nên
     * {@code document.cookie} bên frontend KHÔNG đọc thấy cookie của api.voxenta.net -- axios
     * không có gì để gắn vào header X-XSRF-TOKEN và /api/v1/auth/refresh trả 403 mãi mãi. Đã xảy
     * ra thật 2026-09-01, xác nhận bằng response header:
     * {@code Set-Cookie: XSRF-TOKEN=...; Path=/; Secure} -- không có Domain.
     *
     * <p>Đặt {@code app.cookie.domain=voxenta.net} thì cả voxenta.net, www.voxenta.net và
     * api.voxenta.net cùng đọc được. Đánh đổi: MỌI subdomain của voxenta.net đọc được token này.
     * Chấp nhận được vì token CSRF không phải bí mật đăng nhập -- nó tồn tại để chứng minh
     * request do chính trang của mình phát ra, và trang của mình thì buộc phải đọc được nó.
     *
     * <p>KHÔNG có dấu chấm đầu. Kiểu ".voxenta.net" là cú pháp RFC 2109 cũ; RFC 6265 bỏ hẳn nó
     * và quy định Domain=voxenta.net vốn đã phủ mọi subdomain. Tomcat theo đúng RFC 6265 và ném
     * {@code IllegalArgumentException: An invalid domain [.voxenta.net] was specified for this
     * cookie} -- không phải lúc khởi động mà ở TỪNG request, nên mọi endpoint trả 500. Vì thế
     * chỗ này chặn ngay lúc dựng filter chain thay vì để app lên rồi hỏng toàn bộ.
     *
     * <p>Để TRỐNG khi chạy local: localhost:5173 và localhost:8081 cùng host "localhost" (cookie
     * không phân biệt cổng) nên host-only đã đủ. Khai "localhost" ở đây lại hỏng, vì cookie có
     * Domain thì không áp dụng cho host thuần trong một số trình duyệt.
     */
    private CookieCsrfTokenRepository apiCsrfTokenRepository() {
        var repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
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
     * Tài khoản dùng chung cho hai chain admin ở trên. Dựng mới mỗi lần gọi thay vì làm bean:
     * JwtAuthenticationFilter và JwtGrpcServerInterceptor cùng inject UserDetailsService THEO
     * KIỂU, nên thêm một InMemoryUserDetailsManager vào context là hai chỗ đó không phân giải
     * được nữa và app chết ngay lúc khởi động.
     */
    private DaoAuthenticationProvider adminConsoleAuthenticationProvider() {
        var username = requireConfigured(adminConsoleProperties.username(), "app.admin.username", "SBA_ADMIN_USERNAME");
        var password = requireConfigured(adminConsoleProperties.password(), "app.admin.password", "SBA_ADMIN_PASSWORD");

        // BCrypt chứ không dùng lại bean Argon2PasswordEncoder của app: SBA client đăng ký lại
        // mỗi 10 giây (ClientProperties.period), nên hàm băm này chạy suốt vòng đời process.
        // Argon2 với tham số đang đặt ở dưới ngốn 4MB bộ nhớ mỗi lần gọi, đổi lấy đúng số 0 giá
        // trị an ninh -- mật khẩu thô vốn đã nằm sẵn trong cấu hình để client dùng.
        var passwordHasher = new BCryptPasswordEncoder();

        var adminUser = User.withUsername(username)
            .password(passwordHasher.encode(password))
            .roles(ADMIN_CONSOLE_ROLE)
            .build();
        var authenticationProvider = new DaoAuthenticationProvider(new InMemoryUserDetailsManager(adminUser));
        authenticationProvider.setPasswordEncoder(passwordHasher);
        return authenticationProvider;
    }

    @Bean
    @Order(3)
    public SecurityFilterChain configure(HttpSecurity http) {
        // Xem khối chú thích ở CSRF_PROTECTED_API_PATH. Đọc LƯỜI là cookie chỉ được ghi ở đúng
        // request /refresh -- tức là muộn hơn một nhịp so với lúc client cần nó.
        var apiCsrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        apiCsrfRequestHandler.setCsrfRequestAttributeName(null);

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(apiCsrfTokenRepository())
                .csrfTokenRequestHandler(apiCsrfRequestHandler)
                // ĐẢO NGƯỢC mặc định: mặc định là "chặn tất cả trừ danh sách bỏ qua", ở đây là
                // "chỉ chặn đúng path này". requireCsrfProtectionMatcher thay THẲNG matcher mặc
                // định, kể cả luật bỏ qua GET/HEAD/TRACE/OPTIONS -- nên phải tự ghim METHOD, để
                // trống là GET trên path đó cũng bị đòi token.
                .requireCsrfProtectionMatcher(PathPatternRequestMatcher.withDefaults()
                    .matcher(HttpMethod.POST, CSRF_PROTECTED_API_PATH)))
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
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
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
        var allowedOrigins = validatedAllowedOrigins();

        var config = new CorsConfiguration();
        // setAllowedOrigins chứ không phải setAllowedOriginPatterns: bản pattern so khớp
        // bằng wildcard nên một dấu '*' lọt vào cấu hình là mở API cho toàn bộ internet
        // kèm cookie đăng nhập, mà vẫn khởi động bình thường không báo gì.
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(CORS_ALLOWED_METHODS);
        config.setAllowCredentials(CORS_ALLOW_CREDENTIALS);
        config.setAllowedHeaders(CORS_ALLOWED_HEADERS);
        config.setExposedHeaders(CORS_EXPOSED_HEADERS);
        config.setMaxAge(CORS_MAX_AGE);

        var urlBasedSource = new UrlBasedCorsConfigurationSource();
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
     * Chặn ngay lúc khởi động các cấu hình origin sai -- khi đã chạy rồi thì mọi lỗi ở đây
     * đều hiện ra dưới dạng "thiếu header Access-Control-Allow-Origin" ở phía trình duyệt,
     * không kèm chút manh mối nào trong log server.
     */
    private List<String> validatedAllowedOrigins() {
        var allowedOrigins = corsProperties.allowedOrigins();

        if (allowedOrigins.isEmpty()) {
            throw new InfrastructureException(
                "app.cors.allowed-origins đang trống -- mọi request cross-origin sẽ bị chặn. "
                    + "Đặt biến môi trường CORS_ALLOWED_ORIGINS.");
        }

        var malformed = allowedOrigins.stream()
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
        return new Argon2PasswordEncoder(
            ARGON2_SALT_LENGTH, 
            ARGON2_HASH_LENGTH, 
            ARGON2_PARALLELISM, 
            ARGON2_MEMORY, 
            ARGON2_ITERATION
        );
    }


    @Bean
    public PasswordEncoderPort passwordEncoderPort() {
        return new Argon2PasswordEncodeProvider(passwordEncoder());
    }
}
