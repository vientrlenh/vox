package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.port.input.command.DeleteSchoolCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolRoomCommand;
import com.sep.vox.application.port.input.command.UpdateSchoolStatusCommand;
import com.sep.vox.application.port.input.usecase.school.DeleteSchoolUseCase;
import com.sep.vox.application.port.input.usecase.school.UpdateSchoolStatusUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.AddSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.DeleteSchoolRoomUseCase;
import com.sep.vox.application.response.SchoolResponse.SchoolResponse;
import com.sep.vox.application.response.SchoolRoomResponse.SchoolRoomResponse;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.DeleteSchoolCommandMapper;
import com.sep.vox.interfaces.rest.mapper.DeleteSchoolRoomCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateSchoolStatusCommandMapper;
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
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mockStatic;

class SchoolControllerTests {

    @Mock
    private DeleteSchoolUseCase deleteSchoolUseCase;
    @Mock
    private UpdateSchoolStatusUseCase updateSchoolStatusUseCase;
    @Mock
    private AddSchoolRoomUseCase addSchoolRoomUseCase;
    @Mock
    private DeleteSchoolRoomUseCase deleteSchoolRoomUseCase;

    private SchoolController controller;

    private MockedStatic<DeleteSchoolCommandMapper> deleteMapperMock;
    private MockedStatic<UpdateSchoolStatusCommandMapper> statusMapperMock;
    private MockedStatic<DeleteSchoolRoomCommandMapper> deleteRoomMapperMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new SchoolController(deleteSchoolUseCase, updateSchoolStatusUseCase, addSchoolRoomUseCase, deleteSchoolRoomUseCase);

        deleteMapperMock = mockStatic(DeleteSchoolCommandMapper.class);
        statusMapperMock = mockStatic(UpdateSchoolStatusCommandMapper.class);
        deleteRoomMapperMock = mockStatic(DeleteSchoolRoomCommandMapper.class);
    }

    @AfterEach
    void tearDown() {
        deleteMapperMock.close();
        statusMapperMock.close();
        deleteRoomMapperMock.close();
    }

    @Test
    void deleteSchool_should_return_ok_response() {
        // Arrange
        var schoolId = UUID.randomUUID();
        var command = new DeleteSchoolCommand(schoolId);
        var useCaseResponse = new SchoolResponse(schoolId, "CODE", "School Name", null, "123", "a@a.com", "domain", "address", 100, false, null, null, null, null);

        deleteMapperMock.when(() -> DeleteSchoolCommandMapper.fromRequest(schoolId)).thenReturn(command);
        when(deleteSchoolUseCase.execute(command)).thenReturn(useCaseResponse);

        // Act
        ResponseEntity<ApiResponse<SchoolResponse>> response = controller.deleteSchool(schoolId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(useCaseResponse);
        assertThat(response.getBody().message()).isEqualTo("Xóa trường học thành công");
        verify(deleteSchoolUseCase).execute(command);
    }

    @Test
    void updateSchoolStatus_to_inactive_should_return_ok_response() {
        // Arrange
        var schoolId = UUID.randomUUID();
        boolean isActive = false;
        var command = new UpdateSchoolStatusCommand(schoolId, isActive);
        var useCaseResponse = new SchoolResponse(schoolId, "CODE", "School Name", null, "123", "a@a.com", "domain", "address", 100, isActive, null, null, null, null);

        statusMapperMock.when(() -> UpdateSchoolStatusCommandMapper.fromRequest(schoolId, isActive)).thenReturn(command);
        when(updateSchoolStatusUseCase.execute(command)).thenReturn(useCaseResponse);

        // Act
        ResponseEntity<ApiResponse<SchoolResponse>> response = controller.updateSchoolStatus(schoolId, isActive);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(useCaseResponse);
        assertThat(response.getBody().message()).isEqualTo("Đã vô hiệu hóa trường học thành công");
        verify(updateSchoolStatusUseCase).execute(command);
    }

    @Test
    void updateSchoolStatus_to_active_should_return_ok_response() {
        // Arrange
        var schoolId = UUID.randomUUID();
        boolean isActive = true;
        var command = new UpdateSchoolStatusCommand(schoolId, isActive);
        var useCaseResponse = new SchoolResponse(schoolId, "CODE", "School Name", null, "123", "a@a.com", "domain", "address", 100, isActive, null, null, null, null);

        statusMapperMock.when(() -> UpdateSchoolStatusCommandMapper.fromRequest(schoolId, isActive)).thenReturn(command);
        when(updateSchoolStatusUseCase.execute(command)).thenReturn(useCaseResponse);

        // Act
        ResponseEntity<ApiResponse<SchoolResponse>> response = controller.updateSchoolStatus(schoolId, isActive);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(useCaseResponse);
        assertThat(response.getBody().message()).isEqualTo("Đã kích hoạt lại trường học thành công");
        verify(updateSchoolStatusUseCase).execute(command);
    }

    @Test
    void deleteSchoolRoom_should_return_ok_response() {
        // Arrange
        var roomId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var command = new DeleteSchoolRoomCommand(roomId);
        var useCaseResponse = new SchoolRoomResponse(roomId, schoolId, "ROOM01", "Room Name", "Description", false, null, null, null, null);

        deleteRoomMapperMock.when(() -> DeleteSchoolRoomCommandMapper.fromRequest(roomId)).thenReturn(command);
        when(deleteSchoolRoomUseCase.execute(command)).thenReturn(useCaseResponse);

        // Act
        ResponseEntity<ApiResponse<SchoolRoomResponse>> response = controller.deleteSchoolRoom(roomId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(useCaseResponse);
        assertThat(response.getBody().message()).isEqualTo("Xóa thành công school room");
        verify(deleteSchoolRoomUseCase).execute(command);
    }
}
