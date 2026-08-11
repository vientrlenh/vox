package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewUserDetailsQuery;
import com.sep.vox.application.port.input.query.ViewUsersQuery;
import com.sep.vox.application.port.input.query.key.UserRolesKey;
import com.sep.vox.application.port.input.usecase.user.ViewProfileUseCase;
import com.sep.vox.application.port.input.usecase.user.ViewUserDetailsUseCase;
import com.sep.vox.application.port.input.usecase.user.ViewUsersUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RoleDto;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.SchoolDtoMapper;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

import graphql.schema.DataFetchingEnvironment;

@Controller("graphqlUserController")
public class UserController {

    private final ViewUserDetailsUseCase viewUserDetailsUseCase;
    private final ViewUsersUseCase viewUsersUseCase;
    private final ViewProfileUseCase viewProfileUseCase;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolRepository schoolRepository;

    public UserController(
            ViewUserDetailsUseCase viewUserDetailsUseCase,
            ViewUsersUseCase viewUsersUseCase,
            ViewProfileUseCase viewProfileUseCase,
            SchoolUserRepository schoolUserRepository,
            SchoolRepository schoolRepository) {
        this.viewUserDetailsUseCase = viewUserDetailsUseCase;
        this.viewUsersUseCase = viewUsersUseCase;
        this.viewProfileUseCase = viewProfileUseCase;
        this.schoolUserRepository = schoolUserRepository;
        this.schoolRepository = schoolRepository;
    }

    @QueryMapping(name = "user")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserDto user(@Argument(name = "id") UUID id) {
        var query = new ViewUserDetailsQuery(id);
        return viewUserDetailsUseCase.execute(query);
    }

    @QueryMapping(name = "users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<UserDto> users(@Argument(name = "page") Integer page, @Argument(name = "size") Integer size) {
        if (page == null || size == null || page <= 0  || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích cỡ yêu cầu không hợp lệ");
        }
        var query = new ViewUsersQuery(page, size);
        return viewUsersUseCase.execute(query);
    }

    @QueryMapping(name = "profile")
    @PreAuthorize("isAuthenticated()")
    public UserDto profile() {
        return viewProfileUseCase.execute(null);
    }

    @SchemaMapping(typeName = "User", field = "roles")
    @PreAuthorize("isAuthenticated()")
    public CompletableFuture<List<RoleDto>> userRoles(UserDto user, DataFetchingEnvironment env) {
        DataLoader<UserRolesKey, List<RoleDto>> loader = env.getDataLoader("rolesByUser");
        return loader.load(new UserRolesKey(user.id()));
    }

    @SchemaMapping(typeName = "User", field = "school")
    @PreAuthorize("isAuthenticated()")
    public SchoolDto userSchool(UserDto user) {
        return schoolUserRepository.findByUserId(user.id())
            .flatMap(schoolUser -> schoolRepository.findById(schoolUser.getSchoolId()))
            .map(SchoolDtoMapper::toSchoolDto)
            .orElse(null);
    }
}
