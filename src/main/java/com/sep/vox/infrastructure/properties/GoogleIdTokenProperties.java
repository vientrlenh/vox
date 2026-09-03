package com.sep.vox.infrastructure.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình kiểm chứng ID token Google cho ứng dụng native.
 *
 * <p>Chỉ có một giá trị thật sự phải đặt theo môi trường: {@code allowed-audiences}. Bộ khoá công
 * khai và danh sách {@code iss} là hằng số của chính Google, không phải thứ mỗi deployment tự chọn,
 * nên chúng nằm ở đây dưới dạng mặc định thay vì thành hai biến môi trường không ai từng đổi -- cùng
 * lối nghĩ với {@link CorsProperties}.
 *
 * @param allowedAudiences các client ID CỦA MÌNH được chấp nhận trong {@code aud}, phân tách bằng
 *                         dấu phẩy qua biến môi trường {@code GOOGLE_ALLOWED_AUDIENCES}.
 *                         <p>Phải liệt kê ĐỦ, và đây là chỗ dễ cấu hình thiếu nhất vì mỗi client
 *                         gửi lên một {@code aud} khác nhau:
 *                         <ul>
 *                           <li>Flutter (Android/iOS) gửi client ID <b>WEB</b> -- đó chính là việc
 *                               mà {@code serverClientId} của {@code google_sign_in} làm, nên nó
 *                               trùng với {@code GOOGLE_CLIENT_ID} của luồng web;</li>
 *                           <li>WPF gửi client ID <b>DESKTOP</b> riêng của nó.</li>
 *                         </ul>
 *                         Đặt đúng một giá trị là một trong hai client luôn bị từ chối.
 * @param jwkSetUri        nơi lấy khoá công khai của Google. Bộ khoá này xoay định kỳ; NimbusJwtDecoder
 *                         tự tải lại nên không cần làm gì thêm.
 * @param allowedIssuers   Google phát {@code iss} theo HAI dạng cho cùng một token -- có và không có
 *                         {@code https://}. Chấp nhận đúng một dạng là từ chối ngẫu nhiên một nửa số
 *                         lần đăng nhập, và triệu chứng ("thỉnh thoảng sai") che mất nguyên nhân.
 */
@ConfigurationProperties(prefix = "app.oauth2.google")
public record GoogleIdTokenProperties(
    List<String> allowedAudiences,
    String jwkSetUri,
    List<String> allowedIssuers
) {

    private static final String DEFAULT_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    private static final List<String> DEFAULT_ISSUERS =
        List.of("https://accounts.google.com", "accounts.google.com");

    public GoogleIdTokenProperties {
        allowedAudiences = trimmed(allowedAudiences);
        jwkSetUri = jwkSetUri == null || jwkSetUri.isBlank() ? DEFAULT_JWK_SET_URI : jwkSetUri.trim();
        var issuers = trimmed(allowedIssuers);
        allowedIssuers = issuers.isEmpty() ? DEFAULT_ISSUERS : issuers;
    }

    /**
     * Bỏ qua phần tử null, không chỉ chuỗi rỗng: một biến môi trường dạng
     * {@code GOOGLE_ALLOWED_AUDIENCES=id-web,,id-desktop} hay một danh sách YAML có mục để trống đều
     * ra được phần tử null, và {@code List.of} phía dưới sẽ ném NPE thay vì bỏ qua nó. Chết lúc khởi
     * động vì một dấu phẩy thừa là một cách hỏng rất khó đoán từ thông báo lỗi.
     */
    private static List<String> trimmed(List<String> values) {
        return values == null
            ? List.of()
            : values.stream()
                .filter(value -> value != null)
                .map(value -> value.trim())
                .filter(value -> !value.isEmpty())
                .toList();
    }
}
