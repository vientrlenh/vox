package com.sep.vox.infrastructure.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Danh sách origin được phép gọi API, khai riêng theo từng môi trường.
 *
 * <p>Đây là thứ DUY NHẤT trong cấu hình CORS thay đổi giữa local/staging/production, nên
 * chỉ mình nó được đưa ra ngoài file cấu hình; method, header, credentials, max-age đứng
 * yên trong SecurityConfig dưới dạng hằng số. Đưa nốt chúng ra ngoài chỉ tạo thêm mấy
 * biến môi trường không ai từng đặt, mà lại thêm chỗ để deploy sai.
 *
 * <p>Cố tình KHÔNG nhận wildcard: bean CORS bật {@code allowCredentials} để cookie refresh
 * token đi kèm request, mà origin dạng {@code *} cộng với credentials nghĩa là bất kỳ trang
 * web nào người dùng mở cũng gọi được API bằng chính phiên đăng nhập của họ. Muốn nhiều
 * domain thì liệt kê đủ, đừng rút gọn thành pattern.
 *
 * <p>Giá trị nhận vào là origin theo đúng nghĩa của trình duyệt -- {@code scheme://host[:port]},
 * không có path và không có dấu {@code /} ở cuối. Sai một ký tự là Spring so sánh trượt và
 * trả 403 mà không có header nào để lần ra nguyên nhân, nên chỗ này kiểm tra sẵn lúc khởi
 * động thay vì để lộ ra ngoài production.
 *
 * @param allowedOrigins các origin được phép, phân tách bằng dấu phẩy khi truyền qua biến
 *                       môi trường {@code CORS_ALLOWED_ORIGINS}
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
