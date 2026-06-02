package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.ViewSchoolRoomDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolRoomsBySchoolIdQuery;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomsUseCase;
import com.sep.vox.application.response.SchoolRoomResponse.SchoolRoomResponse;
import com.sep.vox.domain.common.PageResult;

public class SchoolRoomControllerTests {

    @Test
    void getSchoolRoomById_should_return_school_room_response() {
        // Arrange
        var viewSchoolRoomDetailsUseCase = mock(ViewSchoolRoomDetailsUseCase.class);
        var viewSchoolRoomsUseCase = mock(ViewSchoolRoomsUseCase.class);
        var controller = new SchoolRoomController(viewSchoolRoomDetailsUseCase, viewSchoolRoomsUseCase);

        var roomId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();

        var expectedQuery = new ViewSchoolRoomDetailsQuery(roomId);
        var expectedResponse = new SchoolRoomResponse(
            roomId,
            schoolId,
            "ROOM-001",
            "Phòng thi số 1",
            "Phòng thi dùng cho kỳ thi giữa kì",
            true
        );

        when(viewSchoolRoomDetailsUseCase.execute(expectedQuery))
            .thenReturn(expectedResponse);

        // Act
        var response = controller.getSchoolRoomById(roomId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(roomId);
        assertThat(response.schoolId()).isEqualTo(schoolId);
        assertThat(response.code()).isEqualTo("ROOM-001");
        assertThat(response.name()).isEqualTo("Phòng thi số 1");
        assertThat(response.description()).isEqualTo("Phòng thi dùng cho kỳ thi giữa kì");
        assertThat(response.isActive()).isTrue();
        verify(viewSchoolRoomDetailsUseCase).execute(expectedQuery);
    }

    @Test
    void getSchoolRoomById_should_map_argument_to_query_correctly() {
        // Arrange
        var viewSchoolRoomDetailsUseCase = mock(ViewSchoolRoomDetailsUseCase.class);
        var viewSchoolRoomsUseCase = mock(ViewSchoolRoomsUseCase.class);
        var controller = new SchoolRoomController(viewSchoolRoomDetailsUseCase, viewSchoolRoomsUseCase);

        var roomId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();

        var expectedResponse = new SchoolRoomResponse(
            roomId,
            schoolId,
            "ROOM-002",
            "Phòng thi số 2",
            "Mô tả",
            false
        );

        when(viewSchoolRoomDetailsUseCase.execute(new ViewSchoolRoomDetailsQuery(roomId)))
            .thenReturn(expectedResponse);

        // Act
        controller.getSchoolRoomById(roomId);

        // Assert
        verify(viewSchoolRoomDetailsUseCase).execute(new ViewSchoolRoomDetailsQuery(roomId));
    }

    @Test
    void getSchoolRoomById_with_null_description() {
        // Arrange
        var viewSchoolRoomDetailsUseCase = mock(ViewSchoolRoomDetailsUseCase.class);
        var viewSchoolRoomsUseCase = mock(ViewSchoolRoomsUseCase.class);
        var controller = new SchoolRoomController(viewSchoolRoomDetailsUseCase, viewSchoolRoomsUseCase);

        var roomId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();

        var expectedResponse = new SchoolRoomResponse(
            roomId,
            schoolId,
            "ROOM-003",
            "Phòng thi số 3",
            null,
            true
        );

        when(viewSchoolRoomDetailsUseCase.execute(new ViewSchoolRoomDetailsQuery(roomId)))
            .thenReturn(expectedResponse);

        // Act
        var response = controller.getSchoolRoomById(roomId);

        // Assert
        assertThat(response.description()).isNull();
    }

    @Test
    void getSchoolRoomsBySchoolId_should_return_paginated_school_rooms() {
        // Arrange
        var viewSchoolRoomDetailsUseCase = mock(ViewSchoolRoomDetailsUseCase.class);
        var viewSchoolRoomsUseCase = mock(ViewSchoolRoomsUseCase.class);
        var controller = new SchoolRoomController(viewSchoolRoomDetailsUseCase, viewSchoolRoomsUseCase);

        var schoolId = UUID.randomUUID();
        var roomId1 = UUID.randomUUID();
        var roomId2 = UUID.randomUUID();

        var room1 = new SchoolRoomResponse(roomId1, schoolId, "ROOM-001", "Phòng 1", "Mô tả 1", true);
        var room2 = new SchoolRoomResponse(roomId2, schoolId, "ROOM-002", "Phòng 2", "Mô tả 2", false);

        var expectedQuery = new ViewSchoolRoomsBySchoolIdQuery(schoolId, 0, 10);
        List<SchoolRoomResponse> rooms = new ArrayList<>();
        rooms.add(room1);
        rooms.add(room2);
        var expectedPageResult = new PageResult<>(
            rooms,
            0,
            10,
            2L,
            1
        );

        when(viewSchoolRoomsUseCase.execute(expectedQuery))
            .thenReturn(expectedPageResult);

        // Act
        var response = controller.getSchoolRoomsBySchoolId(schoolId, 0, 10);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(2);
        assertThat(response.content()).containsExactly(room1, room2);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(2L);
        assertThat(response.totalPages()).isEqualTo(1);
        verify(viewSchoolRoomsUseCase).execute(expectedQuery);
    }

    @Test
    void getSchoolRoomsBySchoolId_should_use_default_page_when_null() {
        // Arrange
        var viewSchoolRoomDetailsUseCase = mock(ViewSchoolRoomDetailsUseCase.class);
        var viewSchoolRoomsUseCase = mock(ViewSchoolRoomsUseCase.class);
        var controller = new SchoolRoomController(viewSchoolRoomDetailsUseCase, viewSchoolRoomsUseCase);

        var schoolId = UUID.randomUUID();
        var expectedQuery = new ViewSchoolRoomsBySchoolIdQuery(schoolId, 0, 10);
        List<SchoolRoomResponse> emptyList = new ArrayList<>();
        var expectedPageResult = new PageResult<>(emptyList, 0, 10, 0L, 0);

        when(viewSchoolRoomsUseCase.execute(expectedQuery))
            .thenReturn(expectedPageResult);

        // Act
        controller.getSchoolRoomsBySchoolId(schoolId, null, null);

        // Assert
        verify(viewSchoolRoomsUseCase).execute(expectedQuery);
    }

    @Test
    void getSchoolRoomsBySchoolId_should_use_default_size_when_null() {
        // Arrange
        var viewSchoolRoomDetailsUseCase = mock(ViewSchoolRoomDetailsUseCase.class);
        var viewSchoolRoomsUseCase = mock(ViewSchoolRoomsUseCase.class);
        var controller = new SchoolRoomController(viewSchoolRoomDetailsUseCase, viewSchoolRoomsUseCase);

        var schoolId = UUID.randomUUID();
        var expectedQuery = new ViewSchoolRoomsBySchoolIdQuery(schoolId, 0, 10);
        List<SchoolRoomResponse> emptyList = new ArrayList<>();
        var expectedPageResult = new PageResult<>(emptyList, 0, 10, 0L, 0);

        when(viewSchoolRoomsUseCase.execute(expectedQuery))
            .thenReturn(expectedPageResult);

        // Act
        var response = controller.getSchoolRoomsBySchoolId(schoolId, 0, null);

        // Assert
        assertThat(response.size()).isEqualTo(10);
        verify(viewSchoolRoomsUseCase).execute(expectedQuery);
    }

    @Test
    void getSchoolRoomsBySchoolId_should_use_default_page_when_negative() {
        // Arrange
        var viewSchoolRoomDetailsUseCase = mock(ViewSchoolRoomDetailsUseCase.class);
        var viewSchoolRoomsUseCase = mock(ViewSchoolRoomsUseCase.class);
        var controller = new SchoolRoomController(viewSchoolRoomDetailsUseCase, viewSchoolRoomsUseCase);

        var schoolId = UUID.randomUUID();
        var expectedQuery = new ViewSchoolRoomsBySchoolIdQuery(schoolId, 0, 10);
        List<SchoolRoomResponse> emptyList = new ArrayList<>();
        var expectedPageResult = new PageResult<>(emptyList, 0, 10, 0L, 0);

        when(viewSchoolRoomsUseCase.execute(expectedQuery))
            .thenReturn(expectedPageResult);

        // Act
        controller.getSchoolRoomsBySchoolId(schoolId, -1, 10);

        // Assert
        verify(viewSchoolRoomsUseCase).execute(expectedQuery);
    }

    @Test
    void getSchoolRoomsBySchoolId_should_use_default_size_when_zero() {
        // Arrange
        var viewSchoolRoomDetailsUseCase = mock(ViewSchoolRoomDetailsUseCase.class);
        var viewSchoolRoomsUseCase = mock(ViewSchoolRoomsUseCase.class);
        var controller = new SchoolRoomController(viewSchoolRoomDetailsUseCase, viewSchoolRoomsUseCase);

        var schoolId = UUID.randomUUID();
        var expectedQuery = new ViewSchoolRoomsBySchoolIdQuery(schoolId, 0, 10);
        List<SchoolRoomResponse> emptyList = new ArrayList<>();
        var expectedPageResult = new PageResult<>(emptyList, 0, 10, 0L, 0);

        when(viewSchoolRoomsUseCase.execute(expectedQuery))
            .thenReturn(expectedPageResult);

        // Act
        controller.getSchoolRoomsBySchoolId(schoolId, 0, 0);

        // Assert
        verify(viewSchoolRoomsUseCase).execute(expectedQuery);
    }

    @Test
    void getSchoolRoomsBySchoolId_should_return_empty_list_when_no_rooms() {
        // Arrange
        var viewSchoolRoomDetailsUseCase = mock(ViewSchoolRoomDetailsUseCase.class);
        var viewSchoolRoomsUseCase = mock(ViewSchoolRoomsUseCase.class);
        var controller = new SchoolRoomController(viewSchoolRoomDetailsUseCase, viewSchoolRoomsUseCase);

        var schoolId = UUID.randomUUID();
        var expectedQuery = new ViewSchoolRoomsBySchoolIdQuery(schoolId, 0, 10);
        List<SchoolRoomResponse> emptyList = new ArrayList<>();
        var expectedPageResult = new PageResult<>(emptyList, 0, 10, 0L, 0);

        when(viewSchoolRoomsUseCase.execute(expectedQuery))
            .thenReturn(expectedPageResult);

        // Act
        var response = controller.getSchoolRoomsBySchoolId(schoolId, 0, 10);

        // Assert
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0L);
        assertThat(response.totalPages()).isEqualTo(0);
    }

    @Test
    void getSchoolRoomsBySchoolId_should_handle_pagination_correctly() {
        // Arrange
        var viewSchoolRoomDetailsUseCase = mock(ViewSchoolRoomDetailsUseCase.class);
        var viewSchoolRoomsUseCase = mock(ViewSchoolRoomsUseCase.class);
        var controller = new SchoolRoomController(viewSchoolRoomDetailsUseCase, viewSchoolRoomsUseCase);

        var schoolId = UUID.randomUUID();
        var roomId = UUID.randomUUID();
        var room = new SchoolRoomResponse(roomId, schoolId, "ROOM-001", "Phòng 1", "Mô tả", true);

        var expectedQuery = new ViewSchoolRoomsBySchoolIdQuery(schoolId, 1, 5);
        List<SchoolRoomResponse> rooms = new ArrayList<>();
        rooms.add(room);
        var expectedPageResult = new PageResult<>(
            rooms,
            1,
            5,
            6L,
            2
        );

        when(viewSchoolRoomsUseCase.execute(expectedQuery))
            .thenReturn(expectedPageResult);

        // Act
        var response = controller.getSchoolRoomsBySchoolId(schoolId, 1, 5);

        // Assert
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.totalElements()).isEqualTo(6L);
        assertThat(response.totalPages()).isEqualTo(2);
    }

    @Test
    void getSchoolRoomsBySchoolId_multiple_rooms_with_different_status() {
        // Arrange
        var viewSchoolRoomDetailsUseCase = mock(ViewSchoolRoomDetailsUseCase.class);
        var viewSchoolRoomsUseCase = mock(ViewSchoolRoomsUseCase.class);
        var controller = new SchoolRoomController(viewSchoolRoomDetailsUseCase, viewSchoolRoomsUseCase);

        var schoolId = UUID.randomUUID();
        var roomId1 = UUID.randomUUID();
        var roomId2 = UUID.randomUUID();
        var roomId3 = UUID.randomUUID();

        var room1 = new SchoolRoomResponse(roomId1, schoolId, "ROOM-001", "Phòng 1", "Hoạt động", true);
        var room2 = new SchoolRoomResponse(roomId2, schoolId, "ROOM-002", "Phòng 2", "Ngưng hoạt động", false);
        var room3 = new SchoolRoomResponse(roomId3, schoolId, "ROOM-003", "Phòng 3", null, true);

        var expectedQuery = new ViewSchoolRoomsBySchoolIdQuery(schoolId, 0, 10);
        List<SchoolRoomResponse> rooms = new ArrayList<>();
        rooms.add(room1);
        rooms.add(room2);
        rooms.add(room3);
        var expectedPageResult = new PageResult<>(
            rooms,
            0,
            10,
            3L,
            1
        );

        when(viewSchoolRoomsUseCase.execute(expectedQuery))
            .thenReturn(expectedPageResult);

        // Act
        var response = controller.getSchoolRoomsBySchoolId(schoolId, 0, 10);

        // Assert
        assertThat(response.content()).hasSize(3);
        assertThat(response.content().get(0).isActive()).isTrue();
        assertThat(response.content().get(1).isActive()).isFalse();
        assertThat(response.content().get(2).isActive()).isTrue();
        assertThat(response.content().get(1).description()).isEqualTo("Ngưng hoạt động");
        assertThat(response.content().get(2).description()).isNull();
    }

    @Test
    void getSchoolRoomById_with_inactive_room() {
        // Arrange
        var viewSchoolRoomDetailsUseCase = mock(ViewSchoolRoomDetailsUseCase.class);
        var viewSchoolRoomsUseCase = mock(ViewSchoolRoomsUseCase.class);
        var controller = new SchoolRoomController(viewSchoolRoomDetailsUseCase, viewSchoolRoomsUseCase);

        var roomId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();

        var expectedResponse = new SchoolRoomResponse(
            roomId,
            schoolId,
            "ROOM-INACTIVE",
            "Phòng không hoạt động",
            "Phòng này đã đóng",
            false
        );

        when(viewSchoolRoomDetailsUseCase.execute(new ViewSchoolRoomDetailsQuery(roomId)))
            .thenReturn(expectedResponse);

        // Act
        var response = controller.getSchoolRoomById(roomId);

        // Assert
        assertThat(response.isActive()).isFalse();
        assertThat(response.code()).isEqualTo("ROOM-INACTIVE");
    }

    @Test
    void getSchoolRoomsBySchoolId_custom_page_size() {
        // Arrange
        var viewSchoolRoomDetailsUseCase = mock(ViewSchoolRoomDetailsUseCase.class);
        var viewSchoolRoomsUseCase = mock(ViewSchoolRoomsUseCase.class);
        var controller = new SchoolRoomController(viewSchoolRoomDetailsUseCase, viewSchoolRoomsUseCase);

        var schoolId = UUID.randomUUID();
        var expectedQuery = new ViewSchoolRoomsBySchoolIdQuery(schoolId, 2, 20);
        List<SchoolRoomResponse> emptyList = new ArrayList<>();
        var expectedPageResult = new PageResult<>(emptyList, 2, 20, 0L, 0);

        when(viewSchoolRoomsUseCase.execute(expectedQuery))
            .thenReturn(expectedPageResult);

        // Act
        var response = controller.getSchoolRoomsBySchoolId(schoolId, 2, 20);

        // Assert
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(20);
        verify(viewSchoolRoomsUseCase).execute(expectedQuery);
    }
}
