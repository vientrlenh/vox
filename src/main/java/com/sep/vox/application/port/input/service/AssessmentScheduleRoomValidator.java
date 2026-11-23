package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.school.SchoolRoom;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;

/**
 * Kiểm tra phòng cho một ca thi: phòng có thật, thuộc đúng trường của bài kiểm tra, và không đụng
 * ca thi khác trong cùng khoảng thời gian.
 *
 * <p>Gom lại vì ba luồng đều cần đúng một luật: tạo ca thi (kỳ thi tập trung), sửa ca thi, và tạo
 * bài kiểm tra trên lớp.
 */
@Service
@RequiredArgsConstructor
public class AssessmentScheduleRoomValidator {

    private final SchoolRoomRepository schoolRoomRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;

    /** Phòng phải tồn tại và thuộc trường của bài kiểm tra. */
    public void requireRoomOfAssessmentSchool(UUID schoolRoomId, Assessment assessment) {
        SchoolRoom room = schoolRoomRepository.findById(schoolRoomId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phòng học"));
        if (!room.getSchoolId().equals(assessment.getSchoolId())) {
            throw new ForbiddenException("Phòng học không thuộc trường của bài kiểm tra");
        }
    }

    /** Phòng không được có ca thi khác chồng lấn; {@code excludeScheduleId} để bỏ qua chính ca đang sửa. */
    public void requireNoOverlap(UUID schoolRoomId, Instant startDate, Instant endDate, UUID excludeScheduleId) {
        if (assessmentScheduleRepository.existsOverlapping(schoolRoomId, startDate, endDate, excludeScheduleId)) {
            throw new DuplicatedException("Phòng học đã có ca thi khác trong khoảng thời gian này");
        }
    }

    /** Hai kiểm tra trên gộp lại cho luồng tạo ca thi mới. */
    public void validateForNewSchedule(UUID schoolRoomId, Assessment assessment, Instant startDate, Instant endDate) {
        requireRoomOfAssessmentSchool(schoolRoomId, assessment);
        requireNoOverlap(schoolRoomId, startDate, endDate, null);
    }
}
