package com.sep.vox.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tài khoản HTTP Basic canh cửa toàn bộ {@code /admin/**} -- vừa là trang Spring Boot Admin,
 * vừa là endpoint {@code POST /admin/instances} mà SBA client gọi để tự đăng ký.
 *
 * <p>Cố tình KHÔNG có giá trị mặc định. Sau {@code /admin/**} là toàn bộ actuator của mọi
 * instance đã đăng ký (health kèm chi tiết, env, heapdump) và cả đường proxy GHI xuống chúng --
 * xem {@code InstancesProxyController}, nó nhận cả POST/PUT/PATCH/DELETE. Một mật khẩu mặc
 * định lọt vào đây là mở nguyên bộ đó cho bất kỳ ai chạm được tới cổng, mà app vẫn khởi động
 * im lặng như không có gì.
 *
 * <p>Mật khẩu để dạng THÔ, không phải hash. Chính process này vừa là server vừa là client, nên
 * SBA client bắt buộc phải có bản thô để gắn vào header Basic
 * ({@code spring.boot.admin.client.password}). Giữ thêm một bản hash cho phía server không che
 * giấu được gì -- bản thô vẫn nằm ngay cạnh trong cùng file -- mà lại thành hai giá trị phải
 * nhớ sửa cùng lúc. Phía server tự băm bằng BCrypt lúc khởi động, xem
 * {@code SecurityConfig#adminConsoleFilterChain}.
 *
 * @param username tên đăng nhập, đặt qua biến môi trường {@code SBA_ADMIN_USERNAME}
 * @param password mật khẩu thô, đặt qua biến môi trường {@code SBA_ADMIN_PASSWORD}
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminConsoleProperties(String username, String password) {
}
