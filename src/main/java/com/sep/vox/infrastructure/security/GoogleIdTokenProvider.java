package com.sep.vox.infrastructure.security;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.output.GoogleIdTokenVerifierPort;
import com.sep.vox.application.response.output.VerifiedGoogleIdentity;
import com.sep.vox.infrastructure.properties.GoogleIdTokenProperties;

/**
 * Kiểm chứng ID token Google bằng {@link NimbusJwtDecoder}, không bằng thư viện của Google.
 *
 * <p>Lý do chọn Nimbus: nó đã có sẵn trên classpath qua {@code spring-boot-starter-oauth2-client}
 * -- cùng thư viện mà chính luồng redirect của web đang dùng để kiểm token. Bản của Google
 * ({@code GoogleIdTokenVerifier}) chỉ tới đây theo đường phụ thuộc BẮC CẦU của
 * {@code firebase-admin}, tức là một thứ có thể biến mất trong một lần nâng phiên bản firebase mà
 * không ai đọc changelog -- và thứ biến mất khi đó là cửa xác thực.
 *
 * <p>Bộ khoá công khai của Google xoay định kỳ; {@code NimbusJwtDecoder} tự tải lại và tự cache nên
 * ở đây không có gì phải làm thêm. Decoder dựng MỘT LẦN trong constructor chứ không mỗi lần gọi:
 * dựng lại mỗi request nghĩa là tải lại JWK set mỗi lần đăng nhập, biến mỗi lần bấm nút thành một
 * vòng mạng ra ngoài internet.
 */
@Component
public class GoogleIdTokenProvider implements GoogleIdTokenVerifierPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleIdTokenProvider.class);

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_EMAIL_VERIFIED = "email_verified";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_PICTURE = "picture";

    private final JwtDecoder jwtDecoder;
    private final List<String> allowedAudiences;

    public GoogleIdTokenProvider(GoogleIdTokenProperties properties) {
        this.allowedAudiences = properties.allowedAudiences();

        // Cảnh báo chứ không ném: đây là tính năng THÊM VÀO, và mọi deployment đang chạy đều chưa
        // đặt biến này. Chết lúc khởi động sẽ hạ nguyên hệ thống chỉ vì một đường đăng nhập chưa ai
        // dùng -- khác hẳn CORS hay tài khoản admin, những thứ luôn cần thiết nên fail-fast là đúng.
        // Phần từ chối thật nằm ở verify(), nơi có người thật sự đang cố đăng nhập để mà báo lỗi.
        if (allowedAudiences.isEmpty()) {
            LOGGER.warn(
                "app.oauth2.google.allowed-audiences đang trống -- đăng nhập Google từ ứng dụng "
                    + "native (Flutter/WPF) sẽ bị từ chối toàn bộ. Đặt biến môi trường "
                    + "GOOGLE_ALLOWED_AUDIENCES gồm client ID WEB (Flutter gửi cái này qua "
                    + "serverClientId) và client ID DESKTOP (WPF). Luồng đăng nhập Google của web "
                    + "KHÔNG bị ảnh hưởng.");
        }

        var decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            issuerValidator(properties.allowedIssuers()),
            audienceValidator(allowedAudiences)
        ));
        this.jwtDecoder = decoder;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Mọi lỗi của Nimbus đều quy về MỘT thông báo chung, cố ý. {@link JwtException} phân biệt
     * được "sai chữ ký", "hết hạn", "sai aud" -- nhưng người đang gặp lỗi ở đây là người CẦM token,
     * và nói cho họ biết chính xác phép kiểm nào trượt là chỉ đường dò từng bước. Chi tiết đi vào
     * log, nơi người vận hành đọc được mà kẻ tấn công thì không.
     */
    @Override
    public VerifiedGoogleIdentity verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new UnauthorizedException("Đăng nhập Google thất bại");
        }

        if (allowedAudiences.isEmpty()) {
            LOGGER.error(
                "Từ chối đăng nhập Google: app.oauth2.google.allowed-audiences chưa được cấu hình.");
            throw new UnauthorizedException("Đăng nhập Google chưa được cấu hình trên hệ thống");
        }

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(idToken);
        } catch (JwtException e) {
            LOGGER.warn("ID token Google không hợp lệ: {}", e.getMessage());
            throw new UnauthorizedException("Đăng nhập Google thất bại");
        }

        var email = jwt.getClaimAsString(CLAIM_EMAIL);
        if (email == null || email.isBlank()) {
            // Token hợp lệ nhưng không kèm scope email thì không có gì để tra người dùng.
            LOGGER.warn("ID token Google hợp lệ nhưng thiếu claim email -- client thiếu scope?");
            throw new UnauthorizedException("Đăng nhập Google thất bại");
        }

        return new VerifiedGoogleIdentity(
            jwt.getSubject(),
            email,
            emailVerified(jwt),
            jwt.getClaimAsString(CLAIM_NAME),
            jwt.getClaimAsString(CLAIM_PICTURE)
        );
    }

    /**
     * {@code email_verified} về dây khi thì là boolean JSON, khi thì là chuỗi "true".
     *
     * <p>Không phải giả định phòng xa: đây là khác biệt có thật giữa các đường phát token của Google,
     * và {@code getClaimAsBoolean} trên một chuỗi "true" trả về null. Đọc kiểu đó rồi coi null là
     * false sẽ từ chối những tài khoản hoàn toàn hợp lệ -- một lỗi chỉ xuất hiện với một phần người
     * dùng, tức là loại khó dựng lại nhất.
     */
    private static boolean emailVerified(Jwt jwt) {
        var claim = jwt.getClaim(CLAIM_EMAIL_VERIFIED);
        if (claim instanceof Boolean verified) {
            return verified;
        }
        return claim instanceof String verified && Boolean.parseBoolean(verified);
    }

    /**
     * Google phát {@code iss} theo hai dạng cho cùng một token, có và không có {@code https://}.
     *
     * <p>Vì thế không dùng được {@code JwtIssuerValidator} của Spring: nó nhận đúng MỘT giá trị, và
     * chọn dạng nào cũng từ chối phân nửa số token thật.
     *
     * <p>Đọc claim thô bằng {@code getClaimAsString} chứ KHÔNG bằng {@code jwt.getIssuer()}.
     * {@code getIssuer()} trả về {@link java.net.URL} nên nó ném
     * {@code IllegalArgumentException: Unable to convert claim 'iss' ... to URL} với đúng cái dạng
     * {@code accounts.google.com} (không có scheme) mà hàm này sinh ra để chấp nhận -- tức là phép
     * kiểm sẽ nổ ở chính ca nó tồn tại để xử lý. {@code getClaimAsString} không đụng tới URL nên
     * đúng với cả hai dạng.
     *
     * <p>Để mức gói (không private) để test gọi thẳng được: dựng một {@link Jwt} bằng builder rẻ hơn
     * nhiều so với ký thật một token và host một JWK set chỉ để chốt một phép so chuỗi.
     */
    static OAuth2TokenValidator<Jwt> issuerValidator(List<String> allowedIssuers) {
        return jwt -> allowedIssuers.contains(jwt.getClaimAsString(JwtClaimNames.ISS))
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_issuer", "ID token không do Google phát hành", null));
    }

    /**
     * Phép kiểm QUAN TRỌNG NHẤT của lớp này.
     *
     * <p>Ba phép kiểm kia (chữ ký, iss, hạn dùng) chỉ trả lời "Google có thật sự phát token này
     * không". Chúng KHÔNG trả lời "Google phát nó cho ỨNG DỤNG NÀO". Thiếu phép kiểm aud, một ID
     * token hoàn toàn thật mà Google cấp cho một app bất kỳ của một lập trình viên bất kỳ sẽ qua
     * được cửa này -- nghĩa là bất kỳ ai cũng đăng nhập được vào bất kỳ tài khoản nào, chỉ cần biết
     * email. Đây là lỗ hổng kinh điển của kiểu đăng nhập "gửi ID token lên server".
     *
     * <p>Mức gói, cùng lý do với {@link #issuerValidator(List)} -- và ở đây thì càng đáng: đây là
     * phép kiểm mà một bản refactor vô tình làm hỏng sẽ không gây ra triệu chứng nào cả. Mọi lần
     * đăng nhập thật vẫn chạy đúng; chỉ có cửa là mở toang.
     */
    static OAuth2TokenValidator<Jwt> audienceValidator(List<String> allowedAudiences) {
        return jwt -> jwt.getAudience() != null
                && jwt.getAudience().stream().anyMatch(allowedAudiences::contains)
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_audience", "ID token không dành cho ứng dụng này", null));
    }
}
