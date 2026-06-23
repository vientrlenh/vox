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

import com.sep.vox.application.port.input.query.ViewRegisterFormDetailsQuery;
import com.sep.vox.application.port.input.query.ViewRegisterFormsQuery;
import com.sep.vox.application.port.input.usecase.registration.ViewRegisterFormDetailsUseCase;
import com.sep.vox.application.port.input.usecase.registration.ViewRegisterFormsUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RegisterFormDocumentDto;
import com.sep.vox.domain.dto.RegisterFormDto;

import graphql.schema.DataFetchingEnvironment;

@Controller("graphqlRegisterFormController")
public class RegisterFormController {

    private final ViewRegisterFormsUseCase viewRegisterFormsUseCase;
    private final ViewRegisterFormDetailsUseCase viewRegisterFormDetailsUseCase;

    public RegisterFormController(ViewRegisterFormsUseCase viewRegisterFormsUseCase, ViewRegisterFormDetailsUseCase viewRegisterFormDetailsUseCase) {
        this.viewRegisterFormsUseCase = viewRegisterFormsUseCase;
        this.viewRegisterFormDetailsUseCase = viewRegisterFormDetailsUseCase;
    }
    
    @QueryMapping(name = "registerForms")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<RegisterFormDto> registerForms(@Argument(name = "page") int page, @Argument(name = "size") int size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
        var query = new ViewRegisterFormsQuery(page, size);
        return viewRegisterFormsUseCase.execute(query);
    }

    @QueryMapping(name = "registerForm")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public RegisterFormDto registerForm(@Argument(name = "id") UUID id) {
        var query = new ViewRegisterFormDetailsQuery(id);
        return viewRegisterFormDetailsUseCase.execute(query);
    }

    @SchemaMapping(typeName = "RegisterForm", field = "documents")
    public CompletableFuture<List<RegisterFormDocumentDto>> registerFormDocuments(RegisterFormDto form, DataFetchingEnvironment env) {
        DataLoader<UUID, List<RegisterFormDocumentDto>> loader = env.getDataLoader("documentsByRegisterForm");
        if (loader == null) {
            throw new IllegalStateException("Không tìm thấy loader của documentsByRegisterForm");
        }
        return loader.load(form.id());
    } 
}
