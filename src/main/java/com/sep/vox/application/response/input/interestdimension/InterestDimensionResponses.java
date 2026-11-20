package com.sep.vox.application.response.input.interestdimension;

import java.time.Instant;

public final class InterestDimensionResponses {

    private InterestDimensionResponses() {
    }

    public record InterestDimensionResponse(
            String code,
            String label,
            String description,
            boolean active,
            boolean quizEligible,
            int displayOrder,
            Instant createdAt,
            Instant updatedAt) {
    }
}
