package com.sep.vox.domain.dto;

import java.util.UUID;

public record UserDto(
    UUID id, 
    String email, 
    String phone, 
    String fullName, 
    String gender, 
    String dateOfBirth, 
    String address, 
    String avatarUrl, 
    String createdAt, 
    String updatedAt 
) {
}
