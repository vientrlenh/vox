package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.application.port.input.query.*;
import com.sep.vox.application.port.input.usecase.schoolgrade.UpdateSchoolGradeUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.ViewSchoolGradeDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.ViewSchoolGradesUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.UpdateSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomsUseCase;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolGradeRequest;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolRequest;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolRoomRequest;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolCommandMapper;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolGradeCommandMapper;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolRoomCommandMapper;

import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.dto.SchoolUserDto;

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
import com.sep.vox.application.port.input.query.ViewSchoolClassUsersQuery;
import com.sep.vox.application.port.input.query.ViewSchoolClassesQuery;
import com.sep.vox.application.port.input.query.ViewSchoolsQuery;
import com.sep.vox.application.port.input.query.key.SchoolClassGradeKey;
import com.sep.vox.application.port.input.query.key.SchoolClassesKey;
import com.sep.vox.application.port.input.query.key.SchoolUsersKey;
import com.sep.vox.application.port.input.usecase.school.ViewSchoolsUseCase;
import com.sep.vox.application.port.input.usecase.school.UpdateSchoolUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.UpdateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.ViewSchoolClassUsersUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassesUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ListSchoolUsersUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.UpdateSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUserUseCase;
import com.sep.vox.application.response.input.schoolclassuser.SchoolClassUserResponse;
import com.sep.vox.application.response.input.schoolclass.UpdateSchoolClassResponse;
import com.sep.vox.application.response.input.schooluser.UpdateSchoolUserResponse;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.SchoolDtoMapper;
import com.sep.vox.domain.repository.SchoolRepository;

import com.sep.vox.domain.dto.SchoolGradeDto;
import com.sep.vox.domain.dto.SupportedLanguageDto;
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
    private final ListSchoolUsersUseCase listSchoolUsersUseCase;
    private final ViewSchoolUserUseCase viewSchoolUserUseCase;
    private final UpdateSchoolUserUseCase updateSchoolUserUseCase;
    private final SchoolRepository schoolRepository;
    private final UpdateSchoolUseCase updateSchoolUseCase;
    private final ViewSchoolRoomDetailsUseCase viewSchoolRoomDetailsUseCase;
    private final ViewSchoolRoomsUseCase viewSchoolRoomsUseCase;
    private final UpdateSchoolRoomUseCase updateSchoolRoomUseCase;
    private final UpdateSchoolGradeUseCase updateSchoolGradeUseCase;
    private final ViewSchoolGradesUseCase viewSchoolGradesUseCase;
    private final ViewSchoolGradeDetailsUseCase viewSchoolGradeDetailsUseCase;

    public SchoolController(ViewSchoolsUseCase viewSchoolsUseCase, ViewSchoolClassesUseCase viewSchoolClassesUseCase, ViewSchoolClassDetailsUseCase viewSchoolClassDetailsUseCase, ViewSchoolClassUsersUseCase viewSchoolClassUsersUseCase, UpdateSchoolClassUseCase updateSchoolClassUseCase, ListSchoolUsersUseCase listSchoolUsersUseCase, ViewSchoolUserUseCase viewSchoolUserUseCase, UpdateSchoolUserUseCase updateSchoolUserUseCase, SchoolRepository schoolRepository, UpdateSchoolUseCase updateSchoolUseCase, ViewSchoolRoomDetailsUseCase viewSchoolRoomDetailsUseCase, ViewSchoolRoomsUseCase viewSchoolRoomsUseCase, UpdateSchoolRoomUseCase updateSchoolRoomUseCase, UpdateSchoolGradeUseCase updateSchoolGradeUseCase, ViewSchoolGradesUseCase viewSchoolGradesUseCase, ViewSchoolGradeDetailsUseCase viewSchoolGradeDetailsUseCase) {
        this.viewSchoolsUseCase = viewSchoolsUseCase;
        this.viewSchoolClassesUseCase = viewSchoolClassesUseCase;
        this.viewSchoolClassDetailsUseCase = viewSchoolClassDetailsUseCase;
        this.viewSchoolClassUsersUseCase = viewSchoolClassUsersUseCase;
        this.updateSchoolClassUseCase = updateSchoolClassUseCase;
        this.listSchoolUsersUseCase = listSchoolUsersUseCase;
        this.viewSchoolUserUseCase = viewSchoolUserUseCase;
        this.updateSchoolUserUseCase = updateSchoolUserUseCase;
        this.schoolRepository = schoolRepository;
        this.updateSchoolUseCase = updateSchoolUseCase;
        this.viewSchoolRoomDetailsUseCase = viewSchoolRoomDetailsUseCase;
        this.viewSchoolRoomsUseCase = viewSchoolRoomsUseCase;
        this.updateSchoolRoomUseCase = updateSchoolRoomUseCase;
        this.updateSchoolGradeUseCase = updateSchoolGradeUseCase;
        this.viewSchoolGradesUseCase = viewSchoolGradesUseCase;
        this.viewSchoolGradeDetailsUseCase = viewSchoolGradeDetailsUseCase;
    }

    @QueryMapping(name = "school")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolDto school(@Argument(name = "id") UUID id) {
        return schoolRepository.findById(id)
                .map(SchoolDtoMapper::toSchoolDto)
                .orElse(null);
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
    public CompletableFuture<List<SchoolClassDto>> classes(SchoolDto school, @Argument(name = "page") Integer page, @Argument(name = "size") Integer size, DataFetchingEnvironment env) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích cỡ yêu cầu không hợp lệ");
        }
        DataLoader<SchoolClassesKey, List<SchoolClassDto>> loader = env.getDataLoader("schoolClassesBySchool");
        return loader.load(new SchoolClassesKey(school.id(), page, size));
    }

    @SchemaMapping(typeName = "School", field = "schoolUsers")
    public CompletableFuture<List<SchoolUserDto>> schoolUsers(SchoolDto school, @Argument(name = "page") Integer page, @Argument(name = "size") Integer size, DataFetchingEnvironment env) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích cỡ yêu cầu không hợp lệ");
        }
        DataLoader<SchoolUsersKey, List<SchoolUserDto>> loader = env.getDataLoader("schoolUsersBySchool");
        return loader.load(new SchoolUsersKey(school.id(), page, size));
    }

    @SchemaMapping(typeName = "School", field = "users")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolUserDto> users(SchoolDto school, @Argument(name = "page") Integer page, @Argument(name = "size") Integer size) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
        return listSchoolUsersUseCase.execute(new ListSchoolUsersCommand(school.id(), page, size));
    }

    @SchemaMapping(typeName = "School", field = "user")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolUserDto user(SchoolDto school, @Argument(name = "userId") UUID userId) {
        return viewSchoolUserUseCase.execute(new ViewSchoolUserCommand(school.id(), userId));
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
        if (page <= 0 || size <= 0) {
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
        DataLoader<UUID, SchoolDto> loader = env.getDataLoader("schoolById");
        return loader.load(schoolClass.schoolId());
    }

    @SchemaMapping(typeName = "SchoolClass", field = "schoolGrade")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public CompletableFuture<SchoolGradeDto> schoolGrade(SchoolClassDto schoolClass, DataFetchingEnvironment env) {
        DataLoader<SchoolClassGradeKey, SchoolGradeDto> loader = env.getDataLoader("schoolGradeByClass");
        return loader.load(new SchoolClassGradeKey(schoolClass.schoolGradeId(), schoolClass.schoolId()));
    }

    @SchemaMapping(typeName = "SchoolClass", field = "language")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public CompletableFuture<SupportedLanguageDto> language(SchoolClassDto schoolClass, DataFetchingEnvironment env) {
        DataLoader<UUID, SupportedLanguageDto> loader = env.getDataLoader("supportedLanguageById");
        return loader.load(schoolClass.languageId());
    }

    @QueryMapping(name = "schoolClassUsers")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolClassUserResponse> schoolClassUsers(
            @Argument(name = "schoolClassId") UUID schoolClassId,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích cỡ trang yêu cầu không hợp lệ");
        }
        return viewSchoolClassUsersUseCase.execute(new ViewSchoolClassUsersQuery(schoolClassId, page, size));
    }

    @SchemaMapping(typeName = "SchoolClassUser", field = "user")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public CompletableFuture<UserDto> user(SchoolClassUserResponse schoolClassUser, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userById");
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

    @SchemaMapping(typeName = "SchoolUser", field = "user")
    public CompletableFuture<UserDto> user(SchoolUserDto schoolUser, DataFetchingEnvironment env) {
        if (schoolUser.userId() == null) return CompletableFuture.completedFuture(null);
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userById");
        return loader.load(schoolUser.userId());
    }

    @SchemaMapping(typeName = "SchoolUser", field = "startDate")
    public String startDate(SchoolUserDto schoolUser) {
        return schoolUser.startDate() != null ? schoolUser.startDate().toString() : null;
    }

    @SchemaMapping(typeName = "SchoolUser", field = "endDate")
    public String endDate(SchoolUserDto schoolUser) {
        return schoolUser.endDate() != null ? schoolUser.endDate().toString() : null;
    }

    @SchemaMapping(typeName = "SchoolUser", field = "fullName")
    public CompletableFuture<String> fullName(SchoolUserDto schoolUser, DataFetchingEnvironment env) {
        if (schoolUser.userId() == null) return CompletableFuture.completedFuture(null);
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userById");
        return loader.load(schoolUser.userId()).thenApply(u -> u != null ? u.fullName() : null);
    }

    @SchemaMapping(typeName = "SchoolUser", field = "email")
    public CompletableFuture<String> email(SchoolUserDto schoolUser, DataFetchingEnvironment env) {
        if (schoolUser.userId() == null) return CompletableFuture.completedFuture(null);
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userById");
        return loader.load(schoolUser.userId()).thenApply(u -> u != null ? u.email() : null);
    }

    @SchemaMapping(typeName = "SchoolUser", field = "phone")
    public CompletableFuture<String> phone(SchoolUserDto schoolUser, DataFetchingEnvironment env) {
        if (schoolUser.userId() == null) return CompletableFuture.completedFuture(null);
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userById");
        return loader.load(schoolUser.userId()).thenApply(u -> u != null ? u.phone() : null);
    }

    @SchemaMapping(typeName = "SchoolUser", field = "address")
    public CompletableFuture<String> address(SchoolUserDto schoolUser, DataFetchingEnvironment env) {
        if (schoolUser.userId() == null) return CompletableFuture.completedFuture(null);
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userById");
        return loader.load(schoolUser.userId()).thenApply(u -> u != null ? u.address() : null);
    }

    @SchemaMapping(typeName = "SchoolUser", field = "dateOfBirth")
    public CompletableFuture<String> dateOfBirth(SchoolUserDto schoolUser, DataFetchingEnvironment env) {
        if (schoolUser.userId() == null) return CompletableFuture.completedFuture(null);
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userById");
        return loader.load(schoolUser.userId()).thenApply(u -> u != null ? u.dateOfBirth() : null);
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


    //Nhập id school vào update gì đó => SchoolID => check
    @MutationMapping(name = "updateSchool")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchool(@Argument(name = "id") UUID id, @Argument(name = "input") UpdateSchoolRequest request) {

        var command = UpdateSchoolCommandMapper.fromRequest(id, request);

        return updateSchoolUseCase.execute(command);


    }


    //============================SCHOOL ROOM===========================================
    //Nhập id của roomId
    @QueryMapping(name = "schoolRoom")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolRoomFromDto schoolRoom(@Argument(name = "id") UUID id) {
        var query = new ViewSchoolRoomDetailsQuery(id);
        return viewSchoolRoomDetailsUseCase.execute(query);
    }

    //Nhập Id chủa schoolId
    @QueryMapping(name = "schoolRooms")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolRoomFromDto> schoolRooms(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        int pageNumber = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0) ? size : 10;

        var query = new ViewSchoolRoomsQuery(schoolId, new PageRequest(pageNumber, pageSize));
        return viewSchoolRoomsUseCase.execute(query);
    }

    // nhập Id của room muốn đổi => update (200) trả về school Id
    @MutationMapping(name = "updateSchoolRoom")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolRoom(
            @Argument(name = "id") UUID id,
            @Argument(name = "input") UpdateSchoolRoomRequest request
    ) {
        // Mapper truyền cả id và request
        var command = UpdateSchoolRoomCommandMapper.fromRequest(id, request);
        return updateSchoolRoomUseCase.execute(command);
    }


    //=======================SCHOOL USER=======================
    @QueryMapping(name = "schoolUsers")
    public PageResult<SchoolUserDto> schoolUsers(@Argument(name = "schoolId") UUID schoolId, @Argument(name = "page") Integer page, @Argument(name = "size") Integer size) {
        return null;
    }

    //========================SCHOOL GRADE =======================
    @MutationMapping(name = "updateSchoolGrade")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolGrade(
            @Argument(name = "id") UUID id,
            @Argument(name = "input") UpdateSchoolGradeRequest request
    ) {
        var command = UpdateSchoolGradeCommandMapper.fromRequest(id, request);
        return updateSchoolGradeUseCase.execute(command);
    }

    //Nhập id của gradeId
    @QueryMapping(name = "schoolGrade")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolGradeDto schoolGrade(@Argument(name = "id") UUID id) {
        var query = new ViewSchoolGradeDetailsQuery(id);
        return viewSchoolGradeDetailsUseCase.execute(query);
    }

    @QueryMapping(name = "schoolGrades")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolGradeDto> schoolGrades(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        // Validate tham số phân trang
        int pageNumber = (page != null && page > 0) ? page : 1;
        int pageSize = (size != null && size > 0) ? size : 10;

        var query = new ViewSchoolGradesQuery(schoolId, new PageRequest(pageNumber, pageSize));

        return viewSchoolGradesUseCase.execute(query);

    }
}