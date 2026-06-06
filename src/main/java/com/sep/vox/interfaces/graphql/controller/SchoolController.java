package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.application.port.input.query.ViewSchoolRoomDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolRoomsBySchoolIdQuery;
import com.sep.vox.application.port.input.usecase.schoolroom.UpdateSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomsUseCase;
import com.sep.vox.application.response.SchoolRoomResponse.SchoolRoomResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolRequest;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolRoomRequest;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolCommandMapper;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolRoomMapper;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.usecase.school.UpdateSchoolUseCase;

import java.util.UUID;

@Controller("graphqlSchoolController")
public class SchoolController {

    private final UpdateSchoolUseCase updateSchoolUseCase;
    private final ViewSchoolRoomDetailsUseCase viewSchoolRoomDetailsUseCase;
    private final ViewSchoolRoomsUseCase viewSchoolRoomsUseCase;
    private final UpdateSchoolRoomUseCase updateSchoolRoomUseCase;

    public SchoolController(UpdateSchoolUseCase updateSchoolUseCase, ViewSchoolRoomDetailsUseCase viewSchoolRoomDetailsUseCase, ViewSchoolRoomsUseCase viewSchoolRoomsUseCase, UpdateSchoolRoomUseCase updateSchoolRoomUseCase) {
        this.updateSchoolUseCase = updateSchoolUseCase;
        this.viewSchoolRoomDetailsUseCase = viewSchoolRoomDetailsUseCase;
        this.viewSchoolRoomsUseCase = viewSchoolRoomsUseCase;
        this.updateSchoolRoomUseCase = updateSchoolRoomUseCase;
    }


    //Nhập id school vào update gì đó => SchoolID => check
    @MutationMapping(name = "updateSchool")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchool(@Argument(name = "request") UpdateSchoolRequest request) {

       var command = UpdateSchoolCommandMapper.fromRequest(request.id(), request);

        return updateSchoolUseCase.execute(command);
    }



    //============================SCHOOL ROOM===========================================


    @QueryMapping(name = "schoolRoom")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolRoomResponse getSchoolRoomById(@Argument UUID id) {
        var query = new ViewSchoolRoomDetailsQuery(id);
        return viewSchoolRoomDetailsUseCase.execute(query);
    }

    @QueryMapping(name = "schoolRooms")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolRoomResponse> getSchoolRoomsBySchoolId(
            @Argument UUID schoolId,
            @Argument Integer page,
            @Argument Integer size) {

        int validPage = (page != null && page >= 0) ? page : 0;
        int validSize = (size != null && size > 0) ? size : 10;

        var query = new ViewSchoolRoomsBySchoolIdQuery(schoolId, validPage, validSize);
        return viewSchoolRoomsUseCase.execute(query);
    }

    // nhập Id của room muốn đổi => update (200) trả về school Id
    @MutationMapping(name = "updateSchoolRoom")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolRoom(@Argument("input") UpdateSchoolRoomRequest request) {

        var command = UpdateSchoolRoomMapper.fromRequest(request.id(), request);
        return updateSchoolRoomUseCase.execute(command);
    }
}