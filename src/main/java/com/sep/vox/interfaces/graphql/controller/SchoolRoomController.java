package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.application.port.input.query.ViewSchoolRoomDetailsQuery;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomDetailsUseCase;
import com.sep.vox.application.response.SchoolRoomResponse.SchoolRoomResponse;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller("graphqlSchoolRoom")
public class SchoolRoomController {

    private final ViewSchoolRoomDetailsUseCase viewSchoolRoomDetailsUseCase;

    public SchoolRoomController(ViewSchoolRoomDetailsUseCase viewSchoolRoomDetailsUseCase) {
        this.viewSchoolRoomDetailsUseCase = viewSchoolRoomDetailsUseCase;
    }

    @QueryMapping(name = "SchoolRoom")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public SchoolRoomResponse getSchoolRoomById(@Argument UUID id) {
        var query = new ViewSchoolRoomDetailsQuery(id);
        return viewSchoolRoomDetailsUseCase.execute(query);
    }
}