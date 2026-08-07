package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import graphql.schema.DataFetchingEnvironment;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.query.ViewExamSchedulesQuery;
import com.sep.vox.application.port.input.query.ViewProctorBusySlotsQuery;
import com.sep.vox.application.port.input.usecase.examschedule.UpdateExamScheduleUseCase;
import com.sep.vox.application.port.input.usecase.examschedule.ViewExamSchedulesUseCase;
import com.sep.vox.application.port.input.usecase.examschedule.ViewMyExamSchedulesUseCase;
import com.sep.vox.application.port.input.usecase.examschedule.ViewProctorBusySlotsUseCase;
import com.sep.vox.application.response.input.examschedule.ProctorBusySlotResponse;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.dto.ExamScheduleDto;
import com.sep.vox.domain.dto.ExamScheduleProctorDto;
import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.ExamScheduleProctorDtoMapper;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.interfaces.graphql.dto.request.UpdateExamScheduleInput;
import com.sep.vox.interfaces.graphql.mapper.UpdateExamScheduleCommandMapper;

@Controller("graphqlExamScheduleController")
public class ExamScheduleController {

    private final ViewExamSchedulesUseCase viewExamSchedulesUseCase;
    private final ViewMyExamSchedulesUseCase viewMyExamSchedulesUseCase;
    private final UpdateExamScheduleUseCase updateExamScheduleUseCase;
    private final ViewProctorBusySlotsUseCase viewProctorBusySlotsUseCase;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final ExamCandidateRepository examCandidateRepository;

    public ExamScheduleController(
            ViewExamSchedulesUseCase viewExamSchedulesUseCase,
            ViewMyExamSchedulesUseCase viewMyExamSchedulesUseCase,
            UpdateExamScheduleUseCase updateExamScheduleUseCase,
            ViewProctorBusySlotsUseCase viewProctorBusySlotsUseCase,
            ExamScheduleProctorRepository examScheduleProctorRepository,
            ExamCandidateRepository examCandidateRepository) {
        this.viewExamSchedulesUseCase = viewExamSchedulesUseCase;
        this.viewMyExamSchedulesUseCase = viewMyExamSchedulesUseCase;
        this.updateExamScheduleUseCase = updateExamScheduleUseCase;
        this.viewProctorBusySlotsUseCase = viewProctorBusySlotsUseCase;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.examCandidateRepository = examCandidateRepository;
    }

    @QueryMapping(name = "myExamSchedules")
    @PreAuthorize("hasRole('STUDENT')")
    public List<ExamScheduleDto> myExamSchedules(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "status") ExamScheduleStatus status,
            @Argument(name = "startDate") String startDate,
            @Argument(name = "endDate") String endDate) {
        return viewMyExamSchedulesUseCase.execute(new ViewExamSchedulesQuery(
            examId,
            status,
            DateMapper.toInstant(startDate),
            DateMapper.toInstant(endDate)
        ));
    }

    @QueryMapping(name = "examSchedules")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public List<ExamScheduleDto> examSchedules(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "status") ExamScheduleStatus status,
            @Argument(name = "startDate") String startDate,
            @Argument(name = "endDate") String endDate) {
        return viewExamSchedulesUseCase.execute(
            new ViewExamSchedulesQuery(
                examId, 
                status, 
                DateMapper.toInstant(startDate), 
                DateMapper.toInstant(endDate)
            )
        );
    }

    /**
     * Trong nhóm giáo viên đang hiển thị ở màn chọn giám thị, ai bận vào đúng khung giờ của ca này.
     * Chỉ để làm mờ sẵn kèm lý do — luật chặn thật chạy lúc ghi.
     */
    @QueryMapping(name = "proctorBusySlots")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public List<ProctorBusySlotResponse> proctorBusySlots(
            @Argument(name = "scheduleId") UUID scheduleId,
            @Argument(name = "teacherIds") List<UUID> teacherIds) {
        return viewProctorBusySlotsUseCase.execute(new ViewProctorBusySlotsQuery(scheduleId, teacherIds));
    }

    @SchemaMapping(typeName = "ExamSchedule", field = "room")
    public CompletableFuture<SchoolRoomFromDto> room(ExamScheduleDto source, DataFetchingEnvironment env) {
        // Ca thi được phép chưa có phòng (bài kiểm tra trên lớp tạo ca nháp rồi chọn phòng sau).
        // DataLoader ném NPE nếu nhận khoá null, làm hỏng cả query examSchedules chứ không chỉ field này.
        if (source.schoolRoomId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        DataLoader<UUID, SchoolRoomFromDto> loader = env.getDataLoader("schoolRoomById");
        return loader.load(source.schoolRoomId());
    }

    /**
     * Lịch thi của học sinh cần tên/loại kỳ thi ngay trên từng ca. Không có field này thì client
     * phải gọi thêm danh sách bài thi chỉ để tra tên -- mà danh sách đó nay đã phân trang.
     */
    @SchemaMapping(typeName = "ExamSchedule", field = "exam")
    public CompletableFuture<ExamDto> exam(ExamScheduleDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, ExamDto> loader = env.getDataLoader("examById");
        return loader.load(source.examId());
    }

    @SchemaMapping(typeName = "ExamSchedule", field = "proctors")
    public List<ExamScheduleProctorDto> proctors(ExamScheduleDto source) {
        return ExamScheduleProctorDtoMapper.toDtoList(examScheduleProctorRepository.findByScheduleId(source.id()));
    }

    @SchemaMapping(typeName = "ExamSchedule", field = "candidateCount")
    public int candidateCount(ExamScheduleDto source) {
        return (int) examCandidateRepository.countByScheduleId(source.id());
    }

    @SchemaMapping(typeName = "ExamSchedule", field = "requiredProctorCount")
    public int requiredProctorCount(ExamScheduleDto source) {
        return 1;
    }

    @SchemaMapping(typeName = "ExamScheduleProctor", field = "teacher")
    public CompletableFuture<UserDto> teacher(ExamScheduleProctorDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userById");
        return loader.load(source.teacherId());
    }

    @MutationMapping(name = "updateExamSchedule")
    public UUID updateExamSchedule(
            @Argument(name = "id") UUID id,
            @Argument(name = "input") UpdateExamScheduleInput input) {
        return updateExamScheduleUseCase.execute(UpdateExamScheduleCommandMapper.fromRequest(id, input));
    }
}