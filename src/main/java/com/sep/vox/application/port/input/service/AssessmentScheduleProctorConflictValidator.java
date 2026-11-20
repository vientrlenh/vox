package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;

/**
 * A proctor must not supervise two sessions that overlap in time -- a real person cannot be present in two
 * rooms at once. Parallel to {@link AssessmentScheduleRoomValidator}, only the axis changes from room to person.
 *
 * <p>The scope of the check is the WHOLE SCHOOL, every assessment: checking only within the same assessment would still let a school that runs
 * several assessments in parallel create overlaps.
 *
 * <p>Gathered here because four flows all need exactly one rule, and missing any one of them leaves a loophole:
 * adding a proctor to a session, editing the time of a session that already has proctors, moving a session (proctors follow to the target
 * session with a different time window), and creating a class test (the creator is automatically assigned as proctor).
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
            throw new DuplicatedException("Giám thị đã có ca thi khác trong khoảng thời gian này");
        }
    }

    /**
     * Đổi giờ một ca thi: mọi giám thị đang gác ca đó phải còn rảnh ở khung giờ mới.
     *
     * <p>Không có bước này thì luật bị lách dễ dàng — gán giám thị lúc hai ca chưa đụng nhau, rồi
     * dời giờ cho chúng chồng lên.
     */
    public void requireProctorsFreeForNewWindow(UUID scheduleId, Instant start, Instant end) {
        for (AssessmentScheduleProctor proctor : assessmentScheduleProctorRepository.findByScheduleId(scheduleId)) {
            requireTeacherFree(proctor.getTeacherId(), start, end, scheduleId);
        }
    }
}
