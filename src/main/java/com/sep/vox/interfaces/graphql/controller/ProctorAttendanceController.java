package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.usecase.examcandidate.ViewMyProctorScheduleCandidatesUseCase;
import com.sep.vox.application.port.input.usecase.examcandidate.ViewMyProctorSchedulesUseCase;
import com.sep.vox.application.response.input.exam.ProctorCandidateSummaryResponse;
import com.sep.vox.application.response.input.exam.ProctorScheduleSummaryResponse;

@Controller("graphqlProctorAttendanceController")
public class ProctorAttendanceController {

    private final ViewMyProctorSchedulesUseCase viewMyProctorSchedulesUseCase;
    private final ViewMyProctorScheduleCandidatesUseCase viewMyProctorScheduleCandidatesUseCase;

    public ProctorAttendanceController(
            ViewMyProctorSchedulesUseCase viewMyProctorSchedulesUseCase,
            ViewMyProctorScheduleCandidatesUseCase viewMyProctorScheduleCandidatesUseCase) {
        this.viewMyProctorSchedulesUseCase = viewMyProctorSchedulesUseCase;
        this.viewMyProctorScheduleCandidatesUseCase = viewMyProctorScheduleCandidatesUseCase;
    }

    @QueryMapping
    @PreAuthorize("hasRole('TEACHER')")
    public List<ProctorScheduleSummaryResponse> myProctorSchedules() {
        return viewMyProctorSchedulesUseCase.execute(null);
    }

    @QueryMapping
    @PreAuthorize("hasRole('TEACHER')")
    public List<ProctorCandidateSummaryResponse> myProctorScheduleCandidates(
            @Argument(name = "scheduleId") UUID scheduleId) {
        return viewMyProctorScheduleCandidatesUseCase.execute(scheduleId);
    }
}
