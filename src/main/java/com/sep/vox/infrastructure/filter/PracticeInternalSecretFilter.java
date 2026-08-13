package com.sep.vox.infrastructure.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Xác thực các endpoint nội bộ Python->Java (/internal/practice-sessions/**) bằng shared secret
 * -- không phải JWT người dùng, vì bên gọi là Python, không phải học sinh đăng nhập. Đường
 * /internal/practice-selection, /internal/practice-generation (đã có từ trước) KHÔNG được filter
 * này bảo vệ -- chỉ áp cho path mới, không đổi hành vi hai đường cũ.
 */
@Component
public class PracticeInternalSecretFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PracticeInternalSecretFilter.class);
    private static final String SECRET_HEADER = "X-Internal-Secret";
    private static final String[] PROTECTED_PREFIXES = {
        "/internal/practice-sessions/",
        // Đường thi cũng chuyển sang cho Python tự upload audio của lượt, thay vì WPF upload rồi
        // gọi POST /turns/archive. Cùng cơ chế bí mật dùng chung, vì bên gọi vẫn là Python.
        "/internal/exam-turns/",
    };

    private final String expectedSecret;

    public PracticeInternalSecretFilter(
            @Value("${practice.internal.secret:}") String expectedSecret) {
        this.expectedSecret = expectedSecret;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var uri = request.getRequestURI();
        var protectedPath = false;
        for (var prefix : PROTECTED_PREFIXES) {
            if (uri.startsWith(prefix)) {
                protectedPath = true;
                break;
            }
        }
        if (!protectedPath) {
            filterChain.doFilter(request, response);
            return;
        }
        var provided = request.getHeader(SECRET_HEADER);
        if (expectedSecret.isBlank() || provided == null || !expectedSecret.equals(provided)) {
            LOGGER.warn("Từ chối gọi endpoint nội bộ luyện tập: {}", request.getRequestURI());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
