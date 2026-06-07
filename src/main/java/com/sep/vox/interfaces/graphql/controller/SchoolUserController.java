package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.command.ListSchoolUsersCommand;
import com.sep.vox.application.port.input.command.ViewSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.schooluser.ListSchoolUsersUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUserUseCase;
import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.SchoolDtoMapper;
import com.sep.vox.domain.mapper.UserDtoMapper;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;

@Controller("graphqlSchoolUserController")
public class SchoolUserController {

    private final ListSchoolUsersUseCase listSchoolUsersUseCase;
    private final ViewSchoolUserUseCase viewSchoolUserUseCase;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;

    public SchoolUserController(
            ListSchoolUsersUseCase listSchoolUsersUseCase,
            ViewSchoolUserUseCase viewSchoolUserUseCase,
            UserRepository userRepository,
            SchoolRepository schoolRepository) {
        this.listSchoolUsersUseCase = listSchoolUsersUseCase;
        this.viewSchoolUserUseCase = viewSchoolUserUseCase;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
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

    @SchemaMapping(typeName = "SchoolUser", field = "user")
    public UserDto user(SchoolUserResponse schoolUser) {
        if (schoolUser.userId() == null) {
            return null;
        }
        return userRepository.findById(schoolUser.userId())
            .map(UserDtoMapper::toUserDto)
            .orElse(null);
    }

    @SchemaMapping(typeName = "SchoolUser", field = "school")
    public SchoolDto school(SchoolUserResponse schoolUser) {
        if (schoolUser.schoolId() == null) {
            return null;
        }
        return schoolRepository.findById(schoolUser.schoolId())
            .map(SchoolDtoMapper::toSchoolDto)
            .orElse(null);
    }

    @SchemaMapping(typeName = "SchoolUser", field = "startDate")
    public String startDate(SchoolUserResponse schoolUser) {
        return schoolUser.startDate() != null ? schoolUser.startDate().toString() : null;
    }

    @SchemaMapping(typeName = "SchoolUser", field = "endDate")
    public String endDate(SchoolUserResponse schoolUser) {
        return schoolUser.endDate() != null ? schoolUser.endDate().toString() : null;
    }
}