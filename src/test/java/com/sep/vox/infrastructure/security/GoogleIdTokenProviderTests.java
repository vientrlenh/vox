package com.sep.vox.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.infrastructure.properties.GoogleIdTokenProperties;

/**
 * Cửa kiểm ID token của đường đăng nhập Google từ ứng dụng native.
 *
 * <p>Trọng tâm là hai phép kiểm mà Spring KHÔNG làm hộ ở đường này, và cả hai đều thuộc loại hỏng
 * mà không có triệu chứng: mọi lần đăng nhập thật vẫn chạy đúng như cũ, chỉ có cửa là mở.
 */
class GoogleIdTokenProviderTests {

    private static final String WEB_CLIENT_ID = "111-web.apps.googleusercontent.com";
    private static final String DESKTOP_CLIENT_ID = "222-desktop.apps.googleusercontent.com";

    /**
     * Phép kiểm quan trọng nhất của cả lớp.
     *
     * <p>Token trong test này là token THẬT theo mọi nghĩa còn lại -- Google ký, Google phát, còn
     * hạn -- chỉ khác ở chỗ nó được cấp cho ứng dụng của người khác. Không có phép kiểm {@code aud}
     * thì chữ ký, {@code iss} và hạn dùng đều nói "hợp lệ", và bất kỳ ai có một app Google bất kỳ
     * cũng đăng nhập được vào mọi tài khoản chỉ cần biết email.
     */
    @Test
    void should_reject_a_token_minted_for_someone_elses_google_app() {
        var validator = GoogleIdTokenProvider.audienceValidator(List.of(WEB_CLIENT_ID, DESKTOP_CLIENT_ID));

        var result = validator.validate(jwt().audience(List.of("999-attacker.apps.googleusercontent.com")).build());

        assertThat(result.hasErrors()).isTrue();
    }

    /** Cả hai client đều phải qua: Flutter gửi aud của client WEB, WPF gửi aud của client DESKTOP. */
    @Test
    void should_accept_every_configured_client_id() {
        var validator = GoogleIdTokenProvider.audienceValidator(List.of(WEB_CLIENT_ID, DESKTOP_CLIENT_ID));

        assertThat(validator.validate(jwt().audience(List.of(WEB_CLIENT_ID)).build()).hasErrors()).isFalse();
        assertThat(validator.validate(jwt().audience(List.of(DESKTOP_CLIENT_ID)).build()).hasErrors()).isFalse();
    }

    /** Chưa cấu hình thì không có client nào hợp lệ -- KHÔNG phải "mọi client đều hợp lệ". */
    @Test
    void should_reject_everything_when_no_audience_is_configured() {
        var validator = GoogleIdTokenProvider.audienceValidator(List.of());

        assertThat(validator.validate(jwt().audience(List.of(WEB_CLIENT_ID)).build()).hasErrors()).isTrue();
    }

    /**
     * Google phát {@code iss} theo HAI dạng cho cùng một token, có và không có {@code https://}.
     * Chấp nhận đúng một dạng là từ chối ngẫu nhiên một phần số lần đăng nhập thật -- triệu chứng
     * ("thỉnh thoảng sai") che mất nguyên nhân.
     */
    @Test
    void should_accept_both_issuer_spellings_google_actually_uses() {
        var validator = GoogleIdTokenProvider.issuerValidator(
            List.of("https://accounts.google.com", "accounts.google.com"));

        assertThat(validator.validate(jwt().issuer("https://accounts.google.com").build()).hasErrors()).isFalse();
        assertThat(validator.validate(jwt().issuer("accounts.google.com").build()).hasErrors()).isFalse();
    }

    @Test
    void should_reject_a_token_from_another_issuer() {
        var validator = GoogleIdTokenProvider.issuerValidator(List.of("https://accounts.google.com"));

        assertThat(validator.validate(jwt().issuer("https://accounts.evil.example").build()).hasErrors()).isTrue();
    }

    /**
     * Chưa cấu hình aud thì từ chối NGAY, trước cả khi chạm tới Google.
     *
     * <p>Không phải tối ưu tốc độ: nếu để token đi tiếp thì phép kiểm aud với danh sách rỗng vẫn
     * trượt, nhưng người vận hành sẽ đọc được "token không hợp lệ" cho một token hoàn toàn hợp lệ.
     * Đây là lỗi CẤU HÌNH và thông báo phải nói đúng như vậy.
     */
    @Test
    void should_fail_loudly_when_the_feature_is_not_configured() {
        var provider = new GoogleIdTokenProvider(properties(List.of()));

        assertThatThrownBy(() -> provider.verify("bat-ky-chuoi-nao"))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("chưa được cấu hình");
    }

    /** Token rỗng không đáng một vòng mạng ra Google. */
    @Test
    void should_reject_a_blank_token_without_calling_google() {
        var provider = new GoogleIdTokenProvider(properties(List.of(WEB_CLIENT_ID)));

        assertThatThrownBy(() -> provider.verify("  ")).isInstanceOf(UnauthorizedException.class);
    }

    /**
     * iss/jwk-set-uri là hằng số của Google, không phải thứ mỗi môi trường tự đặt.
     *
     * <p>Danh sách đầu vào cố ý bẩn: {@code Arrays.asList} chứ không phải {@code List.of} vì nó CHO
     * PHÉP null, đúng thứ mà một biến môi trường thừa dấu phẩy sinh ra.
     */
    @Test
    void should_default_the_google_constants_so_only_audiences_need_configuring() {
        var properties = new GoogleIdTokenProperties(
            Arrays.asList(" " + WEB_CLIENT_ID + " ", "", null), null, null);

        assertThat(properties.allowedAudiences()).containsExactly(WEB_CLIENT_ID);
        assertThat(properties.jwkSetUri()).isEqualTo("https://www.googleapis.com/oauth2/v3/certs");
        assertThat(properties.allowedIssuers())
            .containsExactly("https://accounts.google.com", "accounts.google.com");
    }

    private static GoogleIdTokenProperties properties(List<String> audiences) {
        return new GoogleIdTokenProperties(audiences, null, null);
    }

    /**
     * Dựng {@link Jwt} bằng builder thay vì ký thật: hai validator đang test chỉ đọc claim, nên một
     * cặp khoá RSA và một JWK set chạy tại chỗ chỉ để chốt một phép so chuỗi là công vô ích.
     */
    private static Jwt.Builder jwt() {
        var now = Instant.now();
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("google-sub-1")
            .issuer("https://accounts.google.com")
            .audience(List.of(WEB_CLIENT_ID))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(3600));
    }
}
