package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.application.port.input.query.GetSchoolRoomByIdQuery;
import com.sep.vox.application.port.input.query.GetSchoolRoomsQuery;
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
public class SchoolRoomGraphQlController {

    private final ViewSchoolRoomDetailsUseCase viewSchoolRoomDetailsUseCase;
    private final ViewSchoolRoomsUseCase viewSchoolRoomsUseCase;

    public SchoolRoomGraphQlController(ViewSchoolRoomDetailsUseCase viewSchoolRoomDetailsUseCase, ViewSchoolRoomsUseCase viewSchoolRoomsUseCase) {
        this.viewSchoolRoomDetailsUseCase = viewSchoolRoomDetailsUseCase;
        this.viewSchoolRoomsUseCase = viewSchoolRoomsUseCase;
    }

    @QueryMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public SchoolRoomResponse getSchoolRoomById(@Argument UUID id) {
        var query = new GetSchoolRoomByIdQuery(id);
        return viewSchoolRoomDetailsUseCase.execute(query);
    }

    // Tự động map với query "getSchoolRooms" trong schema
    @QueryMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<SchoolRoomResponse> getSchoolRooms(
            @Argument Integer page,
            @Argument Integer size) {

        int validPage = (page != null && page >= 0) ? page : 0;
        int validSize = (size != null && size > 0) ? size : 10;

        var query = new GetSchoolRoomsQuery(validPage, validSize);

        return viewSchoolRoomsUseCase.execute(query);
    }
}