package com.sep.vox.infrastructure.security;


import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.domain.model.school.SchoolUser;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final SchoolUserRepository schoolUserRepository;

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
        List<UserRoleInfo> userRoleWithInfo = userRoleQueryRepository.findByUserIdWithRoleInfo(user.getId());
        List<String> roles = userRoleWithInfo
            .stream()
            .map(role -> role.roleCode())
            .toList();
        SchoolUser schoolUser = schoolUserRepository.findByUserId(user.getId())
            .orElse(null);
        UUID schoolId = schoolUser == null ? null : schoolUser.getSchoolId();
        return CustomUserDetails.createFromUser(user, schoolId, roles);
    }
    
}
