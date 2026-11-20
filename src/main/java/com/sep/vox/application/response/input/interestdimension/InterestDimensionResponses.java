package com.sep.vox.application.response.input.interestdimension;

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
            String createdAt,
            String updatedAt) {
    }
}
