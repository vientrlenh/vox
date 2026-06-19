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
import com.sep.vox.application.port.input.usecase.user.ViewUserDetailsUseCase;
import com.sep.vox.application.port.input.usecase.user.ViewUsersUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RoleDto;
import com.sep.vox.domain.dto.UserDto;

import graphql.schema.DataFetchingEnvironment;

@Controller("graphqlUserController")
public class UserController {
    
    private final ViewUserDetailsUseCase viewUserDetailsUseCase;
    private final ViewUsersUseCase viewUsersUseCase;

    public UserController(ViewUserDetailsUseCase viewUserDetailsUseCase, ViewUsersUseCase viewUsersUseCase) {
        this.viewUserDetailsUseCase = viewUserDetailsUseCase;
        this.viewUsersUseCase = viewUsersUseCase;
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

    @SchemaMapping(typeName = "User", field = "roles")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public CompletableFuture<List<RoleDto>> userRoles(UserDto user, DataFetchingEnvironment env) {
        DataLoader<UserRolesKey, List<RoleDto>> loader = env.getDataLoader("rolesByUser");
        return loader.load(new UserRolesKey(user.id()));
    }
}
