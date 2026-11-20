package com.sep.vox.application.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Luật hợp lệ cho URL ảnh đại diện. CỐ Ý không biết ảnh được lưu ở đâu.
 *
 * <p>Ảnh đại diện không đi qua {@code StoragePort}/S3 như asset câu hỏi: client tự tải lên kho của
 * nó (hiện là Firebase Storage -- xem {@code vox-client-web/src/shared/firebase/uploadToStorage.ts})
 * rồi gửi về backend đúng một chuỗi URL, backend chỉ lưu tham chiếu. Nên chỗ này KHÔNG nhúng định
 * dạng URL của riêng nhà cung cấp nào: đổi kho lưu trữ thì sửa cấu hình, không phải sửa code.
 *
 * <p>Hai mức kiểm, tách bạch vì lý do khác nhau:
 *
 * <p><b>1. Scheme -- luôn bật, không tắt được.</b> {@code avatar_url} chảy thẳng vào
 * {@code <img src>} trên máy người khác. Chuỗi tự do không kiểm scheme cho phép {@code javascript:},
 * {@code data:} (SVG nhúng script) hay {@code blob:} lọt vào DB và nằm đó chờ một chỗ render sơ ý.
 * Bắt buộc {@code https} cũng loại luôn {@code http}, thứ sẽ bị chặn vì mixed content khi web chạy
 * https -- lỗi đó hiện ra dưới dạng "ảnh không lên" rất khó lần.
 *
 * <p><b>2. Allowlist host -- tùy chọn, cấu hình bằng {@code app.avatar.allowed-hosts}.</b> Không
 * giới hạn host thì người dùng trỏ ảnh sang máy chủ lạ, và MỌI giáo viên mở danh sách chấm hay màn
 * điểm danh sẽ tự gọi sang đó: lộ IP, lộ thời điểm xem, và chủ host đổi được nội dung ảnh sau lưng.
 * Để trống là chấp nhận đánh đổi đó một cách có ý thức (tiện lúc dev), nên môi trường thật hãy điền
 * host của kho đang dùng.
 */
public final class AvatarUrlPolicy {

    /** Bằng độ dài cột {@code users.avatar_url} (varchar 4096) -- chặn sớm để không ăn lỗi DB. */
    public static final int MAX_URL_LENGTH = 4096;

    private AvatarUrlPolicy() {
    }

    /**
     * Chuẩn hóa URL client gửi lên thành giá trị ghi được vào cột.
     *
     * @param allowedHosts danh sách host ngăn cách bởi dấu phẩy, lấy từ {@code app.avatar.allowed-hosts}.
     *                     Rỗng = không giới hạn host (chỉ còn kiểm scheme).
     * @param rawUrl       URL client gửi. {@code null}/rỗng mang nghĩa GỠ ảnh, trả về {@code null}.
     * @return URL đã cắt khoảng trắng, hoặc {@code null} nếu là yêu cầu gỡ ảnh.
     */
    public static String normalizeOrThrow(String allowedHosts, String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        var normalized = rawUrl.trim();
        if (normalized.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException("Đường dẫn ảnh đại diện quá dài");
        }

        var host = httpsHostOf(normalized);
        var hosts = parseHosts(allowedHosts);
        if (!hosts.isEmpty() && !hosts.contains(host)) {
            throw new IllegalArgumentException("Ảnh đại diện phải được tải lên vùng lưu trữ của hệ thống");
        }

        return normalized;
    }

    /** Trả về host, đồng thời ép URL phải là https tuyệt đối và có host. */
    private static String httpsHostOf(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Đường dẫn ảnh đại diện không hợp lệ");
        }

        var scheme = uri.getScheme();
        var host = uri.getHost();
        if (scheme == null || !scheme.toLowerCase(Locale.ROOT).equals("https") || host == null || host.isBlank()) {
            throw new IllegalArgumentException("Đường dẫn ảnh đại diện phải là URL https hợp lệ");
        }

        return host.toLowerCase(Locale.ROOT);
    }

    private static List<String> parseHosts(String allowedHosts) {
        if (allowedHosts == null || allowedHosts.isBlank()) {
            return List.of();
        }
        return Arrays.stream(allowedHosts.split(","))
            .map(s -> s.trim())
            .filter(value -> !value.isBlank())
            .map(value -> value.toLowerCase(Locale.ROOT))
            .toList();
    }
}
