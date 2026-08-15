package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * "Ai được xem dữ liệu giám sát của ca thi này" -- một nguồn sự thật cho mọi đường đọc theo ca thi.
 *
 * <p>Tách ra khỏi {@code ViewMyProctorScheduleCandidatesUseCase} khi đường đọc thứ hai xuất hiện
 * (lịch sử cảnh báo). Cùng lý do với {@link ExamRecordingAccessService}: chép lại khối kiểm quyền
 * cho người dùng thứ hai nghĩa là sớm muộn hai bản sẽ lệch, và lệch về quyền xem dữ liệu giám sát
 * là loại lỗi không ai nhìn thấy cho tới lúc đã muộn.
 */
@Service
public class ProctorScheduleAccessService {

    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamRepository examRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public ProctorScheduleAccessService(
            ExamScheduleProctorRepository examScheduleProctorRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamRepository examRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examRepository = examRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    /**
     * Trả về ca thi nếu người đang đăng nhập được phép giám sát nó, ném lỗi nếu không.
     *
     * <p>Được phép nghĩa là: giám thị được phân công ca này, hoặc school admin của trường sở hữu kỳ
     * thi.
     */
    public ExamSchedule requireCanMonitorSchedule(UUID scheduleId) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!hasAccess(scheduleId, currentUserId)) {
            throw new ForbiddenException("Bạn không phải giám thị của ca thi này");
        }
        var schedule = examScheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
        if (schedule.getStatus() != null && schedule.getStatus().isRemoved()) {
            throw new NotFoundException("Không tìm thấy ca thi");
        }
        return schedule;
    }

    private boolean hasAccess(UUID scheduleId, UUID currentUserId) {
        if (examScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, currentUserId)) {
            return true;
        }

        var isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (!isSchoolAdmin) {
            return false;
        }

        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schedule = examScheduleRepository.findById(scheduleId).orElse(null);
        if (currentSchoolId == null || schedule == null) {
            return false;
        }
        return examRepository.findById(schedule.getExamId())
            .map(exam -> currentSchoolId.equals(exam.getSchoolId()))
            .orElse(false);
    }
}
