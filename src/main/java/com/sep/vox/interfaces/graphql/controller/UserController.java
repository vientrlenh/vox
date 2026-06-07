package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewUserDetailsQuery;
import com.sep.vox.application.port.input.usecase.user.ViewUserDetailsUseCase;
import com.sep.vox.domain.dto.UserDto;

@Controller("graphqlUserController")
public class UserController {
    
    private final ViewUserDetailsUseCase viewUserDetailsUseCase;

    public UserController(ViewUserDetailsUseCase viewUserDetailsUseCase) {
        this.viewUserDetailsUseCase = viewUserDetailsUseCase;
    }

    @QueryMapping(name = "user")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserDto user(@Argument(name = "id") UUID id) {
        var query = new ViewUserDetailsQuery(id);
        return viewUserDetailsUseCase.execute(query);
    }
}
