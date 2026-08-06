package com.sep.vox.application.port.input.usecase.examcandidate;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.ProctorCandidateSummary;
import com.sep.vox.application.query.repository.ProctorScheduleCandidatesQueryRepository;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class ViewMyProctorScheduleCandidatesUseCase implements IUseCase<UUID, List<ProctorCandidateSummary>> {

    private final ProctorScheduleCandidatesQueryRepository proctorScheduleCandidatesQueryRepository;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamRepository examRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public ViewMyProctorScheduleCandidatesUseCase(
            ProctorScheduleCandidatesQueryRepository proctorScheduleCandidatesQueryRepository,
            ExamScheduleProctorRepository examScheduleProctorRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamRepository examRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.proctorScheduleCandidatesQueryRepository = proctorScheduleCandidatesQueryRepository;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examRepository = examRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProctorCandidateSummary> execute(UUID scheduleId) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!hasAccess(scheduleId, currentUserId)) {
            throw new ForbiddenException("Bạn không phải giám thị của ca thi này");
        }
        var schedule = examScheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
        if (schedule.getStatus() != null && schedule.getStatus().isRemoved()) {
            throw new NotFoundException("Không tìm thấy ca thi");
        }
        return proctorScheduleCandidatesQueryRepository.findByScheduleId(scheduleId);
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
