package com.sep.vox.interfaces.graphql.controller;


import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewRegisterFormsQuery;
import com.sep.vox.application.port.input.usecase.schooladmin.ViewRegisterFormsUseCase;
import com.sep.vox.domain.dto.registerform.RegisterFormDto;
import com.sep.vox.domain.util.PageResult;

@Controller
public class RegisterFormController {

    private final ViewRegisterFormsUseCase viewRegisterFormsUseCase;

    public RegisterFormController(ViewRegisterFormsUseCase viewRegisterFormsUseCase) {
        this.viewRegisterFormsUseCase = viewRegisterFormsUseCase;
    }
    
    @QueryMapping(name = "registerForms")
    public PageResult<RegisterFormDto> registerForms(@Argument(name = "page") int page, @Argument(name = "size") int size) {
        var query = new ViewRegisterFormsQuery(page, size);
        return viewRegisterFormsUseCase.execute(query);
    }
}
