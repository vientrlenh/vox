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

import com.sep.vox.application.port.input.query.ViewRoleDetailsQuery;
import com.sep.vox.application.port.input.query.ViewRolesQuery;
import com.sep.vox.application.port.input.query.key.RoleUsersKey;
import com.sep.vox.application.port.input.usecase.role.ViewRoleDetailsUseCase;
import com.sep.vox.application.port.input.usecase.role.ViewRolesUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RoleDto;
import com.sep.vox.domain.dto.UserDto;

import graphql.schema.DataFetchingEnvironment;

@Controller("graphqlRoleController")
public class RoleController {
    
    private final ViewRolesUseCase viewRolesUseCase;
    private final ViewRoleDetailsUseCase viewRoleDetailsUseCase;

    public RoleController(ViewRolesUseCase viewRolesUseCase, ViewRoleDetailsUseCase viewRoleDetailsUseCase) {
        this.viewRolesUseCase = viewRolesUseCase;
        this.viewRoleDetailsUseCase = viewRoleDetailsUseCase;
    }

    @QueryMapping(name = "roles")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RoleDto> roles(@Argument(name = "page") Integer page, @Argument(name = "size") Integer size) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích cỡ yêu cầu không hợp lệ");
        }
        var query = new ViewRolesQuery(page, size);
        return viewRolesUseCase.execute(query);
    }

    @QueryMapping(name = "role")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public RoleDto role(@Argument(name = "id") UUID id) {
        var query = new ViewRoleDetailsQuery(id);
        return viewRoleDetailsUseCase.execute(query);
    }

    @SchemaMapping(typeName = "Role", field = "users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public CompletableFuture<List<UserDto>> roleUsers(RoleDto role, @Argument(name = "page") Integer page, @Argument(name = "size") Integer size,  DataFetchingEnvironment env) {
        DataLoader<RoleUsersKey, List<UserDto>> loader = env.getDataLoader("usersByRole");
        return loader.load(new RoleUsersKey(role.id(), page, size));
    }
}
