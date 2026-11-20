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
import com.sep.vox.application.port.input.query.ViewAssessmentSchedulesQuery;
import com.sep.vox.application.port.input.query.ViewProctorBusySlotsQuery;
import com.sep.vox.application.port.input.query.ViewStudentBusySlotsQuery;
import com.sep.vox.application.port.input.usecase.assessmentschedule.UpdateAssessmentScheduleUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.ViewAssessmentSchedulesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.ViewMyAssessmentSchedulesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.ViewProctorBusySlotsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.ViewStudentBusySlotsUseCase;
import com.sep.vox.application.response.input.assessmentschedule.ProctorBusySlotResponse;
import com.sep.vox.application.response.input.assessmentschedule.StudentBusySlotResponse;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.dto.AssessmentScheduleDto;
import com.sep.vox.domain.dto.AssessmentScheduleProctorDto;
import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.AssessmentScheduleProctorDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.interfaces.graphql.dto.request.UpdateAssessmentScheduleInput;
import com.sep.vox.interfaces.graphql.mapper.UpdateAssessmentScheduleCommandMapper;

@Controller("graphqlAssessmentScheduleController")
public class AssessmentScheduleController {

    private final ViewAssessmentSchedulesUseCase viewAssessmentSchedulesUseCase;
    private final ViewMyAssessmentSchedulesUseCase viewMyAssessmentSchedulesUseCase;
    private final UpdateAssessmentScheduleUseCase updateAssessmentScheduleUseCase;
    private final ViewProctorBusySlotsUseCase viewProctorBusySlotsUseCase;
    private final ViewStudentBusySlotsUseCase viewStudentBusySlotsUseCase;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;

    public AssessmentScheduleController(
            ViewAssessmentSchedulesUseCase viewAssessmentSchedulesUseCase,
            ViewMyAssessmentSchedulesUseCase viewMyAssessmentSchedulesUseCase,
            UpdateAssessmentScheduleUseCase updateAssessmentScheduleUseCase,
            ViewProctorBusySlotsUseCase viewProctorBusySlotsUseCase,
            ViewStudentBusySlotsUseCase viewStudentBusySlotsUseCase,
            AssessmentScheduleProctorRepository assessmentScheduleProctorRepository,
            AssessmentCandidateRepository assessmentCandidateRepository) {
        this.viewAssessmentSchedulesUseCase = viewAssessmentSchedulesUseCase;
        this.viewMyAssessmentSchedulesUseCase = viewMyAssessmentSchedulesUseCase;
        this.updateAssessmentScheduleUseCase = updateAssessmentScheduleUseCase;
        this.viewProctorBusySlotsUseCase = viewProctorBusySlotsUseCase;
        this.viewStudentBusySlotsUseCase = viewStudentBusySlotsUseCase;
        this.assessmentScheduleProctorRepository = assessmentScheduleProctorRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
    }

    @QueryMapping(name = "myAssessmentSchedules")
    @PreAuthorize("hasRole('STUDENT')")
    public List<AssessmentScheduleDto> myAssessmentSchedules(
            @Argument(name = "assessmentId") UUID assessmentId,
            @Argument(name = "status") AssessmentScheduleStatus status,
            @Argument(name = "startDate") String startDate,
            @Argument(name = "endDate") String endDate) {
        return viewMyAssessmentSchedulesUseCase.execute(new ViewAssessmentSchedulesQuery(
            assessmentId,
            status,
            DateMapper.toInstant(startDate),
            DateMapper.toInstant(endDate)
        ));
    }

    @QueryMapping(name = "assessmentSchedules")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public List<AssessmentScheduleDto> assessmentSchedules(
            @Argument(name = "assessmentId") UUID assessmentId,
            @Argument(name = "status") AssessmentScheduleStatus status,
            @Argument(name = "startDate") String startDate,
            @Argument(name = "endDate") String endDate) {
        return viewAssessmentSchedulesUseCase.execute(
            new ViewAssessmentSchedulesQuery(
                assessmentId, 
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

    /**
     * Trong nhóm học sinh đang xét, ai bận vào đúng khung giờ của từng ca thi đang cân nhắc.
     * Chỉ để làm mờ sẵn kèm lý do — luật chặn thật chạy lúc ghi.
     */
    @QueryMapping(name = "studentBusySlots")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public List<StudentBusySlotResponse> studentBusySlots(
            @Argument(name = "scheduleIds") List<UUID> scheduleIds,
            @Argument(name = "studentIds") List<UUID> studentIds) {
        return viewStudentBusySlotsUseCase.execute(new ViewStudentBusySlotsQuery(scheduleIds, studentIds));
    }

    @SchemaMapping(typeName = "AssessmentSchedule", field = "room")
    public CompletableFuture<SchoolRoomFromDto> room(AssessmentScheduleDto source, DataFetchingEnvironment env) {
        // A session is allowed to have no room yet (a class test creates a draft session and chooses the room later).
        // The DataLoader throws NPE when it receives a null key, breaking the whole assessmentSchedules query and not only this field.
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
    @SchemaMapping(typeName = "AssessmentSchedule", field = "assessment")
    public CompletableFuture<AssessmentDto> assessment(AssessmentScheduleDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, AssessmentDto> loader = env.getDataLoader("assessmentById");
        return loader.load(source.assessmentId());
    }

    @SchemaMapping(typeName = "AssessmentSchedule", field = "proctors")
    public List<AssessmentScheduleProctorDto> proctors(AssessmentScheduleDto source) {
        return AssessmentScheduleProctorDtoMapper.toDtoList(assessmentScheduleProctorRepository.findByScheduleId(source.id()));
    }

    @SchemaMapping(typeName = "AssessmentSchedule", field = "candidateCount")
    public int candidateCount(AssessmentScheduleDto source) {
        return (int) assessmentCandidateRepository.countByScheduleId(source.id());
    }

    @SchemaMapping(typeName = "AssessmentSchedule", field = "requiredProctorCount")
    public int requiredProctorCount(AssessmentScheduleDto source) {
        return 1;
    }

    @SchemaMapping(typeName = "AssessmentScheduleProctor", field = "teacher")
    public CompletableFuture<UserDto> teacher(AssessmentScheduleProctorDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userById");
        return loader.load(source.teacherId());
    }

    @MutationMapping(name = "updateAssessmentSchedule")
    public UUID updateAssessmentSchedule(
            @Argument(name = "id") UUID id,
            @Argument(name = "input") UpdateAssessmentScheduleInput input) {
        return updateAssessmentScheduleUseCase.execute(UpdateAssessmentScheduleCommandMapper.fromRequest(id, input));
    }
}