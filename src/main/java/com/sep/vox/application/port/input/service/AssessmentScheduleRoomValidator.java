package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.school.SchoolRoom;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;

/**
 * Validates the room for an assessment schedule: the room exists, belongs to the school of the assessment, and does not clash with
 * another schedule in the same time range.
 *
 * <p>Gathered here because three flows need exactly one rule: creating a schedule (centralized assessment), editing a schedule, and creating
 * a class test.
 */
@Service
@RequiredArgsConstructor
public class AssessmentScheduleRoomValidator {

    private final SchoolRoomRepository schoolRoomRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;

    /** The room must exist and belong to the school of the assessment. */
    public void requireRoomOfAssessmentSchool(UUID schoolRoomId, Assessment assessment) {
        SchoolRoom room = schoolRoomRepository.findById(schoolRoomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        if (!room.getSchoolId().equals(assessment.getSchoolId())) {
            throw new ForbiddenException("The room does not belong to the school of the assessment");
        }
    }

    /** The room must not have another schedule overlapping; {@code excludeScheduleId} skips the schedule being edited. */
    public void requireNoOverlap(UUID schoolRoomId, Instant startDate, Instant endDate, UUID excludeScheduleId) {
        if (assessmentScheduleRepository.existsOverlapping(schoolRoomId, startDate, endDate, excludeScheduleId)) {
            throw new ResourceDuplicatedException("The room already has another assessment schedule in this time range");
        }
    }

    /** The two checks above combined for the create-new-schedule flow. */
    public void validateForNewSchedule(UUID schoolRoomId, Assessment assessment, Instant startDate, Instant endDate) {
        requireRoomOfAssessmentSchool(schoolRoomId, assessment);
        requireNoOverlap(schoolRoomId, startDate, endDate, null);
    }
}
