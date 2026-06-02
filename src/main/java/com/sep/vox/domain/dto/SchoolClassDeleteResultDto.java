package com.sep.vox.domain.dto;

import java.util.UUID;

public record SchoolClassDeleteResultDto(UUID id, String deleteType, String status, String updatedAt) {
}
