package com.sep.vox.application.port.input.usecase.examcandidate;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.ProctorScheduleSummary;
import com.sep.vox.application.query.repository.ProctorScheduleQueryRepository;
import com.sep.vox.application.response.input.exam.ProctorScheduleSummaryResponse;

@Service
public class ViewMyProctorSchedulesUseCase implements IUseCase<Void, List<ProctorScheduleSummaryResponse>> {

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
    public List<ProctorScheduleSummaryResponse> execute(Void input) {
        return proctorScheduleQueryRepository.findByTeacherId(userContextPort.getCurrentAuthenticatedUserId())
            .stream()
            .map(ViewMyProctorSchedulesUseCase::toResponse)
            .toList();
    }

    public static ProctorScheduleSummaryResponse toResponse(ProctorScheduleSummary summary) {
        return new ProctorScheduleSummaryResponse(
            summary.scheduleId(),
            summary.examId(),
            summary.examName(),
            summary.schoolRoomId(),
            summary.roomName(),
            summary.startDate() == null ? null : summary.startDate().toString(),
            summary.endDate() == null ? null : summary.endDate().toString(),
            summary.status()
        );
    }
}
