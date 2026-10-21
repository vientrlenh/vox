package com.sep.vox.domain.dto.assessment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;

public record AssessmentCandidateDto(
    UUID id,
    UUID assessmentId,
    UUID studentId,
    UUID assignedPaperId,
    UUID scheduleId,
    AssessmentCandidateStatus status,
    Instant assignedAt,
    Instant updatedAt,
    Instant blockedAt
) {

    public static AssessmentCandidateDto toDto(AssessmentCandidate assessmentCandidate) {
        return new AssessmentCandidateDto(
            assessmentCandidate.getId(),
            assessmentCandidate.getAssessmentId(),
            assessmentCandidate.getStudentId(),
            assessmentCandidate.getAssignedPaperId(),
            assessmentCandidate.getScheduleId(),
            assessmentCandidate.getStatus(),
            assessmentCandidate.getAssignedAt(),
            assessmentCandidate.getUpdatedAt(),
            assessmentCandidate.getBlockedAt()
        );
    }

    public static List<AssessmentCandidateDto> toDtoList(List<AssessmentCandidate> domains) {
        return domains.stream()
            .map(AssessmentCandidateDto::toDto)
            .toList();
    }
}
