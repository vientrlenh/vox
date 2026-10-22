package com.sep.vox.infrastructure.security;


import java.util.ArrayList;
import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user;
        if (login.contains("@")) {
            user = userRepository.findByEmail(login)
                .orElseThrow(() -> new UsernameNotFoundException("Thông tin đăng nhập sai"));
        } else {
            user = userRepository.findByPhone(login)
                .orElseThrow(() -> new UsernameNotFoundException("Thông tin đăng nhập sai"));
        } 
        UUID schoolId = null;
        // Authority gồm cả vai trò toàn hệ thống (UserRole) lẫn vai trò trong trường (SchoolUserRole),
        // để hasRole('SCHOOL_ADMIN') và UserContextPort.isSchoolAdmin()/isTeacher() cùng đọc được.
        var roles = new ArrayList<>(user.roleStrs());
        if (!user.isSystemAdmin()) {
            var schoolUser = schoolUserRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User does not belong to any school"));
            schoolId = schoolUser.getSchoolId();
            if (schoolUser.getRole() != null) {
                roles.add(schoolUser.getRole().name());
            }
        }
        return CustomUserDetails.createFromUser(user, schoolId, roles);
    }
    
}
