package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewSchoolClassDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolClassUsersQuery;
import com.sep.vox.application.port.input.query.ViewSchoolClassesQuery;
import com.sep.vox.application.port.input.query.ViewSchoolUserDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolUsersBySchoolQuery;
import com.sep.vox.application.port.input.query.ViewSchoolsQuery;
import com.sep.vox.application.port.input.query.key.SchoolClassesKey;
import com.sep.vox.application.port.input.query.key.SchoolUsersKey;
import com.sep.vox.application.port.input.usecase.school.ViewSchoolsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.UpdateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassesUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.ViewSchoolClassUsersUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.UpdateSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUserDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUsersBySchoolUseCase;
import com.sep.vox.application.response.input.schoolclass.UpdateSchoolClassResponse;
import com.sep.vox.application.response.input.schooluser.UpdateSchoolUserResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolClassUserDto;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.SchoolGradeDto;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolClassCommandMapper;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolUserCommandMapper;

import graphql.schema.DataFetchingEnvironment;

@Controller("graphqlSchoolController")
public class SchoolController {

    private final ViewSchoolsUseCase viewSchoolsUseCase;
    private final ViewSchoolClassesUseCase viewSchoolClassesUseCase;
    private final ViewSchoolClassDetailsUseCase viewSchoolClassDetailsUseCase;
    private final ViewSchoolClassUsersUseCase viewSchoolClassUsersUseCase;
    private final UpdateSchoolClassUseCase updateSchoolClassUseCase;
    private final ViewSchoolUsersBySchoolUseCase viewSchoolUsersBySchoolUseCase;
    private final ViewSchoolUserDetailsUseCase viewSchoolUserDetailsUseCase;
    private final UpdateSchoolUserUseCase updateSchoolUserUseCase;

    public SchoolController(
            ViewSchoolsUseCase viewSchoolsUseCase,
            ViewSchoolClassesUseCase viewSchoolClassesUseCase,
            ViewSchoolClassDetailsUseCase viewSchoolClassDetailsUseCase,
            UpdateSchoolClassUseCase updateSchoolClassUseCase,
            ViewSchoolUsersBySchoolUseCase viewSchoolUsersBySchoolUseCase,
            ViewSchoolUserDetailsUseCase viewSchoolUserDetailsUseCase,
            UpdateSchoolUserUseCase updateSchoolUserUseCase,
            ViewSchoolClassUsersUseCase viewSchoolClassUsersUseCase) {
        this.viewSchoolsUseCase = viewSchoolsUseCase;
        this.viewSchoolClassesUseCase = viewSchoolClassesUseCase;
        this.viewSchoolClassDetailsUseCase = viewSchoolClassDetailsUseCase;
        this.viewSchoolClassUsersUseCase = viewSchoolClassUsersUseCase;
        this.updateSchoolClassUseCase = updateSchoolClassUseCase;
        this.viewSchoolUsersBySchoolUseCase = viewSchoolUsersBySchoolUseCase;
        this.viewSchoolUserDetailsUseCase = viewSchoolUserDetailsUseCase;
        this.updateSchoolUserUseCase = updateSchoolUserUseCase;
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
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public CompletableFuture<List<SchoolClassDto>> classes(SchoolDto school, @Argument(name = "page") Integer page, @Argument(name = "size") Integer size, DataFetchingEnvironment env) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích cỡ yêu cầu không hợp lệ");
        }
        DataLoader<SchoolClassesKey, List<SchoolClassDto>> loader = env.getDataLoader("schoolClassesBySchool");
        return loader.load(new SchoolClassesKey(school.id(), page, size));
    }


    @SchemaMapping(typeName = "School", field = "users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public CompletableFuture<List<SchoolUserDto>> schoolUsers(SchoolDto school, @Argument(name = "page") Integer page, @Argument(name = "size") Integer size, DataFetchingEnvironment env) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích cỡ yêu cầu không hợp lệ");
        }
        DataLoader<SchoolUsersKey, List<SchoolUserDto>> loader = env.getDataLoader("schoolUsersBySchool");
        return loader.load(new SchoolUsersKey(school.id(), page, size));
    }


    @QueryMapping(name = "schoolUsersBySchool")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolUserDto> schoolUsersBySchool(@Argument(name = "schoolId") UUID schoolId, @Argument(name = "page") Integer page, @Argument(name = "size") Integer size) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
        return viewSchoolUsersBySchoolUseCase.execute(new ViewSchoolUsersBySchoolQuery(schoolId, page, size));
    }

    @QueryMapping(name = "schoolUser")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public SchoolUserDto schoolUser(@Argument(name = "schoolId") UUID schoolId, @Argument(name = "userId") UUID userId) {
        return viewSchoolUserDetailsUseCase.execute(new ViewSchoolUserDetailsQuery(schoolId, userId));
    }

    @QueryMapping(name = "schoolClasses")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolClassDto> schoolClasses(
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "search") String search,
            @Argument(name = "status") String status,
            @Argument(name = "languageId") UUID languageId,
            @Argument(name = "schoolGradeId") UUID schoolGradeId) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích cỡ trang yêu cầu không hợp lệ");
        }
        var query = new ViewSchoolClassesQuery(page, size, search, status, languageId, schoolGradeId);
        return viewSchoolClassesUseCase.execute(query);
    }

    @QueryMapping(name = "schoolClass")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolClassDto schoolClass(@Argument(name = "id") UUID id) {
        var query = new ViewSchoolClassDetailsQuery(id);
        return viewSchoolClassDetailsUseCase.execute(query);
    }

    @SchemaMapping(typeName = "SchoolClass", field = "school")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public CompletableFuture<SchoolDto> school(SchoolClassDto schoolClass, DataFetchingEnvironment env) {
        DataLoader<UUID, SchoolDto> loader = env.getDataLoader("schoolByClass");
        return loader.load(schoolClass.schoolId());
    }

    @SchemaMapping(typeName = "SchoolClass", field = "schoolGrade")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public CompletableFuture<SchoolGradeDto> schoolGrade(SchoolClassDto schoolClass, DataFetchingEnvironment env) {
        DataLoader<UUID, SchoolGradeDto> loader = env.getDataLoader("schoolGradeByClass");
        return loader.load(schoolClass.schoolGradeId());
    }

    @SchemaMapping(typeName = "SchoolClass", field = "language")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public CompletableFuture<SupportedLanguageDto> language(SchoolClassDto schoolClass, DataFetchingEnvironment env) {
        DataLoader<UUID, SupportedLanguageDto> loader = env.getDataLoader("supportedLanguageByClass");
        return loader.load(schoolClass.languageId());
    }

    @QueryMapping(name = "schoolClassUsers")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolClassUserDto> schoolClassUsers(
            @Argument(name = "schoolClassId") UUID schoolClassId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích cỡ trang yêu cầu không hợp lệ");
        }
        return viewSchoolClassUsersUseCase.execute(new ViewSchoolClassUsersQuery(schoolClassId, page, size));
    }

    @SchemaMapping(typeName = "SchoolClassUser", field = "user")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public CompletableFuture<UserDto> user(SchoolClassUserDto schoolClassUser, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userBySchoolClassUser");
        return loader.load(schoolClassUser.userId());
    }

    @MutationMapping(name = "updateSchoolClass")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UpdateSchoolClassResponse updateSchoolClass(
            @Argument(name = "id") UUID id,
            @Argument(name = "input") Map<String, Object> input) {
        var command = UpdateSchoolClassCommandMapper.fromInput(id, input);
        return updateSchoolClassUseCase.execute(command);
    }


    @MutationMapping(name = "updateSchoolUser")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UpdateSchoolUserResponse updateSchoolUser(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "userId") UUID userId,
            @Argument(name = "input") Map<String, Object> input) {
        var command = UpdateSchoolUserCommandMapper.fromInput(schoolId, userId, input);
        return updateSchoolUserUseCase.execute(command);
    }
}
