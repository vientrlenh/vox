package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;

/**
 * Kiểm tra phòng cho một ca thi: phòng có thật, thuộc đúng trường của bài kiểm tra, và không đụng
 * ca thi khác trong cùng khoảng thời gian.
 *
 * <p>Gom lại vì ba luồng đều cần đúng một luật: tạo ca thi (kỳ thi tập trung), sửa ca thi, và tạo
 * bài kiểm tra trên lớp.
 */
@Service
public class AssessmentScheduleRoomValidator {

    private final SchoolRoomRepository schoolRoomRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;

    public AssessmentScheduleRoomValidator(
            SchoolRoomRepository schoolRoomRepository,
            AssessmentScheduleRepository assessmentScheduleRepository) {
        this.schoolRoomRepository = schoolRoomRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
    }

    /** Phòng phải tồn tại và thuộc trường của bài kiểm tra. */
    public void requireRoomOfAssessmentSchool(UUID schoolRoomId, Assessment assessment) {
        var room = schoolRoomRepository.findById(schoolRoomId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng học"));
        if (!room.getSchoolId().equals(assessment.getSchoolId())) {
            throw new ForbiddenException("Phòng học không thuộc trường của bài kiểm tra");
        }
    }

    /** Phòng không được có ca thi khác chồng lấn; {@code excludeScheduleId} để bỏ qua chính ca đang sửa. */
    public void requireNoOverlap(UUID schoolRoomId, Instant startDate, Instant endDate, UUID excludeScheduleId) {
        if (assessmentScheduleRepository.existsOverlapping(schoolRoomId, startDate, endDate, excludeScheduleId)) {
            throw new ResourceDuplicatedException("Phòng học đã có ca thi khác trong khoảng thời gian này");
        }
    }

    /** Hai kiểm tra trên gộp lại cho luồng tạo ca thi mới. */
    public void validateForNewSchedule(UUID schoolRoomId, Assessment assessment, Instant startDate, Instant endDate) {
        requireRoomOfAssessmentSchool(schoolRoomId, assessment);
        requireNoOverlap(schoolRoomId, startDate, endDate, null);
    }
}
