package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewInvoicesQuery;
import com.sep.vox.application.port.input.usecase.subscription.ViewInvoicesUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.InvoiceDto;

@Controller("graphqlInvoiceController")
public class InvoiceController {
    
    private final ViewInvoicesUseCase viewInvoicesUseCase;

    public InvoiceController(ViewInvoicesUseCase viewInvoicesUseCase) {
        this.viewInvoicesUseCase = viewInvoicesUseCase;
    }

    @QueryMapping(name = "invoices")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<InvoiceDto> invoices(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        validatePageSize(page, size);
        return viewInvoicesUseCase.execute(new ViewInvoicesQuery(schoolId, page, size));
    }

    private void validatePageSize(Integer page, Integer size) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Trang hoặc kích thước yêu cầu phải lớn hơn 0");
        }
    }
}
