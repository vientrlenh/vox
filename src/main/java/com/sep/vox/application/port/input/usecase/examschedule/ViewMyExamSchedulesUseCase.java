package com.sep.vox.application.port.input.usecase.examschedule;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewExamSchedulesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamScheduleDto;
import com.sep.vox.domain.mapper.ExamScheduleDtoMapper;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;

@Service
public class ViewMyExamSchedulesUseCase
        implements IUseCase<ViewExamSchedulesQuery, List<ExamScheduleDto>> {

    private final ExamCandidateRepository examCandidateRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final UserContextPort userContextPort;

    public ViewMyExamSchedulesUseCase(
            ExamCandidateRepository examCandidateRepository,
            ExamScheduleRepository examScheduleRepository,
            UserContextPort userContextPort) {
        this.examCandidateRepository = examCandidateRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamScheduleDto> execute(ViewExamSchedulesQuery input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var candidates = examCandidateRepository.findByStudentId(studentId).stream()
            .filter(candidate -> input.examId() == null || input.examId().equals(candidate.getExamId()))
            .toList();
        if (input.examId() != null && candidates.isEmpty()) {
            throw new ForbiddenException("Bạn không phải là thí sinh của bài kiểm tra này.");
        }

        var scheduleIds = candidates.stream()
            .map(candidate -> candidate.getScheduleId())
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        var schedules = scheduleIds.isEmpty()
            ? List.<ExamSchedule>of()
            : examScheduleRepository.findByIdIn(scheduleIds);
        return ExamScheduleDtoMapper.toDtoList(schedules.stream()
            .filter(schedule -> input.status() == null || input.status() == schedule.getStatus())
            .filter(schedule -> input.startDate() == null
                || (schedule.getStartDate() != null && !schedule.getStartDate().isBefore(input.startDate())))
            .filter(schedule -> input.endDate() == null
                || (schedule.getStartDate() != null && !schedule.getStartDate().isAfter(input.endDate())))
            .toList());
    }
}
