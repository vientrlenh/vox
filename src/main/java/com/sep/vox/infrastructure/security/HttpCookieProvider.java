package com.sep.vox.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.CookieManagerPort;

import jakarta.servlet.http.HttpServletResponse;

@Component
public class HttpCookieProvider implements CookieManagerPort {
    
    private final boolean secure;
    private final String sameSite;

    public HttpCookieProvider(@Value("${app.cookie.secure}") boolean secure, @Value("${app.cookie.same-site}") String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    @Override
    public void setCookie(HttpServletResponse response, String key, String value, long ttl) {
        var cookie = ResponseCookie.from(key, value)
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .maxAge(ttl)
            .sameSite(sameSite)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Xoá cookie bằng cách đặt lại chính nó với maxAge = 0.
     *
     * <p>Cố ý gọi lại {@link #setCookie} thay vì tự dựng một {@link ResponseCookie} riêng: trình
     * duyệt chỉ ghi đè cookie khi bộ thuộc tính định danh khớp NGUYÊN VẸN với lúc đặt (path,
     * domain, secure, sameSite). Lệch một cái là nó coi đây là cookie khác, cookie cũ vẫn nằm
     * nguyên và refresh token vẫn dùng được -- kiểu hỏng không để lại dấu vết nào ở phía server,
     * vì response vẫn 200 kèm Set-Cookie trông rất hợp lệ. Đi qua đúng một hàm thì hai bộ thuộc
     * tính không thể lệch nhau.
     */
    @Override
    public void clearCookie(HttpServletResponse response, String key) {
        setCookie(response, key, "", 0);
    }
}
