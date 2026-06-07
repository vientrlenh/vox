package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.application.port.input.query.*;
import com.sep.vox.application.port.input.usecase.schoolgrade.UpdateSchoolGradeUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.ViewSchoolGradeDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.ViewSchoolGradesUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.UpdateSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomsUseCase;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolGradeRequest;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolRequest;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolRoomRequest;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolCommandMapper;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolGradeCommandMapper;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolRoomCommandMapper;

import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.SchoolGradeFromDto;
import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.dto.SchoolUserDto;

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

import com.sep.vox.application.port.input.query.key.SchoolClassesKey;
import com.sep.vox.application.port.input.usecase.school.ViewSchoolsUseCase;
import com.sep.vox.application.port.input.usecase.school.UpdateSchoolUseCase;

import graphql.schema.DataFetchingEnvironment;

@Controller("graphqlSchoolController")
public class SchoolController {

    private final ViewSchoolsUseCase viewSchoolsUseCase;
    private final UpdateSchoolUseCase updateSchoolUseCase;
    private final ViewSchoolRoomDetailsUseCase viewSchoolRoomDetailsUseCase;
    private final ViewSchoolRoomsUseCase viewSchoolRoomsUseCase;
    private final UpdateSchoolRoomUseCase updateSchoolRoomUseCase;
    private final UpdateSchoolGradeUseCase updateSchoolGradeUseCase;
    private final ViewSchoolGradesUseCase viewSchoolGradesUseCase;
    private final ViewSchoolGradeDetailsUseCase viewSchoolGradeDetailsUseCase;

    public SchoolController(ViewSchoolsUseCase viewSchoolsUseCase, UpdateSchoolUseCase updateSchoolUseCase, ViewSchoolRoomDetailsUseCase viewSchoolRoomDetailsUseCase, ViewSchoolRoomsUseCase viewSchoolRoomsUseCase, UpdateSchoolRoomUseCase updateSchoolRoomUseCase, UpdateSchoolGradeUseCase updateSchoolGradeUseCase, ViewSchoolGradesUseCase viewSchoolGradesUseCase, ViewSchoolGradeDetailsUseCase viewSchoolGradeDetailsUseCase) {
        this.viewSchoolsUseCase = viewSchoolsUseCase;
        this.updateSchoolUseCase = updateSchoolUseCase;
        this.viewSchoolRoomDetailsUseCase = viewSchoolRoomDetailsUseCase;
        this.viewSchoolRoomsUseCase = viewSchoolRoomsUseCase;
        this.updateSchoolRoomUseCase = updateSchoolRoomUseCase;
        this.updateSchoolGradeUseCase = updateSchoolGradeUseCase;
        this.viewSchoolGradesUseCase = viewSchoolGradesUseCase;
        this.viewSchoolGradeDetailsUseCase = viewSchoolGradeDetailsUseCase;
    }


    //Nhập id school vào update gì đó => SchoolID => check
    @MutationMapping(name = "updateSchool")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchool(@Argument(name = "id") UUID id, @Argument(name = "input") UpdateSchoolRequest request) {

        var command = UpdateSchoolCommandMapper.fromRequest(id, request);

        return updateSchoolUseCase.execute(command);


    }

    @QueryMapping(name = "schools")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<SchoolDto> schools(@Argument(name = "page") Integer page, @Argument(name = "size") Integer size) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích cỡ trang yêu cầu không hợp lệ");
        }
        var query = new ViewSchoolsQuery(page, size);
        return viewSchoolsUseCase.execute(query);
    }


    @SchemaMapping(typeName = "School", field = "classes")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public CompletableFuture<List<SchoolClassDto>> classes(SchoolDto school, @Argument(name = "page") int page, @Argument(name = "size") int size, DataFetchingEnvironment env) {
        DataLoader<SchoolClassesKey, List<SchoolClassDto>> loader = env.getDataLoader("schoolClassesBySchool");
        return loader.load(new SchoolClassesKey(school.id(), page, size));
    }

    //============================SCHOOL ROOM===========================================
    //Nhập id của roomId
    @QueryMapping(name = "schoolRoom")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolRoomFromDto schoolRoom(@Argument(name = "id") UUID id) {
        var query = new ViewSchoolRoomDetailsQuery(id);
        return viewSchoolRoomDetailsUseCase.execute(query);
    }

    //Nhập Id chủa schoolId
    @QueryMapping(name = "schoolRooms")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolRoomFromDto> schoolRooms(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        int pageNumber = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0) ? size : 10;

        var query = new ViewSchoolRoomsQuery(schoolId, new PageRequest(pageNumber, pageSize));
        return viewSchoolRoomsUseCase.execute(query);
    }

    // nhập Id của room muốn đổi => update (200) trả về school Id
    @MutationMapping(name = "updateSchoolRoom")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolRoom(
            @Argument(name = "id") UUID id,
            @Argument(name = "input") UpdateSchoolRoomRequest request
    ) {
        // Mapper truyền cả id và request
        var command = UpdateSchoolRoomCommandMapper.fromRequest(id, request);
        return updateSchoolRoomUseCase.execute(command);
    }


    //=======================SCHOOL USER=======================
    @QueryMapping(name = "schoolUsers")
    public PageResult<SchoolUserDto> schoolUsers(@Argument(name = "schoolId") UUID schoolId, @Argument(name = "page") Integer page, @Argument(name = "size") Integer size) {
        return null;
    }

    //========================SCHOOL GRADE =======================
    @MutationMapping(name = "updateSchoolGrade")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolGrade(
            @Argument(name = "id") UUID id,
            @Argument(name = "input") UpdateSchoolGradeRequest request
    ) {
        var command = UpdateSchoolGradeCommandMapper.fromRequest(id, request);
        return updateSchoolGradeUseCase.execute(command);
    }

    //Nhập id của gradeId
    @QueryMapping(name = "schoolGrade")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolGradeFromDto schoolGrade(@Argument(name = "id") UUID id) {
        var query = new ViewSchoolGradeDetailsQuery(id);
        return viewSchoolGradeDetailsUseCase.execute(query);
    }

    @QueryMapping(name = "schoolGrades")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolGradeFromDto> schoolGrades(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        // Validate tham số phân trang
        int pageNumber = (page != null && page > 0) ? page : 1;
        int pageSize = (size != null && size > 0) ? size : 10;

        var query = new ViewSchoolGradesQuery(schoolId, new PageRequest(pageNumber, pageSize));

        return viewSchoolGradesUseCase.execute(query);
    }


}