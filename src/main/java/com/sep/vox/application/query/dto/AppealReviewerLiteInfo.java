package com.sep.vox.application.query.dto;

import java.util.UUID;

public record AppealReviewerLiteInfo(
    UUID id,
    String name,
    long load
) {
}
