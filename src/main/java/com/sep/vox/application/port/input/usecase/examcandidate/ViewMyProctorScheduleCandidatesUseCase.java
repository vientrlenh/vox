package com.sep.vox.application.port.input.usecase.examcandidate;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.ProctorCandidateSummary;
import com.sep.vox.application.query.repository.ProctorScheduleCandidatesQueryRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;

@Service
public class ViewMyProctorScheduleCandidatesUseCase implements IUseCase<UUID, List<ProctorCandidateSummary>> {

    private final ProctorScheduleCandidatesQueryRepository proctorScheduleCandidatesQueryRepository;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final UserContextPort userContextPort;

    public ViewMyProctorScheduleCandidatesUseCase(
            ProctorScheduleCandidatesQueryRepository proctorScheduleCandidatesQueryRepository,
            ExamScheduleProctorRepository examScheduleProctorRepository,
            UserContextPort userContextPort) {
        this.proctorScheduleCandidatesQueryRepository = proctorScheduleCandidatesQueryRepository;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProctorCandidateSummary> execute(UUID scheduleId) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!examScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, currentUserId)) {
            throw new ForbiddenException("Bạn không phải giám thị của ca thi này");
        }
        return proctorScheduleCandidatesQueryRepository.findByScheduleId(scheduleId);
    }
}
