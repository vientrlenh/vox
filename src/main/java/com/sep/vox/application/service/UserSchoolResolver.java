package com.sep.vox.application.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class UserSchoolResolver {

    private static final String CURRENT_USER_NOT_FOUND = "Không tìm thấy người dùng hiện tại";
    private static final String CURRENT_USER_INACTIVE = "Người dùng hiện tại không hoạt động";
    private static final String CURRENT_USER_HAS_NO_SCHOOL = "Người dùng hiện tại không thuộc trường nào";
    private static final String TARGET_USER_HAS_NO_SCHOOL = "Người dùng không thuộc trường nào";

    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;

    public UserSchoolResolver(UserRepository userRepository, SchoolUserRepository schoolUserRepository) {
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    public User requireActiveCurrentUser(UUID currentUserId) {
        var user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new NotFoundException(CURRENT_USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException(CURRENT_USER_INACTIVE);
        }
        return user;
    }

    public UUID requireSchoolIdForActiveCurrentUser(UUID currentUserId) {
        requireActiveCurrentUser(currentUserId);
        return requireSchoolId(currentUserId, CURRENT_USER_HAS_NO_SCHOOL);
    }

    public UUID requireSchoolId(UUID userId) {
        return requireSchoolId(userId, TARGET_USER_HAS_NO_SCHOOL);
    }

    public Optional<UUID> findSchoolId(UUID userId) {
        return schoolUserRepository.findByUserId(userId)
            .map(schoolUser -> schoolUser.getSchoolId());
    }

    private UUID requireSchoolId(UUID userId, String message) {
        return findSchoolId(userId)
            .orElseThrow(() -> new IllegalStateException(message));
    }
}
