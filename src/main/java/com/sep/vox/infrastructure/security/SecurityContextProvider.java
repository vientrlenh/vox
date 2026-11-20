package com.sep.vox.infrastructure.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.UserContextPort;

@Component
public class SecurityContextProvider implements UserContextPort {

    @Override
    public UUID getCurrentAuthenticatedUserId() {
        var authentication = getAuthentication();
        var userDetails = getUserDetails(authentication);
        return userDetails.getId();
    }

    /**
     * Bản KHÔNG ném lỗi của {@link #getCurrentAuthenticatedUserId()}, dành cho luồng phải chạy
     * được cả khi người gọi chưa/không còn đăng nhập -- hiện là đăng xuất.
     *
     * <p>Access token sống 15 phút, nên một phiên bị bỏ quên gần như luôn gọi /logout với token
     * đã hết hạn: {@code JwtAuthenticationFilter} nuốt lỗi token và cho request đi tiếp dưới danh
     * nghĩa anonymous. Ném lỗi ở đó thì đúng cái phiên cần thu hồi nhất lại là cái không thu hồi
     * được.
     */
    @Override
    public Optional<UUID> findCurrentAuthenticatedUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return Optional.empty();
        }

        return Optional.of(userDetails.getId());
    }

    @Override
    public UUID getCurrentSchoolId() {
        var authentication = getAuthentication();
        var userDetails = getUserDetails(authentication);
        return userDetails.getSchoolId();
    }

    @Override
    public boolean isSystemAdmin() {
        var authentication = getAuthentication();
        var userDetails = getUserDetails(authentication);
        return isRoleMatch(userDetails, "ROLE_SYSTEM_ADMIN");
    }

    
    @Override
    public boolean isSchoolAdmin() {
        var authentication = getAuthentication();
        var userDetails = getUserDetails(authentication);
        return isRoleMatch(userDetails, "ROLE_SCHOOL_ADMIN");
    }

    @Override
    public boolean isTeacher() {
         var authentication = getAuthentication();
        var userDetails = getUserDetails(authentication);
        return isRoleMatch(userDetails, "ROLE_TEACHER");
    }

    private Authentication getAuthentication() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("Người dùng hiện không đăng nhập");
        }
        return authentication;
    }
    
    private CustomUserDetails getUserDetails(Authentication authentication) {
        var principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new AuthenticationCredentialsNotFoundException("Nguyên tắc xác thực không hợp lệ");
        }
        return userDetails;
    }

    private boolean isRoleMatch(CustomUserDetails userDetails, String role) {
        var authorities = userDetails.getAuthorities();
        return authorities.stream()
            .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
