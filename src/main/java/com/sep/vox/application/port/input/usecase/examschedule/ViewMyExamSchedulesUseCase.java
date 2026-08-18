package com.sep.vox.application.port.input.usecase.examschedule;

import java.util.Comparator;
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
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;

@Service
public class ViewMyExamSchedulesUseCase
        implements IUseCase<ViewExamSchedulesQuery, List<ExamScheduleDto>> {

    private final ExamCandidateRepository examCandidateRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamRepository examRepository;
    private final UserContextPort userContextPort;

    public ViewMyExamSchedulesUseCase(
            ExamCandidateRepository examCandidateRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamRepository examRepository,
            UserContextPort userContextPort) {
        this.examCandidateRepository = examCandidateRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examRepository = examRepository;
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

        // Ca thi chỉ hiện khi KỲ THI đã công bố, không chỉ khi ca đã publish.
        //
        // Hai trạng thái độc lập nhau: người xếp lịch có thể publish ca trong lúc kỳ thi còn
        // DRAFT (chưa bấm SCHEDULE). Trước đây chỉ lọc theo ca, nên học sinh thấy lịch của một
        // kỳ thi chưa được công bố -- lộ đề cương thời gian trước khi nhà trường chốt.
        //
        // Cùng luật với ViewMyExamsUseCase#toRow: chỉ ẩn DRAFT. CANCELLED vẫn hiện để học sinh
        // biết kỳ thi đã bị huỷ, giống cách ca CANCELLED vẫn lọt isVisibleToStudent.
        var publishedExamIds = examRepository.findByIdIn(
                candidates.stream()
                    .map(candidate -> candidate.getExamId())
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList()).stream()
            .filter(exam -> exam.getStatus() != ExamStatus.DRAFT)
            .map(exam -> exam.getId())
            .collect(java.util.stream.Collectors.toSet());

        var scheduleIds = candidates.stream()
            .filter(candidate -> publishedExamIds.contains(candidate.getExamId()))
            .map(candidate -> candidate.getScheduleId())
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        var schedules = scheduleIds.isEmpty()
            ? List.<ExamSchedule>of()
            : examScheduleRepository.findByIdIn(scheduleIds);
        return ExamScheduleDtoMapper.toDtoList(schedules.stream()
            // Lọc hiển thị đặt trước bộ lọc của client: ca DRAFT là ca chưa publish -- chưa xếp lịch
            // xong -- nên dù client có truyền status=DRAFT cũng không được moi ra.
            .filter(schedule -> schedule.getStatus() != null && schedule.getStatus().isVisibleToStudent())
            .filter(schedule -> input.status() == null || input.status() == schedule.getStatus())
            .filter(schedule -> input.startDate() == null
                || (schedule.getStartDate() != null && !schedule.getStartDate().isBefore(input.startDate())))
            .filter(schedule -> input.endDate() == null
                || (schedule.getStartDate() != null && !schedule.getStartDate().isAfter(input.endDate())))
            .sorted(Comparator.comparing((ExamSchedule schedule) -> schedule.getStartDate(),
                Comparator.nullsLast(Comparator.reverseOrder())))
            .toList());
    }
}
