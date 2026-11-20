package com.sep.vox.application.port.input.usecase.examcandidate;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.ProctorScheduleSummary;
import com.sep.vox.application.query.repository.ProctorScheduleQueryRepository;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class ViewMyProctorSchedulesUseCase implements IUseCase<Void, List<ProctorScheduleSummary>> {

    private final ProctorScheduleQueryRepository proctorScheduleQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public ViewMyProctorSchedulesUseCase(
            ProctorScheduleQueryRepository proctorScheduleQueryRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.proctorScheduleQueryRepository = proctorScheduleQueryRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProctorScheduleSummary> execute(Void input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (isSchoolAdmin) {
            var schoolId = schoolUserRepository.findByUserId(currentUserId)
                .map(schoolUser -> schoolUser.getSchoolId())
                .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));
            return proctorScheduleQueryRepository.findBySchoolId(schoolId);
        }
        return proctorScheduleQueryRepository.findByTeacherId(currentUserId);
    }
}
