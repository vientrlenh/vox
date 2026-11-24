package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.interfaces.graphql.dataloader.SchoolGraphQLDataLoaderConfig;
import com.sep.vox.interfaces.graphql.dataloader.SupportedLanguageGraphQLDataLoaderConfig;
import com.sep.vox.interfaces.graphql.dataloader.GraphQLDataLoaders;
import com.sep.vox.interfaces.graphql.dataloader.UserGraphQLDataLoaderConfig;
import com.sep.vox.application.port.input.command.gradelevel.UpdateGradeLevelCommand;
import com.sep.vox.application.port.input.command.schoolclass.UpdateSchoolClassCommand;
import com.sep.vox.application.port.input.command.school.UpdateSchoolCommand;
import com.sep.vox.application.port.input.command.schoolgrade.UpdateSchoolGradeCommand;
import com.sep.vox.application.port.input.command.schoolroom.UpdateSchoolRoomCommand;
import com.sep.vox.application.port.input.command.schooluser.UpdateSchoolUserCommand;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.schoolclass.ViewMySchoolClassesQuery;
import com.sep.vox.application.port.input.query.schoolclass.ViewSchoolClassDetailsQuery;
import com.sep.vox.application.port.input.query.schoolclassuser.ViewSchoolClassUsersQuery;
import com.sep.vox.application.port.input.query.schoolclass.ViewSchoolClassesByUserQuery;
import com.sep.vox.application.port.input.query.schoolclass.ViewSchoolClassesQuery;
import com.sep.vox.application.port.input.query.schooldirectory.ViewSchoolDirectoryCursorPageQuery;
import com.sep.vox.application.port.input.query.schooldirectory.ViewSchoolDirectoryDetailsQuery;
import com.sep.vox.application.port.input.query.schooldirectory.ViewSchoolDirectoryPageQuery;
import com.sep.vox.application.port.input.query.schoolgrade.ViewSchoolGradeDetailsQuery;
import com.sep.vox.application.port.input.query.gradelevel.ViewGradeLevelDetailsQuery;
import com.sep.vox.application.port.input.query.gradelevel.ViewGradeLevelsQuery;
import com.sep.vox.application.port.input.query.schoolgrade.ViewSchoolGradesQuery;
import com.sep.vox.application.port.input.query.schoolroom.ViewSchoolRoomDetailsQuery;
import com.sep.vox.application.port.input.query.schoolroom.ViewSchoolRoomsQuery;
import com.sep.vox.application.port.input.query.schooluser.ViewSchoolStudentsBySchoolQuery;
import com.sep.vox.application.port.input.query.schooluser.ViewSchoolTeachersBySchoolQuery;
import com.sep.vox.application.port.input.query.schooluser.ViewSchoolUserDetailsQuery;
import com.sep.vox.application.port.input.query.schooluser.ViewSchoolUsersBySchoolQuery;
import com.sep.vox.application.port.input.query.school.ViewSchoolsQuery;
import com.sep.vox.interfaces.graphql.key.SchoolClassesKey;
import com.sep.vox.interfaces.graphql.key.SchoolUsersKey;
import com.sep.vox.application.port.input.usecase.school.ListSchoolsWithOnGoingAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.school.UpdateSchoolUseCase;
import com.sep.vox.application.port.input.usecase.school.ViewSchoolsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.UpdateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewMySchoolClassesUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassesByUserUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.ViewSchoolClassesUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.ViewSchoolClassUsersUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.ViewSchoolDirectoryCursorPageUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.ViewSchoolDirectoryDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.ViewSchoolDirectoryPageUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.UpdateSchoolGradeUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.ViewSchoolGradeDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.ViewSchoolGradesUseCase;
import com.sep.vox.application.port.input.usecase.gradelevel.UpdateGradeLevelUseCase;
import com.sep.vox.application.port.input.usecase.gradelevel.ViewGradeLevelDetailsUseCase;
import com.sep.vox.application.port.input.usecase.gradelevel.ViewGradeLevelsUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.UpdateSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomsUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.UpdateSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolStudentsBySchoolUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolTeachersBySchoolUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUserDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUsersBySchoolUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUsersForRequesterUseCase;
import com.sep.vox.application.response.input.schoolclass.UpdateSchoolClassResponse;
import com.sep.vox.application.response.input.schooluser.UpdateSchoolUserResponse;
import com.sep.vox.domain.common.CursorPage;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolClassUserDto;
import com.sep.vox.domain.dto.SchoolDirectoryDto;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.SchoolGradeDto;
import com.sep.vox.domain.dto.GradeLevelDto;
import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.interfaces.graphql.input.gradelevel.UpdateGradeLevelRequest;
import com.sep.vox.interfaces.graphql.input.schoolgrade.UpdateSchoolGradeRequest;
import com.sep.vox.interfaces.graphql.input.school.UpdateSchoolRequest;
import com.sep.vox.interfaces.graphql.input.schoolroom.UpdateSchoolRoomRequest;
import com.sep.vox.interfaces.graphql.input.schoolclass.UpdateSchoolClassInput;
import com.sep.vox.interfaces.graphql.input.schooluser.UpdateSchoolUserInput;
import com.sep.vox.interfaces.shared.PageArguments;

import graphql.schema.DataFetchingEnvironment;

@Controller
@RequiredArgsConstructor
public class GraphQLSchoolController {

        private final ViewSchoolsUseCase viewSchoolsUseCase;
    private final ViewSchoolClassesUseCase viewSchoolClassesUseCase;
    private final ViewSchoolClassesByUserUseCase viewSchoolClassesByUserUseCase;
    private final ViewMySchoolClassesUseCase viewMySchoolClassesUseCase;
    private final ViewSchoolClassDetailsUseCase viewSchoolClassDetailsUseCase;
    private final ViewSchoolClassUsersUseCase viewSchoolClassUsersUseCase;
    private final UpdateSchoolClassUseCase updateSchoolClassUseCase;
    private final ViewSchoolUsersBySchoolUseCase viewSchoolUsersBySchoolUseCase;
    private final ViewSchoolUsersForRequesterUseCase viewSchoolUsersForRequesterUseCase;
    private final ViewSchoolStudentsBySchoolUseCase viewSchoolStudentsBySchoolUseCase;
    private final ViewSchoolTeachersBySchoolUseCase viewSchoolTeachersBySchoolUseCase;
    private final ViewSchoolUserDetailsUseCase viewSchoolUserDetailsUseCase;
    private final UpdateSchoolUserUseCase updateSchoolUserUseCase;
    private final UpdateSchoolUseCase updateSchoolUseCase;
    private final ViewSchoolRoomDetailsUseCase viewSchoolRoomDetailsUseCase;
    private final ViewSchoolRoomsUseCase viewSchoolRoomsUseCase;
    private final UpdateSchoolRoomUseCase updateSchoolRoomUseCase;
    private final UpdateSchoolGradeUseCase updateSchoolGradeUseCase;
    private final ViewSchoolGradesUseCase viewSchoolGradesUseCase;
    private final ViewSchoolGradeDetailsUseCase viewSchoolGradeDetailsUseCase;
    private final ViewGradeLevelsUseCase viewGradeLevelsUseCase;
    private final ViewGradeLevelDetailsUseCase viewGradeLevelDetailsUseCase;
    private final UpdateGradeLevelUseCase updateGradeLevelUseCase;
    private final ViewSchoolDirectoryCursorPageUseCase viewSchoolDirectoryCursorPageUseCase;
    private final ViewSchoolDirectoryPageUseCase viewSchoolDirectoryPageUseCase;
    private final ViewSchoolDirectoryDetailsUseCase viewSchoolDirectoryDetailsUseCase;
    private final ListSchoolsWithOnGoingAssessmentUseCase listSchoolsWithOnGoingAssessmentUseCase;



    // schoolDebtEvents moved to GraphQLSchoolBalanceController: debt is the other side of the school wallet (a
    // negative balance IS the debt), so it belongs with the wallet statement, not with classes and rooms.

    @QueryMapping(name = "schools")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<SchoolDto> schools(
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "search") String search,
            @Argument(name = "isActive") Boolean isActive) {
        PageArguments.validate(page, size);
        ViewSchoolsQuery query = new ViewSchoolsQuery(page, size, search, isActive);
        return viewSchoolsUseCase.execute(query);
    }


    @SchemaMapping(typeName = "School", field = "classes")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public CompletableFuture<List<SchoolClassDto>> classes(SchoolDto school, @Argument(name = "page") Integer page, @Argument(name = "size") Integer size, DataFetchingEnvironment env) {
        PageArguments.validate(page, size);
        DataLoader<SchoolClassesKey, List<SchoolClassDto>> loader = GraphQLDataLoaders.require(env, SchoolGraphQLDataLoaderConfig.SCHOOL_CLASSES_BY_SCHOOL_ID);
        return loader.load(new SchoolClassesKey(school.id(), page, size));
    }


    @SchemaMapping(typeName = "School", field = "users")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public CompletableFuture<List<SchoolUserDto>> schoolUsers(SchoolDto school, @Argument(name = "page") Integer page, @Argument(name = "size") Integer size, DataFetchingEnvironment env) {
        PageArguments.validate(page, size);
        DataLoader<SchoolUsersKey, List<SchoolUserDto>> loader = GraphQLDataLoaders.require(env, SchoolGraphQLDataLoaderConfig.SCHOOL_USERS_BY_SCHOOL_ID);
        return loader.load(new SchoolUsersKey(school.id(), page, size));
    }


    @QueryMapping(name = "schoolUsersBySchool")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolUserDto> schoolUsersBySchool(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "search") String search,
            @Argument(name = "roleId") UUID roleId,
            @Argument(name = "roleCode") String roleCode,
            @Argument(name = "status") UserStatus status,
            @Argument(name = "excludeClassId") UUID excludeClassId) {
        PageArguments.validate(page, size);
        return viewSchoolUsersBySchoolUseCase.execute(new ViewSchoolUsersBySchoolQuery(
            schoolId, page, size, search, roleId, roleCode, status, excludeClassId));
    }

    @QueryMapping(name = "schoolUsersForRequester")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<SchoolUserDto> schoolUsersForRequester(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "search") String search,
            @Argument(name = "roleId") UUID roleId,
            @Argument(name = "status") UserStatus status) {
        PageArguments.validate(page, size);
        return viewSchoolUsersForRequesterUseCase.execute(new ViewSchoolUsersBySchoolQuery(schoolId, page, size, search, roleId, status));
    }

    @QueryMapping(name = "schoolStudentsBySchool")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolUserDto> schoolStudentsBySchool(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "search") String search,
            @Argument(name = "status") UserStatus status) {
        PageArguments.validate(page, size);
        return viewSchoolStudentsBySchoolUseCase.execute(new ViewSchoolStudentsBySchoolQuery(schoolId, page, size, search, status));
    }

    @QueryMapping(name = "schoolTeachersBySchool")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolUserDto> schoolTeachersBySchool(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "search") String search,
            @Argument(name = "status") UserStatus status) {
        PageArguments.validate(page, size);
        return viewSchoolTeachersBySchoolUseCase.execute(new ViewSchoolTeachersBySchoolQuery(schoolId, page, size, search, status));
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
            @Argument(name = "status") SchoolClassStatus status,
            @Argument(name = "languageId") UUID languageId,
            @Argument(name = "schoolGradeId") UUID schoolGradeId) {
        PageArguments.validate(page, size);
        ViewSchoolClassesQuery query = new ViewSchoolClassesQuery(page, size, search, status, languageId, schoolGradeId);
        return viewSchoolClassesUseCase.execute(query);
    }

    @QueryMapping(name = "schoolClass")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolClassDto schoolClass(@Argument(name = "id") UUID id) {
        ViewSchoolClassDetailsQuery query = new ViewSchoolClassDetailsQuery(id);
        return viewSchoolClassDetailsUseCase.execute(query);
    }

    @SchemaMapping(typeName = "SchoolClass", field = "school")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public CompletableFuture<SchoolDto> school(SchoolClassDto schoolClass, DataFetchingEnvironment env) {
        DataLoader<UUID, SchoolDto> loader = GraphQLDataLoaders.require(env, SchoolGraphQLDataLoaderConfig.SCHOOL_BY_SCHOOL_ID);
        return loader.load(schoolClass.schoolId());
    }

    @SchemaMapping(typeName = "SchoolClass", field = "schoolGrade")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public CompletableFuture<SchoolGradeDto> schoolGrade(SchoolClassDto schoolClass, DataFetchingEnvironment env) {
        DataLoader<UUID, SchoolGradeDto> loader = GraphQLDataLoaders.require(env, SchoolGraphQLDataLoaderConfig.SCHOOL_GRADE_BY_SCHOOL_GRADE_ID);
        return loader.load(schoolClass.schoolGradeId());
    }

    @SchemaMapping(typeName = "SchoolClass", field = "language")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public CompletableFuture<SupportedLanguageDto> language(SchoolClassDto schoolClass, DataFetchingEnvironment env) {
        DataLoader<UUID, SupportedLanguageDto> loader = GraphQLDataLoaders.require(env, SupportedLanguageGraphQLDataLoaderConfig.SUPPORTED_LANGUAGE_BY_SUPPORTED_LANGUAGE_ID);
        return loader.load(schoolClass.languageId());
    }

    @QueryMapping(name = "schoolClassUsers")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolClassUserDto> schoolClassUsers(
            @Argument(name = "schoolClassId") UUID schoolClassId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewSchoolClassUsersUseCase.execute(new ViewSchoolClassUsersQuery(schoolClassId, page, size));
    }

    @QueryMapping(name = "schoolClassesByUser")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolClassDto> schoolClassesByUser(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "userId") UUID userId,
            @Argument(name = "status") SchoolClassStatus status,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewSchoolClassesByUserUseCase.execute(new ViewSchoolClassesByUserQuery(schoolId, userId, status, page, size));
    }

    @QueryMapping(name = "mySchoolClasses")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<SchoolClassDto> mySchoolClasses(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "status") SchoolClassStatus status,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewMySchoolClassesUseCase.execute(new ViewMySchoolClassesQuery(schoolId, status, page, size));
    }

    @SchemaMapping(typeName = "SchoolClassUser", field = "user")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public CompletableFuture<UserDto> user(SchoolClassUserDto schoolClassUser, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = GraphQLDataLoaders.require(env, UserGraphQLDataLoaderConfig.USER_BY_USER_ID);
        return loader.load(schoolClassUser.userId());
    }

    @SchemaMapping(typeName = "SchoolUser", field = "user")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public CompletableFuture<UserDto> schoolUserUser(SchoolUserDto schoolUser, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = GraphQLDataLoaders.require(env, UserGraphQLDataLoaderConfig.USER_BY_USER_ID);
        return loader.load(schoolUser.userId());
    }

    @MutationMapping(name = "updateSchoolClass")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UpdateSchoolClassResponse updateSchoolClass(
            @Argument(name = "id") UUID id,
            @Argument(name = "input") UpdateSchoolClassInput input) {
        UpdateSchoolClassCommand command = UpdateSchoolClassInput.toCommand(id, input);
        return updateSchoolClassUseCase.execute(command);
    }


    @MutationMapping(name = "updateSchoolUser")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UpdateSchoolUserResponse updateSchoolUser(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "userId") UUID userId,
            @Argument(name = "input") UpdateSchoolUserInput input) {
        UpdateSchoolUserCommand command = UpdateSchoolUserInput.toCommand(schoolId, userId, input);
        return updateSchoolUserUseCase.execute(command);
    }

    // Enter the school id to update something => SchoolID => check
    @MutationMapping(name = "updateSchool")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchool(@Argument(name = "id") UUID id, @Argument(name = "input") UpdateSchoolRequest request) {

        UpdateSchoolCommand command = UpdateSchoolRequest.toCommand(id, request);

        return updateSchoolUseCase.execute(command);


    }


    //============================SCHOOL ROOM===========================================
    // Enter the room id
    @QueryMapping(name = "schoolRoom")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolRoomFromDto schoolRoom(@Argument(name = "id") UUID id) {
        ViewSchoolRoomDetailsQuery query = new ViewSchoolRoomDetailsQuery(id);
        return viewSchoolRoomDetailsUseCase.execute(query);
    }

    // Enter the school id. Also open to TEACHER because teachers need to pick a room when organizing a
    // class test; ViewSchoolRoomsUseCase itself blocks callers who do not belong to the requested school.
    @QueryMapping(name = "schoolRooms")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<SchoolRoomFromDto> schoolRooms(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        PageArguments.validate(page, size);

        ViewSchoolRoomsQuery query = new ViewSchoolRoomsQuery(schoolId, page, size);
        return viewSchoolRoomsUseCase.execute(query);
    }

    // Enter the id of the room to change => update (200) returns the school id
    @MutationMapping(name = "updateSchoolRoom")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolRoom(
            @Argument(name = "id") UUID id,
            @Argument(name = "input") UpdateSchoolRoomRequest request
    ) {
        UpdateSchoolRoomCommand command = UpdateSchoolRoomRequest.toCommand(id, request);
        return updateSchoolRoomUseCase.execute(command);
    }


    //=======================SCHOOL USER=======================
    //========================SCHOOL GRADE =======================
    @MutationMapping(name = "updateSchoolGrade")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public UUID updateSchoolGrade(
            @Argument(name = "id") UUID id,
            @Argument(name = "input") UpdateSchoolGradeRequest request
    ) {
        UpdateSchoolGradeCommand command = UpdateSchoolGradeRequest.toCommand(id, request);
        return updateSchoolGradeUseCase.execute(command);
    }

    // Enter the grade id
    @QueryMapping(name = "schoolGrade")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public SchoolGradeDto schoolGrade(@Argument(name = "schoolId") UUID schoolId, @Argument(name = "id") UUID id) {
        ViewSchoolGradeDetailsQuery query = new ViewSchoolGradeDetailsQuery(schoolId, id);
        return viewSchoolGradeDetailsUseCase.execute(query);
    }

    @QueryMapping(name = "schoolGrades")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolGradeDto> schoolGrades(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "gradeLevelId") UUID gradeLevelId,
            @Argument(name = "status") SchoolGradeStatus status,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        PageArguments.validate(page, size);

        ViewSchoolGradesQuery query = new ViewSchoolGradesQuery(schoolId, gradeLevelId, status, page, size);

        return viewSchoolGradesUseCase.execute(query);
    }

    //======================== GRADE LEVEL (global catalog) =======================
    // Read: any signed-in role. Write: SYSTEM_ADMIN only -- one record affects every school.
    @QueryMapping(name = "gradeLevels")
    public PageResult<GradeLevelDto> gradeLevels(
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "search") String search,
            @Argument(name = "status") GradeLevelStatus status) {
        PageArguments.validate(page, size);
        ViewGradeLevelsQuery query = new ViewGradeLevelsQuery(page, size, search, status);
        return viewGradeLevelsUseCase.execute(query);
    }

    @QueryMapping(name = "gradeLevel")
    public GradeLevelDto gradeLevel(@Argument(name = "gradeLevelId") UUID gradeLevelId) {
        return viewGradeLevelDetailsUseCase.execute(new ViewGradeLevelDetailsQuery(gradeLevelId));
    }

    @MutationMapping(name = "updateGradeLevel")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateGradeLevel(
            @Argument(name = "gradeLevelId") UUID gradeLevelId,
            @Argument(name = "input") UpdateGradeLevelRequest request) {
        UpdateGradeLevelCommand command = UpdateGradeLevelRequest.toCommand(gradeLevelId, request);
        return updateGradeLevelUseCase.execute(command);
    }

    @QueryMapping(name = "schoolDirectoryCursorPage")
    public CursorPage<SchoolDirectoryDto> schoolDirectoryCursorPage(@Argument(name = "cursor") UUID cursor, @Argument(name = "limit") Integer limit) {
        if (limit == null || limit <= 0) {
            throw new IllegalArgumentException("Giới hạn số phần tử lấy lên không hợp lệ");
        }
        ViewSchoolDirectoryCursorPageQuery query = new ViewSchoolDirectoryCursorPageQuery(cursor, limit);
        return viewSchoolDirectoryCursorPageUseCase.execute(query);
    }

    @QueryMapping(name = "schoolDirectoryPage")
    public PageResult<SchoolDirectoryDto> schoolDirectoryPage(@Argument(name = "page") Integer page, @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        ViewSchoolDirectoryPageQuery query = new ViewSchoolDirectoryPageQuery(page, size);
        return viewSchoolDirectoryPageUseCase.execute(query);
    }

    @QueryMapping(name = "schoolDirectory")
    public SchoolDirectoryDto schoolDirectory(@Argument(name = "id") UUID id) {
        ViewSchoolDirectoryDetailsQuery query = new ViewSchoolDirectoryDetailsQuery(id);
        return viewSchoolDirectoryDetailsUseCase.execute(query);
    }

    @QueryMapping(name = "schoolsWithOngoingAssessment")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public List<UUID> schoolsWithOngoingAssessment() {
        return listSchoolsWithOnGoingAssessmentUseCase.execute(null);
    }
}