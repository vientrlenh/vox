package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.interfaces.graphql.dataloader.GraphQLDataLoaders;
import com.sep.vox.interfaces.graphql.dataloader.UserGraphQLDataLoaderConfig;
import com.sep.vox.application.port.input.command.user.UpdateProfileCommand;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.user.ViewUserDetailsQuery;
import com.sep.vox.application.port.input.query.user.ViewUsersQuery;
import com.sep.vox.interfaces.graphql.key.UserRolesKey;
import com.sep.vox.application.port.input.usecase.user.UpdateProfileUseCase;
import com.sep.vox.application.port.input.usecase.user.ViewProfileUseCase;
import com.sep.vox.application.port.input.usecase.user.ViewUserDetailsUseCase;
import com.sep.vox.application.port.input.usecase.user.ViewUsersUseCase;
import com.sep.vox.interfaces.graphql.input.user.UpdateProfileInput;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RoleDto;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.SchoolDtoMapper;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.interfaces.shared.PageArguments;

import graphql.schema.DataFetchingEnvironment;

@Controller
@RequiredArgsConstructor
public class GraphQLUserController {

    private final ViewUserDetailsUseCase viewUserDetailsUseCase;
    private final ViewUsersUseCase viewUsersUseCase;
    private final ViewProfileUseCase viewProfileUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolRepository schoolRepository;

    @QueryMapping(name = "user")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserDto user(@Argument(name = "id") UUID id) {
        ViewUserDetailsQuery query = new ViewUserDetailsQuery(id);
        return viewUserDetailsUseCase.execute(query);
    }

    @QueryMapping(name = "users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<UserDto> users(@Argument(name = "page") Integer page, @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        ViewUsersQuery query = new ViewUsersQuery(page, size);
        return viewUsersUseCase.execute(query);
    }

    @QueryMapping(name = "profile")
    @PreAuthorize("isAuthenticated()")
    public UserDto profile() {
        return viewProfileUseCase.execute(null);
    }

    /**
     * {@code isAuthenticated()} rather than a list of roles: every role may edit its own profile.
     * Only the avatar is refused to students, and that rule lives in the use case, not here --
     * it depends on whether the client sent the {@code avatarUrl} field, which
     * {@code @PreAuthorize} cannot see.
     */
    @MutationMapping(name = "updateProfile")
    @PreAuthorize("isAuthenticated()")
    public UserDto updateProfile(@Argument(name = "input") UpdateProfileInput input) {
        UpdateProfileCommand command = UpdateProfileInput.toCommand(input);
        return updateProfileUseCase.execute(command);
    }

    @SchemaMapping(typeName = "User", field = "roles")
    @PreAuthorize("isAuthenticated()")
    public CompletableFuture<List<RoleDto>> userRoles(UserDto user, DataFetchingEnvironment env) {
        DataLoader<UserRolesKey, List<RoleDto>> loader = GraphQLDataLoaders.require(env, UserGraphQLDataLoaderConfig.ROLES_BY_USER_ID);
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
