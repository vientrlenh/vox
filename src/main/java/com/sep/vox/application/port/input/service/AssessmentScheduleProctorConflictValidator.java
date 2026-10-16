package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleProctorRepository;

/**
 * Một giám thị không được gác hai ca thi chồng lấn thời gian — người thật không thể có mặt ở hai
 * phòng cùng lúc. Song song với {@link AssessmentScheduleRoomValidator}, chỉ đổi trục từ phòng sang người.
 *
 * <p>Phạm vi kiểm tra là TOÀN TRƯỜNG, mọi kỳ thi: chỉ soát trong cùng một kỳ thi thì trường chạy
 * nhiều kỳ thi song song vẫn xếp trùng được.
 *
 * <p>Gom lại vì bốn luồng đều cần đúng một luật, và thiếu bất kỳ luồng nào là còn lối lách:
 * thêm giám thị vào ca, sửa giờ ca đã có giám thị, dời ca (giám thị theo sang ca đích có khung giờ
 * khác), và tạo bài kiểm tra trên lớp (tự gán người tạo làm giám thị).
 */
@Service
public class AssessmentScheduleProctorConflictValidator {

    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;

    public AssessmentScheduleProctorConflictValidator(AssessmentScheduleProctorRepository assessmentScheduleProctorRepository) {
        this.assessmentScheduleProctorRepository = assessmentScheduleProctorRepository;
    }

    /**
     * Giáo viên phải rảnh trong khoảng [start, end); {@code excludeScheduleId} để bỏ qua chính ca
     * đang thao tác.
     */
    public void requireTeacherFree(UUID teacherId, Instant start, Instant end, UUID excludeScheduleId) {
        if (assessmentScheduleProctorRepository.existsOverlappingAssignment(teacherId, start, end, excludeScheduleId)) {
            throw new ResourceDuplicatedException("Giám thị đã có ca thi khác trong khoảng thời gian này");
        }
    }

    /**
     * Đổi giờ một ca thi: mọi giám thị đang gác ca đó phải còn rảnh ở khung giờ mới.
     *
     * <p>Không có bước này thì luật bị lách dễ dàng — gán giám thị lúc hai ca chưa đụng nhau, rồi
     * dời giờ cho chúng chồng lên.
     */
    public void requireProctorsFreeForNewWindow(UUID scheduleId, Instant start, Instant end) {
        for (var proctor : assessmentScheduleProctorRepository.findByScheduleId(scheduleId)) {
            requireTeacherFree(proctor.getTeacherId(), start, end, scheduleId);
        }
    }
}
