package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.school.SchoolUserRole;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;

/**
 * "Ai được xem dữ liệu giám sát của ca thi này" -- một nguồn sự thật cho mọi đường đọc theo ca thi.
 *
 * <p>Tách ra khỏi {@code ViewMyProctorScheduleCandidatesUseCase} khi đường đọc thứ hai xuất hiện
 * (lịch sử cảnh báo). Cùng lý do với {@link AssessmentRecordingAccessService}: chép lại khối kiểm quyền
 * cho người dùng thứ hai nghĩa là sớm muộn hai bản sẽ lệch, và lệch về quyền xem dữ liệu giám sát
 * là loại lỗi không ai nhìn thấy cho tới lúc đã muộn.
 */
@Service
public class ProctorScheduleAccessService {

    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentRepository assessmentRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public ProctorScheduleAccessService(
            AssessmentScheduleProctorRepository assessmentScheduleProctorRepository,
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentRepository assessmentRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.assessmentScheduleProctorRepository = assessmentScheduleProctorRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentRepository = assessmentRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    /**
     * Trả về ca thi nếu người đang đăng nhập được phép giám sát nó, ném lỗi nếu không.
     *
     * <p>Được phép nghĩa là: giám thị được phân công ca này, hoặc school admin của trường sở hữu kỳ
     * thi.
     */
    public AssessmentSchedule requireCanMonitorSchedule(UUID scheduleId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        if (!hasAccess(scheduleId, currentUserId)) {
            throw new ForbiddenException("Bạn không phải giám thị của ca thi này");
        }
        var schedule = assessmentScheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi"));
        if (schedule.getStatus() != null && schedule.getStatus().isRemoved()) {
            throw new ResourceNotFoundException("Không tìm thấy ca thi");
        }
        return schedule;
    }

    private boolean hasAccess(UUID scheduleId, UUID currentUserId) {
        if (assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, currentUserId)) {
            return true;
        }

        var isSchoolAdmin = schoolUserRepository.hasRole(currentUserId, SchoolUserRole.SCHOOL_ADMIN);
        if (!isSchoolAdmin) {
            return false;
        }

        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schedule = assessmentScheduleRepository.findById(scheduleId).orElse(null);
        if (currentSchoolId == null || schedule == null) {
            return false;
        }
        return assessmentRepository.findById(schedule.getAssessmentId())
            .map(assessment -> currentSchoolId.equals(assessment.getSchoolId()))
            .orElse(false);
    }
}
