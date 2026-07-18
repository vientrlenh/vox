package com.sep.vox.application.port.input.usecase.examcandidate;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.ProctorScheduleSummary;
import com.sep.vox.application.query.repository.ProctorScheduleQueryRepository;

@Service
public class ViewMyProctorSchedulesUseCase implements IUseCase<Void, List<ProctorScheduleSummary>> {

    private final ProctorScheduleQueryRepository proctorScheduleQueryRepository;
    private final UserContextPort userContextPort;

    public ViewMyProctorSchedulesUseCase(
            ProctorScheduleQueryRepository proctorScheduleQueryRepository,
            UserContextPort userContextPort) {
        this.proctorScheduleQueryRepository = proctorScheduleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProctorScheduleSummary> execute(Void input) {
        return proctorScheduleQueryRepository.findByTeacherId(userContextPort.getCurrentAuthenticatedUserId());
    }
}
