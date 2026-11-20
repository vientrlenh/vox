package com.sep.vox.domain.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.sep.vox.domain.model.user.Gender;

public record UserDto(
    UUID id, 
    String email, 
    String phone, 
    String fullName, 
    Gender gender,
    LocalDate dateOfBirth, 
    String address, 
    String avatarUrl, 
    Instant createdAt, 
    Instant updatedAt 
) {
}
