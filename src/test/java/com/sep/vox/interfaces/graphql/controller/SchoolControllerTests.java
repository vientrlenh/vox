package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.application.port.input.command.UpdateSchoolCommand;
import com.sep.vox.application.port.input.command.UpdateSchoolRoomCommand;
import com.sep.vox.application.port.input.query.ViewSchoolRoomDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolRoomsBySchoolIdQuery;
import com.sep.vox.application.port.input.usecase.school.UpdateSchoolUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.UpdateSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomsUseCase;
import com.sep.vox.application.response.SchoolRoomResponse.SchoolRoomResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolRequest;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolRoomRequest;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolCommandMapper;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolRoomMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchoolControllerTests {

    @Mock
    private UpdateSchoolUseCase updateSchoolUseCase;
    @Mock
    private ViewSchoolRoomDetailsUseCase viewSchoolRoomDetailsUseCase;
    @Mock
    private ViewSchoolRoomsUseCase viewSchoolRoomsUseCase;
    @Mock
    private UpdateSchoolRoomUseCase updateSchoolRoomUseCase;

    private SchoolController controller;

    private MockedStatic<UpdateSchoolCommandMapper> schoolMapperMock;
    private MockedStatic<UpdateSchoolRoomMapper> roomMapperMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new SchoolController(updateSchoolUseCase, viewSchoolRoomDetailsUseCase, viewSchoolRoomsUseCase, updateSchoolRoomUseCase);
        schoolMapperMock = mockStatic(UpdateSchoolCommandMapper.class);
        roomMapperMock = mockStatic(UpdateSchoolRoomMapper.class);
    }

    @AfterEach
    void tearDown() {
        schoolMapperMock.close();
        roomMapperMock.close();
    }

    @Test
    void updateSchool_should_return_updated_id() {
        // Arrange
        var schoolId = UUID.randomUUID();
        var request = new UpdateSchoolRequest(schoolId, "New Name", null, null, null, null, null, null);
        var command = new UpdateSchoolCommand(schoolId, "New Name", null, null, null, null, null, null);

        schoolMapperMock.when(() -> UpdateSchoolCommandMapper.fromRequest(schoolId, request)).thenReturn(command);
        when(updateSchoolUseCase.execute(command)).thenReturn(schoolId);

        // Act
        var response = controller.updateSchool(request);

        // Assert
        assertThat(response).isEqualTo(schoolId);
        verify(updateSchoolUseCase).execute(command);
    }

    @Test
    void getSchoolRoomById_should_return_school_room() {
        // Arrange
        var roomId = UUID.randomUUID();
        var expectedResponse = new SchoolRoomResponse(roomId, UUID.randomUUID(), "CODE", "Name", "Desc", true, null, null, null, null);
        when(viewSchoolRoomDetailsUseCase.execute(any(ViewSchoolRoomDetailsQuery.class))).thenReturn(expectedResponse);

        // Act
        SchoolRoomResponse response = controller.getSchoolRoomById(roomId);

        // Assert
        assertThat(response).isEqualTo(expectedResponse);
        verify(viewSchoolRoomDetailsUseCase).execute(new ViewSchoolRoomDetailsQuery(roomId));
    }

    @Test
    void getSchoolRoomsBySchoolId_should_return_paginated_rooms() {
        // Arrange
        var schoolId = UUID.randomUUID();
        var page = 0;
        var size = 10;
        var rooms = List.of(new SchoolRoomResponse(UUID.randomUUID(), schoolId, "C1", "N1", "D1", true, null, null, null, null));
        var expectedResult = new PageResult<>(rooms, page, size, 1L, 1);
        when(viewSchoolRoomsUseCase.execute(any(ViewSchoolRoomsBySchoolIdQuery.class))).thenReturn(expectedResult);

        // Act
        PageResult<SchoolRoomResponse> response = controller.getSchoolRoomsBySchoolId(schoolId, page, size);

        // Assert
        assertThat(response).isEqualTo(expectedResult);
        verify(viewSchoolRoomsUseCase).execute(new ViewSchoolRoomsBySchoolIdQuery(schoolId, page, size));
    }

    @Test
    void updateSchoolRoom_should_return_updated_id() {
        // Arrange
        var roomId = UUID.randomUUID();
        var request = new UpdateSchoolRoomRequest(roomId, "New Name", "New Desc", false);
        var command = new UpdateSchoolRoomCommand(roomId, "New Name", "New Desc", false);

        roomMapperMock.when(() -> UpdateSchoolRoomMapper.fromRequest(roomId, request)).thenReturn(command);
        when(updateSchoolRoomUseCase.execute(command)).thenReturn(roomId);

        // Act
        UUID result = controller.updateSchoolRoom(request);

        // Assert
        assertThat(result).isEqualTo(roomId);
        verify(updateSchoolRoomUseCase).execute(command);
    }
}
