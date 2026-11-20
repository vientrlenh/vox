package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;

/**
 * Kiểm tra phòng cho một ca thi: phòng có thật, thuộc đúng trường của bài kiểm tra, và không đụng
 * ca thi khác trong cùng khoảng thời gian.
 *
 * <p>Gom lại vì ba luồng đều cần đúng một luật: tạo ca thi (kỳ thi tập trung), sửa ca thi, và tạo
 * bài kiểm tra trên lớp.
 */
@Service
public class ExamScheduleRoomValidator {

    private final SchoolRoomRepository schoolRoomRepository;
    private final ExamScheduleRepository examScheduleRepository;

    public ExamScheduleRoomValidator(
            SchoolRoomRepository schoolRoomRepository,
            ExamScheduleRepository examScheduleRepository) {
        this.schoolRoomRepository = schoolRoomRepository;
        this.examScheduleRepository = examScheduleRepository;
    }

    /** Phòng phải tồn tại và thuộc trường của bài kiểm tra. */
    public void requireRoomOfExamSchool(UUID schoolRoomId, Exam exam) {
        var room = schoolRoomRepository.findById(schoolRoomId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phòng học"));
        if (!room.getSchoolId().equals(exam.getSchoolId())) {
            throw new ForbiddenException("Phòng học không thuộc trường của bài kiểm tra");
        }
    }

    /** Phòng không được có ca thi khác chồng lấn; {@code excludeScheduleId} để bỏ qua chính ca đang sửa. */
    public void requireNoOverlap(UUID schoolRoomId, Instant startDate, Instant endDate, UUID excludeScheduleId) {
        if (examScheduleRepository.existsOverlapping(schoolRoomId, startDate, endDate, excludeScheduleId)) {
            throw new DuplicatedException("Phòng học đã có ca thi khác trong khoảng thời gian này");
        }
    }

    /** Hai kiểm tra trên gộp lại cho luồng tạo ca thi mới. */
    public void validateForNewSchedule(UUID schoolRoomId, Exam exam, Instant startDate, Instant endDate) {
        requireRoomOfExamSchool(schoolRoomId, exam);
        requireNoOverlap(schoolRoomId, startDate, endDate, null);
    }
}
