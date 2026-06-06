package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.application.port.input.query.ViewSchoolRoomDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolRoomsBySchoolIdQuery;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomsUseCase;
import com.sep.vox.application.response.SchoolRoomResponse.SchoolRoomResponse;
import com.sep.vox.domain.common.PageResult;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller("graphqlSchoolRoom")
public class SchoolRoomController {

    private final ViewSchoolRoomDetailsUseCase viewSchoolRoomDetailsUseCase;
    private final ViewSchoolRoomsUseCase viewSchoolRoomsUseCase;

    public SchoolRoomController(ViewSchoolRoomDetailsUseCase viewSchoolRoomDetailsUseCase, ViewSchoolRoomsUseCase viewSchoolRoomsUseCase) {
        this.viewSchoolRoomDetailsUseCase = viewSchoolRoomDetailsUseCase;
        this.viewSchoolRoomsUseCase = viewSchoolRoomsUseCase;
    }

    @QueryMapping(name = "schoolRoom")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public SchoolRoomResponse getSchoolRoomById(@Argument UUID id) {
        var query = new ViewSchoolRoomDetailsQuery(id);
        return viewSchoolRoomDetailsUseCase.execute(query);
    }

    @QueryMapping(name = "schoolRooms")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<SchoolRoomResponse> getSchoolRoomsBySchoolId(
            @Argument UUID schoolId,
            @Argument Integer page,
            @Argument Integer size) {

        int validPage = (page != null && page >= 0) ? page : 0;
        int validSize = (size != null && size > 0) ? size : 10;

        var query = new ViewSchoolRoomsBySchoolIdQuery(schoolId, validPage, validSize);
        return viewSchoolRoomsUseCase.execute(query);
    }
}