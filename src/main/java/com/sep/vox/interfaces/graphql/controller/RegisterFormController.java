package com.sep.vox.interfaces.graphql.controller;


import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewRegisterFormDetailsQuery;
import com.sep.vox.application.port.input.query.ViewRegisterFormsQuery;
import com.sep.vox.application.port.input.usecase.systemadmin.ViewRegisterFormDetailsUseCase;
import com.sep.vox.application.port.input.usecase.systemadmin.ViewRegisterFormsUseCase;
import com.sep.vox.domain.dto.RegisterFormDto;
import com.sep.vox.domain.util.PageResult;

@Controller
public class RegisterFormController {

    private final ViewRegisterFormsUseCase viewRegisterFormsUseCase;
    private final ViewRegisterFormDetailsUseCase viewRegisterFormDetailsUseCase;

    public RegisterFormController(ViewRegisterFormsUseCase viewRegisterFormsUseCase, ViewRegisterFormDetailsUseCase viewRegisterFormDetailsUseCase) {
        this.viewRegisterFormsUseCase = viewRegisterFormsUseCase;
        this.viewRegisterFormDetailsUseCase = viewRegisterFormDetailsUseCase;
    }
    
    @QueryMapping(name = "registerForms")
    public PageResult<RegisterFormDto> registerForms(@Argument(name = "page") int page, @Argument(name = "size") int size) {
        var query = new ViewRegisterFormsQuery(page, size);
        return viewRegisterFormsUseCase.execute(query);
    }

    @QueryMapping(name = "registerForm")
    public RegisterFormDto registerForm(@Argument UUID id) {
        var query = new ViewRegisterFormDetailsQuery(id);
        return viewRegisterFormDetailsUseCase.execute(query);
    }
}
