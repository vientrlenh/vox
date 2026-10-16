package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import com.sep.vox.application.port.input.command.UpdateProfileCommand;
import com.sep.vox.application.port.input.query.ViewUserDetailsQuery;
import com.sep.vox.application.port.input.query.ViewUsersQuery;
import com.sep.vox.application.port.input.usecase.user.UpdateProfileUseCase;
import com.sep.vox.application.port.input.usecase.user.ViewProfileUseCase;
import com.sep.vox.application.port.input.usecase.user.ViewUserDetailsUseCase;
import com.sep.vox.application.port.input.usecase.user.ViewUsersUseCase;
import com.sep.vox.application.shared.AppPageRequest;
import com.sep.vox.interfaces.graphql.dto.UpdateProfileInput;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.shared.PageResult;

import lombok.RequiredArgsConstructor;

@Controller
@Validated 
@RequiredArgsConstructor 
public class GraphQLUserController {

    private final ViewUserDetailsUseCase viewUserDetailsUseCase;
    private final ViewUsersUseCase viewUsersUseCase;
    private final ViewProfileUseCase viewProfileUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;


    @QueryMapping(name = "user")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserDto user(@Argument(name = "id") UUID id) {
        ViewUserDetailsQuery query = new ViewUserDetailsQuery(id);
        return viewUserDetailsUseCase.execute(query);
    }

    @QueryMapping(name = "users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<UserDto> users(@Argument(name = "page") Integer page, @Argument(name = "size") Integer size) {
        AppPageRequest pageRequest = AppPageRequest.pageRequest(page, size);
        ViewUsersQuery query = new ViewUsersQuery(pageRequest);
        return viewUsersUseCase.execute(query);
    }

    @QueryMapping(name = "me")
    @PreAuthorize("isAuthenticated()")
    public UserDto me() {
        return viewProfileUseCase.execute(null);
    }

    @MutationMapping(name = "updateMe")
    @PreAuthorize("isAuthenticated()")
    public UUID updateMe(@Argument(name = "input") UpdateProfileInput input) {
        UpdateProfileCommand command = UpdateProfileInput.toCommand(input);
        return updateProfileUseCase.execute(command);
    }
}
