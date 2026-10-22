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

import com.sep.vox.application.port.input.query.ViewMyClassDetailsQuery;
import com.sep.vox.application.port.input.query.ViewMyClassMembersQuery;
import com.sep.vox.application.port.input.query.ViewMyClassesQuery;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMyClassDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMyClassMembersUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMyClassesUseCase;
import com.sep.vox.application.shared.AppPageRequest;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolClassUserDto;
import com.sep.vox.domain.dto.SchoolGradeDto;
import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.shared.PageResult;

import graphql.schema.DataFetchingEnvironment;

@Controller
public class MyClassController {

    private final ViewMyClassesUseCase viewMyClassesUseCase;
    private final ViewMyClassDetailsUseCase viewMyClassDetailsUseCase;
    private final ViewMyClassMembersUseCase viewMyClassMembersUseCase;

    public MyClassController(
            ViewMyClassesUseCase viewMyClassesUseCase,
            ViewMyClassDetailsUseCase viewMyClassDetailsUseCase,
            ViewMyClassMembersUseCase viewMyClassMembersUseCase) {
        this.viewMyClassesUseCase = viewMyClassesUseCase;
        this.viewMyClassDetailsUseCase = viewMyClassDetailsUseCase;
        this.viewMyClassMembersUseCase = viewMyClassMembersUseCase;
    }

    @QueryMapping(name = "myClasses")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<SchoolClassDto> myClasses(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "search") String search,
            @Argument(name = "status") String status,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        AppPageRequest pageRequest = AppPageRequest.pageRequest(page, size);
        return viewMyClassesUseCase.execute(new ViewMyClassesQuery(schoolId, search, status, pageRequest));
    }

    @QueryMapping(name = "myClass")
    @PreAuthorize("hasRole('TEACHER')")
    public SchoolClassDto myClass(@Argument(name = "id") UUID id) {
        return viewMyClassDetailsUseCase.execute(new ViewMyClassDetailsQuery(id));
    }

    @QueryMapping(name = "myClassMembers")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<SchoolClassUserDto> myClassMembers(
            @Argument(name = "schoolClassId") UUID schoolClassId,
            @Argument(name = "roleCode") String roleCode,
            @Argument(name = "search") String search,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        AppPageRequest pageRequest = AppPageRequest.pageRequest(page, size);
        return viewMyClassMembersUseCase.execute(
            new ViewMyClassMembersQuery(schoolClassId, roleCode, search, pageRequest));
    }

    @SchemaMapping(typeName = "MyClass", field = "schoolGrade")
    @PreAuthorize("hasRole('TEACHER')")
    public CompletableFuture<SchoolGradeDto> myClassSchoolGrade(SchoolClassDto schoolClass, DataFetchingEnvironment env) {
        DataLoader<UUID, SchoolGradeDto> loader = env.getDataLoader("schoolGradeByClass");
        return loader.load(schoolClass.schoolGradeId());
    }

    @SchemaMapping(typeName = "MyClass", field = "language")
    @PreAuthorize("hasRole('TEACHER')")
    public CompletableFuture<SupportedLanguageDto> myClassLanguage(SchoolClassDto schoolClass, DataFetchingEnvironment env) {
        DataLoader<UUID, SupportedLanguageDto> loader = env.getDataLoader("supportedLanguageByClass");
        return loader.load(schoolClass.learningLanguageId());
    }

    @SchemaMapping(typeName = "MyClass", field = "activeMemberCount")
    @PreAuthorize("hasRole('TEACHER')")
    public CompletableFuture<Integer> myClassActiveMemberCount(SchoolClassDto schoolClass, DataFetchingEnvironment env) {
        DataLoader<UUID, Integer> loader = env.getDataLoader("activeMemberCountByClass");
        return loader.load(schoolClass.id());
    }

    @SchemaMapping(typeName = "MyClassMember", field = "user")
    @PreAuthorize("hasRole('TEACHER')")
    public CompletableFuture<UserDto> myClassMemberUser(SchoolClassUserDto member, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userBySchoolClassUser");
        return loader.load(member.userId());
    }

}
