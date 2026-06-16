package com.sep.vox.application.response.input.schoolclass;

import java.util.UUID;

public record DeleteSchoolClassResponse(UUID id, String deleteType, String status, String updatedAt) {
}
