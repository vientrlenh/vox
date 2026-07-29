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
import com.sep.vox.application.port.input.query.key.UserRolesKey;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMyClassDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMyClassMembersUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMyClassesUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RoleDto;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolClassUserDto;
import com.sep.vox.domain.dto.SchoolGradeDto;
import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.domain.dto.UserDto;

import graphql.schema.DataFetchingEnvironment;

/**
 * Bề mặt GraphQL "lớp của tôi" cho giáo viên — chỉ đọc.
 *
 * Tách khỏi {@link SchoolController} (toàn quyền SCHOOL_ADMIN) để không phải nới
 * quyền trên các type dùng chung. Các DataLoader thì dùng lại nguyên: chúng chỉ
 * gom batch theo id, phân quyền nằm ở @PreAuthorize của từng resolver dưới đây.
 */
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
        requireValidPaging(page, size);
        return viewMyClassesUseCase.execute(new ViewMyClassesQuery(schoolId, search, status, page, size));
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
        requireValidPaging(page, size);
        return viewMyClassMembersUseCase.execute(
            new ViewMyClassMembersQuery(schoolClassId, roleCode, search, page, size));
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
        return loader.load(schoolClass.languageId());
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

    @SchemaMapping(typeName = "MyClassMemberUser", field = "roleCodes")
    @PreAuthorize("hasRole('TEACHER')")
    public CompletableFuture<List<String>> myClassMemberUserRoleCodes(UserDto user, DataFetchingEnvironment env) {
        DataLoader<UserRolesKey, List<RoleDto>> loader = env.getDataLoader("rolesByUser");
        return loader.load(new UserRolesKey(user.id()))
            .thenApply(roles -> roles.stream().map(RoleDto::code).toList());
    }

    private static void requireValidPaging(Integer page, Integer size) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích cỡ trang yêu cầu không hợp lệ");
        }
    }
}
