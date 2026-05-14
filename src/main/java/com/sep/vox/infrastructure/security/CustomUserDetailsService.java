package com.sep.vox.infrastructure.security;

import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user;
        if (username.contains("@")) {
            user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Thông tin đăng nhập sai"));
        } else if (username.matches("^\\d+$")) {
            user = userRepository.findByPhone(username)
                .orElseThrow(() -> new UsernameNotFoundException("Thông tin đăng nhập sai"));
        } else if (username.contains("-")) {
            var userId = UUID.fromString(username);
            user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Thông tin đăng nhập sai"));
        } else {
            user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Thông tin đăng nhập sai"));
        }
        return CustomUserDetails.createFromUser(user);
    }
    
}
