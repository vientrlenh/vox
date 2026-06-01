package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.command.ListSchoolUsersCommand;
import com.sep.vox.application.port.input.command.ViewSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.schooluser.ListSchoolUsersUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUserUseCase;
import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.domain.common.PageResult;

@Controller("graphqlSchoolUserController")
public class SchoolUserController {

    private final ListSchoolUsersUseCase listSchoolUsersUseCase;
    private final ViewSchoolUserUseCase viewSchoolUserUseCase;

    public SchoolUserController(
            ListSchoolUsersUseCase listSchoolUsersUseCase,
            ViewSchoolUserUseCase viewSchoolUserUseCase) {
        this.listSchoolUsersUseCase = listSchoolUsersUseCase;
        this.viewSchoolUserUseCase = viewSchoolUserUseCase;
    }

    @QueryMapping(name = "schoolUsers")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolUserResponse> schoolUsers(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
        var query = new ListSchoolUsersCommand(schoolId, page, size);
        return listSchoolUsersUseCase.execute(query);
    }

    @QueryMapping(name = "schoolUser")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolUserResponse schoolUser(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "userId") UUID userId) {
        var query = new ViewSchoolUserCommand(schoolId, userId);
        return viewSchoolUserUseCase.execute(query);
    }
}