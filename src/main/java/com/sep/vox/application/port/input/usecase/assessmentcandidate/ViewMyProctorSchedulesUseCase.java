package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import java.util.List;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ViewMyProctorSchedulesUseCase implements IUseCase<Void, List<ProctorScheduleSummary>> {

    private final ProctorScheduleQueryRepository proctorScheduleQueryRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public List<ProctorScheduleSummary> execute(Void input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        boolean isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (isSchoolAdmin) {
            UUID schoolId = schoolUserRepository.findByUserId(currentUserId)
                .map(schoolUser -> schoolUser.getSchoolId())
                .orElseThrow(() -> new ForbiddenException("Access denied"));
            return proctorScheduleQueryRepository.findBySchoolId(schoolId);
        }
        return proctorScheduleQueryRepository.findByTeacherId(currentUserId);
    }
}
