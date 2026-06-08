package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.port.input.command.*;
import com.sep.vox.application.port.input.usecase.school.DeleteSchoolUseCase;
import com.sep.vox.application.port.input.usecase.school.UpdateSchoolStatusUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.CreateSchoolGradeUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.DeleteSchoolGradeUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.AddSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.DeleteSchoolRoomUseCase;
import com.sep.vox.application.response.SchoolGradeResponse.SchoolGradeResponse;
import com.sep.vox.application.response.SchoolResponse.SchoolResponse;
import com.sep.vox.application.response.SchoolRoomResponse.SchoolRoomResponse;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.interfaces.rest.dto.request.AddSchoolRoomRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolGradeRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchoolControllerTests {

    //<editor-fold desc="Mocks">
    @Mock
    private DeleteSchoolUseCase deleteSchoolUseCase;
    @Mock
    private UpdateSchoolStatusUseCase updateSchoolStatusUseCase;
    @Mock
    private AddSchoolRoomUseCase addSchoolRoomUseCase;
    @Mock
    private DeleteSchoolRoomUseCase deleteSchoolRoomUseCase;
    @Mock
    private CreateSchoolGradeUseCase createSchoolGradeUseCase;
    @Mock
    private DeleteSchoolGradeUseCase deleteSchoolGradeUseCase;
    //</editor-fold>

    private SchoolController controller;

    //<editor-fold desc="Mapper Mocks">
    private MockedStatic<DeleteSchoolCommandMapper> deleteSchoolMapperMock;
    private MockedStatic<UpdateSchoolStatusCommandMapper> statusMapperMock;
    private MockedStatic<AddSchoolRoomCommandMapper> addRoomMapperMock;
    private MockedStatic<DeleteSchoolRoomCommandMapper> deleteRoomMapperMock;
    private MockedStatic<CreateSchoolGradeCommandMapper> createGradeMapperMock;
    private MockedStatic<DeleteSchoolGradeCommandMapper> deleteGradeMapperMock;
    //</editor-fold>

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new SchoolController(
                deleteSchoolUseCase, updateSchoolStatusUseCase,
                addSchoolRoomUseCase, deleteSchoolRoomUseCase,
                createSchoolGradeUseCase, deleteSchoolGradeUseCase
        );

        // Initialize static mapper mocks
        deleteSchoolMapperMock = mockStatic(DeleteSchoolCommandMapper.class);
        statusMapperMock = mockStatic(UpdateSchoolStatusCommandMapper.class);
        addRoomMapperMock = mockStatic(AddSchoolRoomCommandMapper.class);
        deleteRoomMapperMock = mockStatic(DeleteSchoolRoomCommandMapper.class);
        createGradeMapperMock = mockStatic(CreateSchoolGradeCommandMapper.class);
        deleteGradeMapperMock = mockStatic(DeleteSchoolGradeCommandMapper.class);
    }

    @AfterEach
    void tearDown() {
        // Close all mocks
        deleteSchoolMapperMock.close();
        statusMapperMock.close();
        addRoomMapperMock.close();
        deleteRoomMapperMock.close();
        createGradeMapperMock.close();
        deleteGradeMapperMock.close();
    }

    @Test
    void deleteSchool_should_return_ok_response() {
        // Arrange
        var schoolId = UUID.randomUUID();
        var command = new DeleteSchoolCommand(schoolId);

        deleteSchoolMapperMock.when(() -> DeleteSchoolCommandMapper.fromRequest(schoolId)).thenReturn(command);
        when(deleteSchoolUseCase.execute(command)).thenReturn(schoolId);

        // Act
        ResponseEntity<ApiResponse<UUID>> response = controller.deleteSchool(schoolId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().message()).isEqualTo("Xóa trường học thành công");
        // SỬA Ở ĐÂY: So sánh UUID với UUID, không so sánh với SchoolResponse
        assertThat(response.getBody().data()).isEqualTo(schoolId);
        verify(deleteSchoolUseCase).execute(command);
    }

    @Test
    void updateSchoolStatus_to_active_should_return_ok() {
        // Arrange
        var schoolId = UUID.randomUUID();
        var command = new UpdateSchoolStatusCommand(schoolId, true);
        var useCaseResponse = new SchoolResponse(schoolId, "CODE", "School", null, "123", "a@a.com", "domain", "address", 100, true, null, null, null, null);

        statusMapperMock.when(() -> UpdateSchoolStatusCommandMapper.fromRequest(schoolId, true)).thenReturn(command);
        when(updateSchoolStatusUseCase.execute(command)).thenReturn(useCaseResponse);

        // Act
        ResponseEntity<ApiResponse<SchoolResponse>> response = controller.updateSchoolStatus(schoolId, true);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().message()).isEqualTo("Đã kích hoạt lại trường học thành công");
        assertThat(response.getBody().data()).isEqualTo(useCaseResponse);
    }

    @Test
    void addSchoolRoom_should_return_ok_response() {
        // Arrange
        var schoolId = UUID.randomUUID();
        var request = new AddSchoolRoomRequest("VIT", "ROOM-01", "Desc");
        var command = new AddSchoolRoomCommand(schoolId, "ROOM-01", "Room 1", "Desc");
        var useCaseResponse = new SchoolRoomResponse(UUID.randomUUID(), schoolId, "ROOM-01", "Room 1", "Desc", false, null, null, null, null);

        addRoomMapperMock.when(() -> AddSchoolRoomCommandMapper.fromRequest(schoolId, request)).thenReturn(command);
        when(addSchoolRoomUseCase.execute(command)).thenReturn(useCaseResponse);

        // Act
        ResponseEntity<ApiResponse<SchoolRoomResponse>> response = controller.addSchoolRoom(schoolId, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().message()).isEqualTo("Thêm phòng học thành công");
        assertThat(response.getBody().data()).isEqualTo(useCaseResponse);
    }

    @Test
    void deleteSchoolRoom_should_return_ok_response() {
        // Arrange
        var roomId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var command = new DeleteSchoolRoomCommand(roomId,schoolId);
        var useCaseResponse = new SchoolRoomResponse(roomId, UUID.randomUUID(), "DELETED", "Deleted", null, false, null, null, null, null);

        deleteRoomMapperMock.when(() -> DeleteSchoolRoomCommandMapper.fromRequest(roomId, schoolId)).thenReturn(command);
        when(deleteSchoolRoomUseCase.execute(command)).thenReturn(useCaseResponse);

        // Act
        ResponseEntity<ApiResponse<SchoolRoomResponse>> response = controller.deleteSchoolRoom(roomId, schoolId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().message()).isEqualTo("Xóa thành công school room");
        assertThat(response.getBody().data()).isEqualTo(useCaseResponse);
    }

    @Test
    void createSchoolGrade_should_return_ok_with_new_id() {
        // Arrange
        var schoolId = UUID.randomUUID();
        var request = new CreateSchoolGradeRequest("G10", "Khối 10", null, null, null);
        var command = new CreateSchoolGradeCommand(schoolId, "G10", "Khối 10", null, null, null);
        var newGradeId = UUID.randomUUID();

        createGradeMapperMock.when(() -> CreateSchoolGradeCommandMapper.fromRequest(schoolId, request)).thenReturn(command);
        when(createSchoolGradeUseCase.execute(command)).thenReturn(newGradeId);

        // Act
        ResponseEntity<ApiResponse<UUID>> response = controller.createSchoolGrade(schoolId, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().message()).isEqualTo("Thêm thành công khối của trường");
        assertThat(response.getBody().data()).isEqualTo(newGradeId);
    }

    @Test
    void deleteSchoolGrade_should_return_ok_response() {
        // Arrange
        var gradeId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();

        // PHẢI ĐÚNG THỨ TỰ: (schoolId, gradeId)
        var command = new DeleteSchoolGradeCommand(schoolId, gradeId);

        var useCaseResponse = new SchoolGradeResponse(gradeId, schoolId, "DELETED", "Deleted", null, null, null, null, null, null, null, null);

        // Mapper mock phải khớp thứ tự
        deleteGradeMapperMock.when(() -> DeleteSchoolGradeCommandMapper.fromRequest(schoolId, gradeId))
                .thenReturn(command);

        when(deleteSchoolGradeUseCase.execute(command)).thenReturn(useCaseResponse);

        // Act
        // Controller cũng phải truyền schoolId trước, gradeId sau (dựa vào code controller của bạn)
        ResponseEntity<ApiResponse<SchoolGradeResponse>> response = controller.deleteSchoolGrade(schoolId, gradeId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(useCaseResponse);
    }
}