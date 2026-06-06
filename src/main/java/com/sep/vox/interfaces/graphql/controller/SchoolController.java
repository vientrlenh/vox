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

import com.sep.vox.application.port.input.query.ViewSchoolsQuery;
import com.sep.vox.application.port.input.query.key.SchoolClassesKey;
import com.sep.vox.application.port.input.usecase.school.ViewSchoolsUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.SchoolUserDto;

import graphql.schema.DataFetchingEnvironment;

@Controller("graphqlSchoolController")
public class SchoolController {

    private final ViewSchoolsUseCase viewSchoolsUseCase;

    public SchoolController(ViewSchoolsUseCase viewSchoolsUseCase) {
        this.viewSchoolsUseCase = viewSchoolsUseCase;
    }

    @QueryMapping(name = "schools")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<SchoolDto> schools(@Argument(name = "page") Integer page, @Argument(name = "size") Integer size) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích cỡ trang yêu cầu không hợp lệ");
        }
        var query = new ViewSchoolsQuery(page, size);
        return viewSchoolsUseCase.execute(query);
    }

    @SchemaMapping(typeName = "School", field = "classes")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public CompletableFuture<List<SchoolClassDto>> classes(SchoolDto school, @Argument(name = "page") int page, @Argument(name = "size") int size, DataFetchingEnvironment env) {
        DataLoader<SchoolClassesKey, List<SchoolClassDto>> loader = env.getDataLoader("schoolClassesBySchool");
        return loader.load(new SchoolClassesKey(school.id(), page, size));
    }


    @QueryMapping(name = "schoolUsers")
    public PageResult<SchoolUserDto> schoolUsers(@Argument(name = "schoolId") UUID schoolId, @Argument(name = "page") Integer page, @Argument(name = "size") Integer size) {
        return null;
    }
}
