package com.sep.vox.infrastructure.security;


import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.projection.repository.SchoolUserProjectionRepository;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final SchoolUserProjectionRepository schoolUserProjectionRepository;

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
        if (!user.isSystemAdmin()) {
            schoolId = schoolUserProjectionRepository.findSchoolIdByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User does not belong to any school"));
        }
        return CustomUserDetails.createFromUser(user, schoolId, user.roleStrs());
    }
    
}
