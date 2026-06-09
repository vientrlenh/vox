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

import com.sep.vox.application.port.input.command.ListSchoolUsersCommand;
import com.sep.vox.application.port.input.command.ViewSchoolUserCommand;
import com.sep.vox.application.port.input.query.ViewSchoolClassDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolClassesQuery;
import com.sep.vox.application.port.input.query.ViewSchoolsQuery;
import com.sep.vox.application.port.input.query.key.SchoolClassesKey;
import com.sep.vox.application.port.input.usecase.school.ViewSchoolsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.UpdateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassesUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ListSchoolUsersUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUserUseCase;
import com.sep.vox.application.response.input.schoolclass.SchoolClassResponse;
import com.sep.vox.application.response.input.schoolclass.UpdateSchoolClassResponse;
import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.SchoolDtoMapper;
import com.sep.vox.domain.mapper.UserDtoMapper;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;

import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolClassCommandMapper;

import graphql.schema.DataFetchingEnvironment;

@Controller("graphqlSchoolController")
public class SchoolController {

    private final ViewSchoolsUseCase viewSchoolsUseCase;
    private final ViewSchoolClassesUseCase viewSchoolClassesUseCase;
    private final ViewSchoolClassDetailsUseCase viewSchoolClassDetailsUseCase;
    private final UpdateSchoolClassUseCase updateSchoolClassUseCase;
    private final ListSchoolUsersUseCase listSchoolUsersUseCase;
    private final ViewSchoolUserUseCase viewSchoolUserUseCase;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;

    public SchoolController(
            ViewSchoolsUseCase viewSchoolsUseCase,
            ViewSchoolClassesUseCase viewSchoolClassesUseCase,
            ViewSchoolClassDetailsUseCase viewSchoolClassDetailsUseCase,
            UpdateSchoolClassUseCase updateSchoolClassUseCase,
            ListSchoolUsersUseCase listSchoolUsersUseCase,
            ViewSchoolUserUseCase viewSchoolUserUseCase,
            UserRepository userRepository,
            SchoolRepository schoolRepository) {
        this.viewSchoolsUseCase = viewSchoolsUseCase;
        this.viewSchoolClassesUseCase = viewSchoolClassesUseCase;
        this.viewSchoolClassDetailsUseCase = viewSchoolClassDetailsUseCase;
        this.updateSchoolClassUseCase = updateSchoolClassUseCase;
        this.listSchoolUsersUseCase = listSchoolUsersUseCase;
        this.viewSchoolUserUseCase = viewSchoolUserUseCase;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
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

    @QueryMapping(name = "schoolClasses")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolClassResponse> schoolClasses(
            @Argument(name = "page") int page,
            @Argument(name = "size") int size,
            @Argument(name = "search") String search,
            @Argument(name = "status") String status,
            @Argument(name = "languageId") UUID languageId,
            @Argument(name = "schoolGradeId") UUID schoolGradeId) {
        if (page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích cỡ trang yêu cầu không hợp lệ");
        }
        var query = new ViewSchoolClassesQuery(page, size, search, status, languageId, schoolGradeId);
        return viewSchoolClassesUseCase.execute(query);
    }

    @QueryMapping(name = "schoolClass")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolClassResponse schoolClass(@Argument(name = "id") UUID id) {
        var query = new ViewSchoolClassDetailsQuery(id);
        return viewSchoolClassDetailsUseCase.execute(query);
    }

    @MutationMapping(name = "updateSchoolClass")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UpdateSchoolClassResponse updateSchoolClass(
            @Argument(name = "id") UUID id,
            @Argument(name = "input") Map<String, Object> input) {
        var command = UpdateSchoolClassCommandMapper.fromInput(id, input);
        return updateSchoolClassUseCase.execute(command);
    }

    @QueryMapping(name = "schoolUsers")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolUserResponse> schoolUsers(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
        var query = new ListSchoolUsersCommand(schoolId, page, size);
        return listSchoolUsersUseCase.execute(query);
    }

    @QueryMapping(name = "schoolUser")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolUserResponse schoolUser(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "userId") UUID userId) {
        var query = new ViewSchoolUserCommand(schoolId, userId);
        return viewSchoolUserUseCase.execute(query);
    }

    @SchemaMapping(typeName = "SchoolUser", field = "user")
    public UserDto user(SchoolUserResponse schoolUser) {
        if (schoolUser.userId() == null) {
            return null;
        }
        return userRepository.findById(schoolUser.userId())
            .map(UserDtoMapper::toUserDto)
            .orElse(null);
    }

    @SchemaMapping(typeName = "SchoolUser", field = "school")
    public SchoolDto school(SchoolUserResponse schoolUser) {
        if (schoolUser.schoolId() == null) {
            return null;
        }
        return schoolRepository.findById(schoolUser.schoolId())
            .map(SchoolDtoMapper::toSchoolDto)
            .orElse(null);
    }

    @SchemaMapping(typeName = "SchoolUser", field = "startDate")
    public String startDate(SchoolUserResponse schoolUser) {
        return schoolUser.startDate() != null ? schoolUser.startDate().toString() : null;
    }

    @SchemaMapping(typeName = "SchoolUser", field = "endDate")
    public String endDate(SchoolUserResponse schoolUser) {
        return schoolUser.endDate() != null ? schoolUser.endDate().toString() : null;
    }
}
