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

import com.sep.vox.application.port.input.query.ViewMySchoolClassesQuery;
import com.sep.vox.application.port.input.query.ViewSchoolClassDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolClassUsersQuery;
import com.sep.vox.application.port.input.query.ViewSchoolClassesByUserQuery;
import com.sep.vox.application.port.input.query.ViewSchoolClassesQuery;
import com.sep.vox.application.port.input.query.ViewSchoolDirectoryCursorPageQuery;
import com.sep.vox.application.port.input.query.ViewSchoolDirectoryDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolDirectoryPageQuery;
import com.sep.vox.application.port.input.query.ViewSchoolGradeDetailsQuery;
import com.sep.vox.application.port.input.query.ViewGradeLevelDetailsQuery;
import com.sep.vox.application.port.input.query.ViewGradeLevelsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolGradesQuery;
import com.sep.vox.application.port.input.query.ViewSchoolRoomDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolRoomsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolStudentsBySchoolQuery;
import com.sep.vox.application.port.input.query.ViewSchoolTeachersBySchoolQuery;
import com.sep.vox.application.port.input.query.ViewSchoolUserDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolUsersBySchoolQuery;
import com.sep.vox.application.port.input.query.ViewSchoolsQuery;
import com.sep.vox.application.port.input.query.key.SchoolClassesKey;
import com.sep.vox.application.port.input.query.key.SchoolUsersKey;
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
import com.sep.vox.interfaces.graphql.dto.request.UpdateGradeLevelRequest;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolGradeRequest;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolRequest;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolRoomRequest;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolClassCommandMapper;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolCommandMapper;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolGradeCommandMapper;
import com.sep.vox.interfaces.graphql.mapper.UpdateGradeLevelCommandMapper;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolRoomCommandMapper;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolUserCommandMapper;

import graphql.schema.DataFetchingEnvironment;

@Controller("graphqlSchoolController")
public class SchoolController {

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

    public SchoolController(ViewSchoolsUseCase viewSchoolsUseCase,
                            ViewSchoolClassesUseCase viewSchoolClassesUseCase,
                            ViewSchoolClassesByUserUseCase viewSchoolClassesByUserUseCase,
                            ViewMySchoolClassesUseCase viewMySchoolClassesUseCase,
                            ViewSchoolClassDetailsUseCase viewSchoolClassDetailsUseCase,
                            ViewSchoolClassUsersUseCase viewSchoolClassUsersUseCase,
                            UpdateSchoolClassUseCase updateSchoolClassUseCase,
                            ViewSchoolUsersBySchoolUseCase viewSchoolUsersBySchoolUseCase,
                            ViewSchoolUsersForRequesterUseCase viewSchoolUsersForRequesterUseCase,
                            ViewSchoolStudentsBySchoolUseCase viewSchoolStudentsBySchoolUseCase,
                            ViewSchoolTeachersBySchoolUseCase viewSchoolTeachersBySchoolUseCase,
                            ViewSchoolUserDetailsUseCase viewSchoolUserDetailsUseCase,
                            UpdateSchoolUserUseCase updateSchoolUserUseCase,
                            UpdateSchoolUseCase updateSchoolUseCase,
                            ViewSchoolRoomDetailsUseCase viewSchoolRoomDetailsUseCase,
                            ViewSchoolRoomsUseCase viewSchoolRoomsUseCase,
                            UpdateSchoolRoomUseCase updateSchoolRoomUseCase,
                            UpdateSchoolGradeUseCase updateSchoolGradeUseCase,
                            ViewSchoolGradesUseCase viewSchoolGradesUseCase,
                            ViewSchoolGradeDetailsUseCase viewSchoolGradeDetailsUseCase,
                            ViewGradeLevelsUseCase viewGradeLevelsUseCase,
                            ViewGradeLevelDetailsUseCase viewGradeLevelDetailsUseCase,
                            UpdateGradeLevelUseCase updateGradeLevelUseCase,
                            ViewSchoolDirectoryCursorPageUseCase viewSchoolDirectoryCursorPageUseCase,
                            ViewSchoolDirectoryPageUseCase viewSchoolDirectoryPageUseCase,
                            ViewSchoolDirectoryDetailsUseCase viewSchoolDirectoryDetailsUseCase
    ) {
        this.viewSchoolsUseCase = viewSchoolsUseCase;
        this.viewSchoolClassesUseCase = viewSchoolClassesUseCase;
        this.viewSchoolClassesByUserUseCase = viewSchoolClassesByUserUseCase;
        this.viewMySchoolClassesUseCase = viewMySchoolClassesUseCase;
        this.viewSchoolClassDetailsUseCase = viewSchoolClassDetailsUseCase;
        this.viewSchoolClassUsersUseCase = viewSchoolClassUsersUseCase;
        this.updateSchoolClassUseCase = updateSchoolClassUseCase;
        this.viewSchoolUsersBySchoolUseCase = viewSchoolUsersBySchoolUseCase;
        this.viewSchoolUsersForRequesterUseCase = viewSchoolUsersForRequesterUseCase;
        this.viewSchoolStudentsBySchoolUseCase = viewSchoolStudentsBySchoolUseCase;
        this.viewSchoolTeachersBySchoolUseCase = viewSchoolTeachersBySchoolUseCase;
        this.viewSchoolUserDetailsUseCase = viewSchoolUserDetailsUseCase;
        this.updateSchoolUserUseCase = updateSchoolUserUseCase;
        this.updateSchoolUseCase = updateSchoolUseCase;
        this.viewSchoolRoomDetailsUseCase = viewSchoolRoomDetailsUseCase;
        this.viewSchoolRoomsUseCase = viewSchoolRoomsUseCase;
        this.updateSchoolRoomUseCase = updateSchoolRoomUseCase;
        this.updateSchoolGradeUseCase = updateSchoolGradeUseCase;
        this.viewSchoolGradesUseCase = viewSchoolGradesUseCase;
        this.viewSchoolGradeDetailsUseCase = viewSchoolGradeDetailsUseCase;
        this.viewGradeLevelsUseCase = viewGradeLevelsUseCase;
        this.viewGradeLevelDetailsUseCase = viewGradeLevelDetailsUseCase;
        this.updateGradeLevelUseCase = updateGradeLevelUseCase;
        this.viewSchoolDirectoryCursorPageUseCase = viewSchoolDirectoryCursorPageUseCase;
        this.viewSchoolDirectoryPageUseCase = viewSchoolDirectoryPageUseCase;
        this.viewSchoolDirectoryDetailsUseCase = viewSchoolDirectoryDetailsUseCase;
    }


    @QueryMapping(name = "schools")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<SchoolDto> schools(
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "search") String search,
            @Argument(name = "isActive") Boolean isActive) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích cỡ trang yêu cầu không hợp lệ");
        }
        var query = new ViewSchoolsQuery(page, size, search, isActive);
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


    @SchemaMapping(typeName = "School", field = "users")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public CompletableFuture<List<SchoolUserDto>> schoolUsers(SchoolDto school, @Argument(name = "page") Integer page, @Argument(name = "size") Integer size, DataFetchingEnvironment env) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích cỡ yêu cầu không hợp lệ");
        }
        DataLoader<SchoolUsersKey, List<SchoolUserDto>> loader = env.getDataLoader("schoolUsersBySchool");
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
            @Argument(name = "status") String status,
            @Argument(name = "excludeClassId") UUID excludeClassId) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
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
            @Argument(name = "status") String status) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
        return viewSchoolUsersForRequesterUseCase.execute(new ViewSchoolUsersBySchoolQuery(schoolId, page, size, search, roleId, status));
    }

    @QueryMapping(name = "schoolStudentsBySchool")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolUserDto> schoolStudentsBySchool(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "search") String search,
            @Argument(name = "status") String status) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
        return viewSchoolStudentsBySchoolUseCase.execute(new ViewSchoolStudentsBySchoolQuery(schoolId, page, size, search, status));
    }

    @QueryMapping(name = "schoolTeachersBySchool")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolUserDto> schoolTeachersBySchool(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "search") String search,
            @Argument(name = "status") String status) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
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
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích cỡ trang yêu cầu không hợp lệ");
        }
        return viewSchoolClassUsersUseCase.execute(new ViewSchoolClassUsersQuery(schoolClassId, page, size));
    }

    @QueryMapping(name = "schoolClassesByUser")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolClassDto> schoolClassesByUser(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "userId") UUID userId,
            @Argument(name = "status") String status,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích cỡ trang yêu cầu không hợp lệ");
        }
        return viewSchoolClassesByUserUseCase.execute(new ViewSchoolClassesByUserQuery(schoolId, userId, status, page, size));
    }

    @QueryMapping(name = "mySchoolClasses")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<SchoolClassDto> mySchoolClasses(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "status") String status,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích cỡ trang yêu cầu không hợp lệ");
        }
        return viewMySchoolClassesUseCase.execute(new ViewMySchoolClassesQuery(schoolId, status, page, size));
    }

    @SchemaMapping(typeName = "SchoolClassUser", field = "user")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public CompletableFuture<UserDto> user(SchoolClassUserDto schoolClassUser, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userBySchoolClassUser");
        return loader.load(schoolClassUser.userId());
    }

    @SchemaMapping(typeName = "SchoolUser", field = "user")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public CompletableFuture<UserDto> schoolUserUser(SchoolUserDto schoolUser, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userBySchoolUser");
        return loader.load(schoolUser.userId());
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

    // Nhập Id của schoolId. Mở cho cả TEACHER vì giáo viên cần chọn phòng khi tổ chức bài kiểm tra
    // trên lớp; ViewSchoolRoomsUseCase tự chặn người gọi không thuộc trường được yêu cầu.
    @QueryMapping(name = "schoolRooms")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<SchoolRoomFromDto> schoolRooms(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        int pageNumber = (page != null && page > 0) ? page : 1;
        int pageSize = (size != null && size > 0) ? size : 10;

        var query = new ViewSchoolRoomsQuery(schoolId, pageNumber, pageSize);
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
    public SchoolGradeDto schoolGrade(@Argument(name = "schoolId") UUID schoolId, @Argument(name = "id") UUID id) {
        var query = new ViewSchoolGradeDetailsQuery(schoolId, id);
        return viewSchoolGradeDetailsUseCase.execute(query);
    }

    @QueryMapping(name = "schoolGrades")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<SchoolGradeDto> schoolGrades(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "gradeLevelId") UUID gradeLevelId,
            @Argument(name = "status") String status,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {

        int pageNumber = (page != null && page > 0) ? page : 1;
        int pageSize = (size != null && size > 0) ? size : 10;

        var query = new ViewSchoolGradesQuery(schoolId, gradeLevelId, status, pageNumber, pageSize);

        return viewSchoolGradesUseCase.execute(query);
    }

    //======================== GRADE LEVEL (catalog toàn cục) =======================
    // Đọc: mọi vai trò đã đăng nhập. Ghi: chỉ SYSTEM_ADMIN -- một bản ghi ảnh hưởng mọi trường.
    @QueryMapping(name = "gradeLevels")
    public PageResult<GradeLevelDto> gradeLevels(
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "search") String search,
            @Argument(name = "status") String status) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích cỡ trang yêu cầu không hợp lệ");
        }
        var query = new ViewGradeLevelsQuery(page, size, search, status);
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
        var command = UpdateGradeLevelCommandMapper.fromRequest(gradeLevelId, request);
        return updateGradeLevelUseCase.execute(command);
    }

    @QueryMapping(name = "schoolDirectoryCursorPage")
    public CursorPage<SchoolDirectoryDto> schoolDirectoryCursorPage(@Argument(name = "cursor") UUID cursor, @Argument(name = "limit") Integer limit) {
        if (limit == null || limit <= 0) {
            throw new IllegalArgumentException("Giới hạn số phần tử lấy lên không hợp lệ");
        }
        var query = new ViewSchoolDirectoryCursorPageQuery(cursor, limit);
        return viewSchoolDirectoryCursorPageUseCase.execute(query);
    }

    @QueryMapping(name = "schoolDirectoryPage")
    public PageResult<SchoolDirectoryDto> schoolDirectoryPage(@Argument(name = "page") Integer page, @Argument(name = "size") Integer size) {
        if (page == null || size == null || page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích thước yêu cầu không hợp lệ");
        }
        var query = new ViewSchoolDirectoryPageQuery(page, size);
        return viewSchoolDirectoryPageUseCase.execute(query);
    }

    @QueryMapping(name = "schoolDirectory")
    public SchoolDirectoryDto schoolDirectory(@Argument(name = "id") UUID id) {
        var query = new ViewSchoolDirectoryDetailsQuery(id);
        return viewSchoolDirectoryDetailsUseCase.execute(query);
    }
}