package com.sep.vox.application.port.input.usecase.assessmentschedule;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewAssessmentSchedulesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.assessment.AssessmentScheduleDto;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;

@Service
public class ViewMyAssessmentSchedulesUseCase
        implements IUseCase<ViewAssessmentSchedulesQuery, List<AssessmentScheduleDto>> {

    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentRepository assessmentRepository;
    private final UserContextPort userContextPort;

    public ViewMyAssessmentSchedulesUseCase(
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentRepository assessmentRepository,
            UserContextPort userContextPort) {
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentRepository = assessmentRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentScheduleDto> execute(ViewAssessmentSchedulesQuery input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var candidates = assessmentCandidateRepository.findByStudentId(studentId).stream()
            .filter(candidate -> input.assessmentId() == null || input.assessmentId().equals(candidate.getAssessmentId()))
            .toList();
        if (input.assessmentId() != null && candidates.isEmpty()) {
            throw new ForbiddenException("Bạn không phải là thí sinh của bài kiểm tra này.");
        }

        // Ca thi chỉ hiện khi KỲ THI đã công bố, không chỉ khi ca đã publish.
        //
        // Hai trạng thái độc lập nhau: người xếp lịch có thể publish ca trong lúc kỳ thi còn
        // DRAFT (chưa bấm SCHEDULE). Trước đây chỉ lọc theo ca, nên học sinh thấy lịch của một
        // kỳ thi chưa được công bố -- lộ đề cương thời gian trước khi nhà trường chốt.
        //
        // Cùng luật với ViewMyAssessmentsUseCase#toRow: chỉ ẩn DRAFT. CANCELLED vẫn hiện để học sinh
        // biết kỳ thi đã bị huỷ, giống cách ca CANCELLED vẫn lọt isVisibleToStudent.
        var publishedAssessmentIds = assessmentRepository.findByIdIn(
                candidates.stream()
                    .map(candidate -> candidate.getAssessmentId())
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList()).stream()
            .filter(assessment -> assessment.getStatus() != AssessmentStatus.DRAFT)
            .map(assessment -> assessment.getId())
            .collect(java.util.stream.Collectors.toSet());

        var scheduleIds = candidates.stream()
            .filter(candidate -> publishedAssessmentIds.contains(candidate.getAssessmentId()))
            .map(candidate -> candidate.getScheduleId())
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        var schedules = scheduleIds.isEmpty()
            ? List.<AssessmentSchedule>of()
            : assessmentScheduleRepository.findByIdIn(scheduleIds);
        return AssessmentScheduleDto.toDtoList(schedules.stream()
            // Lọc hiển thị đặt trước bộ lọc của client: ca DRAFT là ca chưa publish -- chưa xếp lịch
            // xong -- nên dù client có truyền status=DRAFT cũng không được moi ra.
            .filter(schedule -> schedule.getStatus() != null && schedule.getStatus().isVisibleToStudent())
            .filter(schedule -> input.status() == null || input.status() == schedule.getStatus())
            .filter(schedule -> input.startDate() == null
                || (schedule.getStartDate() != null && !schedule.getStartDate().isBefore(input.startDate())))
            .filter(schedule -> input.endDate() == null
                || (schedule.getStartDate() != null && !schedule.getStartDate().isAfter(input.endDate())))
            .sorted(Comparator.comparing((AssessmentSchedule schedule) -> schedule.getStartDate(),
                Comparator.nullsLast(Comparator.reverseOrder())))
            .toList());
    }
}
