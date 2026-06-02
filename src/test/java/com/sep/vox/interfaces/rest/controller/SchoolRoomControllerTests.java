package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.sep.vox.application.port.input.command.AddSchoolRoomCommand;
import com.sep.vox.application.port.input.command.UpdateSchoolRoomCommand;
import com.sep.vox.application.port.input.usecase.schoolroom.AddSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.UpdateSchoolRoomUseCase;
import com.sep.vox.application.response.SchoolRoomResponse.SchoolRoomResponse;
import com.sep.vox.interfaces.rest.dto.request.AddSchoolRoomRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateSchoolRoomRequest;

public class SchoolRoomControllerTests {

    @Test
    void addSchoolRoom_should_return_ok_response() {
        // Arrange
        var addSchoolRoomUseCase = mock(AddSchoolRoomUseCase.class);
        var updateSchoolRoomUseCase = mock(UpdateSchoolRoomUseCase.class);
        var controller = new SchoolRoomController(addSchoolRoomUseCase, updateSchoolRoomUseCase);

        var schoolId = UUID.randomUUID();
        var request = new AddSchoolRoomRequest(
            schoolId,
            "ROOM-001",
            "Phòng thi số 1",
            "Phòng thi dùng cho kỳ thi giữa kì"
        );

        var expectedCommand = new AddSchoolRoomCommand(
            request.schoolId(),
            request.code(),
            request.name(),
            request.description()
        );

        var roomId = UUID.randomUUID();
        var expectedResponse = new SchoolRoomResponse(
            roomId,
            schoolId,
            "ROOM-001",
            "Phòng thi số 1",
            "Phòng thi dùng cho kỳ thi giữa kì",
            false
        );

        when(addSchoolRoomUseCase.execute(expectedCommand))
            .thenReturn(expectedResponse);

        // Act
        var response = controller.addSchoolRoom(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expectedResponse);
        assertThat(response.getBody().data().id()).isEqualTo(roomId);
        assertThat(response.getBody().data().code()).isEqualTo("ROOM-001");
        assertThat(response.getBody().data().name()).isEqualTo("Phòng thi số 1");
        assertThat(response.getBody().data().isActive()).isFalse();
        verify(addSchoolRoomUseCase).execute(expectedCommand);
    }

    @Test
    void addSchoolRoom_should_map_request_to_command_correctly() {
        // Arrange
        var addSchoolRoomUseCase = mock(AddSchoolRoomUseCase.class);
        var updateSchoolRoomUseCase = mock(UpdateSchoolRoomUseCase.class);
        var controller = new SchoolRoomController(addSchoolRoomUseCase, updateSchoolRoomUseCase);

        var schoolId = UUID.randomUUID();
        var request = new AddSchoolRoomRequest(
            schoolId,
            "ROOM-002",
            "Phòng thi số 2",
            "Mô tả phòng thi"
        );

        var roomId = UUID.randomUUID();
        var responseData = new SchoolRoomResponse(
            roomId,
            schoolId,
            "ROOM-002",
            "Phòng thi số 2",
            "Mô tả phòng thi",
            false
        );

        when(addSchoolRoomUseCase.execute(
            new AddSchoolRoomCommand(schoolId, "ROOM-002", "Phòng thi số 2", "Mô tả phòng thi")
        )).thenReturn(responseData);

        // Act
        controller.addSchoolRoom(request);

        // Assert
        verify(addSchoolRoomUseCase).execute(
            new AddSchoolRoomCommand(schoolId, "ROOM-002", "Phòng thi số 2", "Mô tả phòng thi")
        );
    }

    @Test
    void addSchoolRoom_with_null_description_should_work() {
        // Arrange
        var addSchoolRoomUseCase = mock(AddSchoolRoomUseCase.class);
        var updateSchoolRoomUseCase = mock(UpdateSchoolRoomUseCase.class);
        var controller = new SchoolRoomController(addSchoolRoomUseCase, updateSchoolRoomUseCase);

        var schoolId = UUID.randomUUID();
        var request = new AddSchoolRoomRequest(
            schoolId,
            "ROOM-003",
            "Phòng thi số 3",
            null
        );

        var roomId = UUID.randomUUID();
        var responseData = new SchoolRoomResponse(
            roomId,
            schoolId,
            "ROOM-003",
            "Phòng thi số 3",
            null,
            false
        );

        when(addSchoolRoomUseCase.execute(
            new AddSchoolRoomCommand(schoolId, "ROOM-003", "Phòng thi số 3", null)
        )).thenReturn(responseData);

        // Act
        var response = controller.addSchoolRoom(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data().description()).isNull();
    }

    @Test
    void updateSchoolRoom_should_return_ok_response() {
        // Arrange
        var addSchoolRoomUseCase = mock(AddSchoolRoomUseCase.class);
        var updateSchoolRoomUseCase = mock(UpdateSchoolRoomUseCase.class);
        var controller = new SchoolRoomController(addSchoolRoomUseCase, updateSchoolRoomUseCase);

        var roomId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var request = new UpdateSchoolRoomRequest(
            schoolId,
            "ROOM-001-UPDATED",
            "Phòng thi số 1 - Cập nhật",
            "Mô tả cập nhật",
            true
        );

        var expectedCommand = new UpdateSchoolRoomCommand(
            roomId,
            request.schoolId(),
            request.code(),
            request.name(),
            request.description(),
            request.isActive()
        );

        var expectedResponse = new SchoolRoomResponse(
            roomId,
            schoolId,
            "ROOM-001-UPDATED",
            "Phòng thi số 1 - Cập nhật",
            "Mô tả cập nhật",
            true
        );

        when(updateSchoolRoomUseCase.execute(expectedCommand))
            .thenReturn(expectedResponse);

        // Act
        var response = controller.updateSchoolRoom(roomId, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expectedResponse);
        assertThat(response.getBody().data().id()).isEqualTo(roomId);
        assertThat(response.getBody().data().code()).isEqualTo("ROOM-001-UPDATED");
        assertThat(response.getBody().data().name()).isEqualTo("Phòng thi số 1 - Cập nhật");
        assertThat(response.getBody().data().isActive()).isTrue();
        verify(updateSchoolRoomUseCase).execute(expectedCommand);
    }

    @Test
    void updateSchoolRoom_should_map_request_and_id_to_command_correctly() {
        // Arrange
        var addSchoolRoomUseCase = mock(AddSchoolRoomUseCase.class);
        var updateSchoolRoomUseCase = mock(UpdateSchoolRoomUseCase.class);
        var controller = new SchoolRoomController(addSchoolRoomUseCase, updateSchoolRoomUseCase);

        var roomId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var request = new UpdateSchoolRoomRequest(
            schoolId,
            "ROOM-002-NEW",
            "Phòng thi mới",
            "Cập nhật mô tả",
            true
        );

        var responseData = new SchoolRoomResponse(
            roomId,
            schoolId,
            "ROOM-002-NEW",
            "Phòng thi mới",
            "Cập nhật mô tả",
            true
        );

        when(updateSchoolRoomUseCase.execute(
            new UpdateSchoolRoomCommand(
                roomId,
                schoolId,
                "ROOM-002-NEW",
                "Phòng thi mới",
                "Cập nhật mô tả",
                true
            )
        )).thenReturn(responseData);

        // Act
        controller.updateSchoolRoom(roomId, request);

        // Assert
        verify(updateSchoolRoomUseCase).execute(
            new UpdateSchoolRoomCommand(
                roomId,
                schoolId,
                "ROOM-002-NEW",
                "Phòng thi mới",
                "Cập nhật mô tả",
                true
            )
        );
    }

    @Test
    void updateSchoolRoom_should_set_isActive_to_false() {
        // Arrange
        var addSchoolRoomUseCase = mock(AddSchoolRoomUseCase.class);
        var updateSchoolRoomUseCase = mock(UpdateSchoolRoomUseCase.class);
        var controller = new SchoolRoomController(addSchoolRoomUseCase, updateSchoolRoomUseCase);

        var roomId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var request = new UpdateSchoolRoomRequest(
            schoolId,
            "ROOM-003",
            "Phòng thi số 3",
            "Mô tả",
            false
        );

        var responseData = new SchoolRoomResponse(
            roomId,
            schoolId,
            "ROOM-003",
            "Phòng thi số 3",
            "Mô tả",
            false
        );

        when(updateSchoolRoomUseCase.execute(
            new UpdateSchoolRoomCommand(
                roomId,
                schoolId,
                "ROOM-003",
                "Phòng thi số 3",
                "Mô tả",
                false
            )
        )).thenReturn(responseData);

        // Act
        var response = controller.updateSchoolRoom(roomId, request);

        // Assert
        assertThat(response.getBody().data().isActive()).isFalse();
    }

    @Test
    void updateSchoolRoom_with_null_description_should_work() {
        // Arrange
        var addSchoolRoomUseCase = mock(AddSchoolRoomUseCase.class);
        var updateSchoolRoomUseCase = mock(UpdateSchoolRoomUseCase.class);
        var controller = new SchoolRoomController(addSchoolRoomUseCase, updateSchoolRoomUseCase);

        var roomId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var request = new UpdateSchoolRoomRequest(
            schoolId,
            "ROOM-004",
            "Phòng thi số 4",
            null,
            true
        );

        var responseData = new SchoolRoomResponse(
            roomId,
            schoolId,
            "ROOM-004",
            "Phòng thi số 4",
            null,
            true
        );

        when(updateSchoolRoomUseCase.execute(
            new UpdateSchoolRoomCommand(
                roomId,
                schoolId,
                "ROOM-004",
                "Phòng thi số 4",
                null,
                true
            )
        )).thenReturn(responseData);

        // Act
        var response = controller.updateSchoolRoom(roomId, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data().description()).isNull();
    }

    @Test
    void addSchoolRoom_response_message_should_be_success() {
        // Arrange
        var addSchoolRoomUseCase = mock(AddSchoolRoomUseCase.class);
        var updateSchoolRoomUseCase = mock(UpdateSchoolRoomUseCase.class);
        var controller = new SchoolRoomController(addSchoolRoomUseCase, updateSchoolRoomUseCase);

        var schoolId = UUID.randomUUID();
        var request = new AddSchoolRoomRequest(
            schoolId,
            "ROOM-005",
            "Phòng thi số 5",
            "Mô tả"
        );

        var roomId = UUID.randomUUID();
        var responseData = new SchoolRoomResponse(
            roomId,
            schoolId,
            "ROOM-005",
            "Phòng thi số 5",
            "Mô tả",
            false
        );

        when(addSchoolRoomUseCase.execute(
            new AddSchoolRoomCommand(schoolId, "ROOM-005", "Phòng thi số 5", "Mô tả")
        )).thenReturn(responseData);

        // Act
        var response = controller.addSchoolRoom(request);

        // Assert
        assertThat(response.getBody().message()).isEqualTo("Thêm phòng học thành công");
    }

    @Test
    void updateSchoolRoom_response_message_should_be_success() {
        // Arrange
        var addSchoolRoomUseCase = mock(AddSchoolRoomUseCase.class);
        var updateSchoolRoomUseCase = mock(UpdateSchoolRoomUseCase.class);
        var controller = new SchoolRoomController(addSchoolRoomUseCase, updateSchoolRoomUseCase);

        var roomId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var request = new UpdateSchoolRoomRequest(
            schoolId,
            "ROOM-006",
            "Phòng thi số 6",
            "Mô tả",
            true
        );

        var responseData = new SchoolRoomResponse(
            roomId,
            schoolId,
            "ROOM-006",
            "Phòng thi số 6",
            "Mô tả",
            true
        );

        when(updateSchoolRoomUseCase.execute(
            new UpdateSchoolRoomCommand(roomId, schoolId, "ROOM-006", "Phòng thi số 6", "Mô tả", true)
        )).thenReturn(responseData);

        // Act
        var response = controller.updateSchoolRoom(roomId, request);

        // Assert
        assertThat(response.getBody().message()).isEqualTo("Cập nhật phòng học thành công");
    }

    @Test
    void addSchoolRoom_different_school_ids() {
        // Arrange
        var addSchoolRoomUseCase = mock(AddSchoolRoomUseCase.class);
        var updateSchoolRoomUseCase = mock(UpdateSchoolRoomUseCase.class);
        var controller = new SchoolRoomController(addSchoolRoomUseCase, updateSchoolRoomUseCase);

        var schoolId1 = UUID.randomUUID();
        var schoolId2 = UUID.randomUUID();

        var request1 = new AddSchoolRoomRequest(schoolId1, "ROOM-A", "Phòng A", null);
        var request2 = new AddSchoolRoomRequest(schoolId2, "ROOM-B", "Phòng B", null);

        var roomId1 = UUID.randomUUID();
        var roomId2 = UUID.randomUUID();

        var response1 = new SchoolRoomResponse(roomId1, schoolId1, "ROOM-A", "Phòng A", null, false);
        var response2 = new SchoolRoomResponse(roomId2, schoolId2, "ROOM-B", "Phòng B", null, false);

        when(addSchoolRoomUseCase.execute(
            new AddSchoolRoomCommand(schoolId1, "ROOM-A", "Phòng A", null)
        )).thenReturn(response1);

        when(addSchoolRoomUseCase.execute(
            new AddSchoolRoomCommand(schoolId2, "ROOM-B", "Phòng B", null)
        )).thenReturn(response2);

        // Act
        var result1 = controller.addSchoolRoom(request1);
        var result2 = controller.addSchoolRoom(request2);

        // Assert
        assertThat(result1.getBody().data().schoolId()).isEqualTo(schoolId1);
        assertThat(result2.getBody().data().schoolId()).isEqualTo(schoolId2);
        assertThat(result1.getBody().data().id()).isNotEqualTo(result2.getBody().data().id());
    }

    @Test
    void updateSchoolRoom_different_ids_should_call_with_correct_id() {
        // Arrange
        var addSchoolRoomUseCase = mock(AddSchoolRoomUseCase.class);
        var updateSchoolRoomUseCase = mock(UpdateSchoolRoomUseCase.class);
        var controller = new SchoolRoomController(addSchoolRoomUseCase, updateSchoolRoomUseCase);

        var roomId1 = UUID.randomUUID();
        var roomId2 = UUID.randomUUID();
        var schoolId = UUID.randomUUID();

        var request = new UpdateSchoolRoomRequest(
            schoolId,
            "ROOM-UPDATE",
            "Cập nhật",
            "Mô tả",
            true
        );

        var response1 = new SchoolRoomResponse(roomId1, schoolId, "ROOM-UPDATE", "Cập nhật", "Mô tả", true);
        var response2 = new SchoolRoomResponse(roomId2, schoolId, "ROOM-UPDATE", "Cập nhật", "Mô tả", true);

        when(updateSchoolRoomUseCase.execute(
            new UpdateSchoolRoomCommand(roomId1, schoolId, "ROOM-UPDATE", "Cập nhật", "Mô tả", true)
        )).thenReturn(response1);

        when(updateSchoolRoomUseCase.execute(
            new UpdateSchoolRoomCommand(roomId2, schoolId, "ROOM-UPDATE", "Cập nhật", "Mô tả", true)
        )).thenReturn(response2);

        // Act
        var result1 = controller.updateSchoolRoom(roomId1, request);
        var result2 = controller.updateSchoolRoom(roomId2, request);

        // Assert
        assertThat(result1.getBody().data().id()).isEqualTo(roomId1);
        assertThat(result2.getBody().data().id()).isEqualTo(roomId2);
        verify(updateSchoolRoomUseCase).execute(
            new UpdateSchoolRoomCommand(roomId1, schoolId, "ROOM-UPDATE", "Cập nhật", "Mô tả", true)
        );
        verify(updateSchoolRoomUseCase).execute(
            new UpdateSchoolRoomCommand(roomId2, schoolId, "ROOM-UPDATE", "Cập nhật", "Mô tả", true)
        );
    }
}
