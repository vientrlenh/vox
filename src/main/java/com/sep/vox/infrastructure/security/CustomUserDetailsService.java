package com.sep.vox.infrastructure.security;


import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final SchoolUserRepository schoolUserRepository;

    public CustomUserDetailsService(UserRepository userRepository, UserRoleQueryRepository userRoleQueryRepository, SchoolUserRepository schoolUserRepository) {
        this.userRepository = userRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user;
        if (login.contains("@")) {
            user = userRepository.findByEmail(login)
                .orElseThrow(() -> new UsernameNotFoundException("Thông tin đăng nhập sai"));
        } else {
            user = userRepository.findByPhone(login)
                .orElseThrow(() -> new UsernameNotFoundException("Thông tin đăng nhập sai"));
        } 
        var userRoleWithInfo = userRoleQueryRepository.findByUserIdWithRoleInfo(user.getId());
        var roles = userRoleWithInfo
            .stream()
            .map(role -> role.roleCode())
            .toList();
        var schoolUser = schoolUserRepository.findByUserId(user.getId())
            .orElse(null);
        var schoolId = schoolUser == null ? null : schoolUser.getSchoolId();
        return CustomUserDetails.createFromUser(user, schoolId, roles);
    }
    
}
