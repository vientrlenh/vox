package com.sep.vox.interfaces.graphql.controller;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewSchoolClassesQuery;
import com.sep.vox.application.port.input.usecase.schooladmin.ViewSchoolClassesUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassDto;

@Controller("graphqlSchoolClassController")
public class SchoolClassController {

    private final ViewSchoolClassesUseCase viewSchoolClassesUseCase;

    public SchoolClassController(ViewSchoolClassesUseCase viewSchoolClassesUseCase) {
        this.viewSchoolClassesUseCase = viewSchoolClassesUseCase;
    }

    @QueryMapping(name = "schoolClasses")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolClassDto> schoolClasses(
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
        var query = new ViewSchoolClassesQuery(page, size);
        return viewSchoolClassesUseCase.execute(query);
    }
}
