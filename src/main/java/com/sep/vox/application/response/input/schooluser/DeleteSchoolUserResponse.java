package com.sep.vox.application.response.input.schooluser;

import java.util.UUID;

public record DeleteSchoolUserResponse(
    UUID id, 
    String deleteType, 
    String status, 
    String updatedAt) {
}
