package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.application.response.SchoolResponse.SchoolResponse;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolRequest;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolCommandMapper;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.usecase.school.UpdateSchoolUseCase;

@Controller("graphqlSchoolController")
public class SchoolController {

    private final UpdateSchoolUseCase updateSchoolUseCase;

    public SchoolController(UpdateSchoolUseCase updateSchoolUseCase) {
        this.updateSchoolUseCase = updateSchoolUseCase;
    }

    @MutationMapping(name = "updateSchool")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolResponse updateSchool(@Argument(name = "request") UpdateSchoolRequest request) {

       var command = UpdateSchoolCommandMapper.fromRequest(request.id(), request);

        return updateSchoolUseCase.execute(command);
    }
}