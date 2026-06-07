package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.application.port.input.command.UpdateSchoolCommand;
import com.sep.vox.application.port.input.command.UpdateSchoolGradeCommand;
import com.sep.vox.application.port.input.command.UpdateSchoolRoomCommand;
import com.sep.vox.application.port.input.query.*;
import com.sep.vox.application.port.input.usecase.school.UpdateSchoolUseCase;
import com.sep.vox.application.port.input.usecase.school.ViewSchoolsUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.UpdateSchoolGradeUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.ViewSchoolGradeDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.ViewSchoolGradesUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.UpdateSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomDetailsUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.ViewSchoolRoomsUseCase;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.SchoolGradeFromDto;
import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolGradeRequest;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolRequest;
import com.sep.vox.interfaces.graphql.dto.request.UpdateSchoolRoomRequest;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolCommandMapper;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolGradeCommandMapper;
import com.sep.vox.interfaces.graphql.mapper.UpdateSchoolRoomCommandMapper;
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

    //<editor-fold desc="Mocks">
    @Mock
    private ViewSchoolsUseCase viewSchoolsUseCase;
    @Mock
    private UpdateSchoolUseCase updateSchoolUseCase;
    @Mock
    private ViewSchoolRoomDetailsUseCase viewSchoolRoomDetailsUseCase;
    @Mock
    private ViewSchoolRoomsUseCase viewSchoolRoomsUseCase;
    @Mock
    private UpdateSchoolRoomUseCase updateSchoolRoomUseCase;
    @Mock
    private UpdateSchoolGradeUseCase updateSchoolGradeUseCase;
    @Mock
    private ViewSchoolGradesUseCase viewSchoolGradesUseCase;
    @Mock
    private ViewSchoolGradeDetailsUseCase viewSchoolGradeDetailsUseCase;
    //</editor-fold>

    private SchoolController controller;

    private MockedStatic<UpdateSchoolCommandMapper> schoolMapperMock;
    private MockedStatic<UpdateSchoolRoomCommandMapper> roomMapperMock;
    private MockedStatic<UpdateSchoolGradeCommandMapper> gradeMapperMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new SchoolController(
                viewSchoolsUseCase, updateSchoolUseCase,
                viewSchoolRoomDetailsUseCase, viewSchoolRoomsUseCase, updateSchoolRoomUseCase,
                updateSchoolGradeUseCase, viewSchoolGradesUseCase, viewSchoolGradeDetailsUseCase
        );
        schoolMapperMock = mockStatic(UpdateSchoolCommandMapper.class);
        roomMapperMock = mockStatic(UpdateSchoolRoomCommandMapper.class);
        gradeMapperMock = mockStatic(UpdateSchoolGradeCommandMapper.class);
    }

    @AfterEach
    void tearDown() {
        schoolMapperMock.close();
        roomMapperMock.close();
        gradeMapperMock.close();
    }

    @Test
    void updateSchool_should_call_usecase_and_return_id() {
        // Arrange
        var schoolId = UUID.randomUUID();
        var request = new UpdateSchoolRequest(schoolId, "New Name", "Desc", "123", "a@a.com", "domain", "address", 100);
        var command = new UpdateSchoolCommand(schoolId, "New Name", "Desc", "123", "a@a.com", "domain", "address", 100);

        schoolMapperMock.when(() -> UpdateSchoolCommandMapper.fromRequest(schoolId, request)).thenReturn(command);
        when(updateSchoolUseCase.execute(command)).thenReturn(schoolId);

        // Act
        UUID result = controller.updateSchool(request);

        // Assert
        assertThat(result).isEqualTo(schoolId);
        verify(updateSchoolUseCase).execute(command);
    }

    @Test
    void schools_should_return_paginated_schools() {
        // Arrange
        var page = 1;
        var size = 10;
        var schools = List.of(new SchoolDto(UUID.randomUUID(), "S1", "School One", null, null, null, null, null, 0, false, null, null));
        var expectedResult = new PageResult<>(schools, page, size, 1L, 1);
        when(viewSchoolsUseCase.execute(any(ViewSchoolsQuery.class))).thenReturn(expectedResult);

        // Act
        PageResult<SchoolDto> response = controller.schools(page, size);

        // Assert
        assertThat(response).isEqualTo(expectedResult);
        verify(viewSchoolsUseCase).execute(new ViewSchoolsQuery(page, size));
    }

    @Test
    void schoolRoom_should_return_room_details() {
        // Arrange
        var roomId = UUID.randomUUID();
        var expectedResponse = new SchoolRoomFromDto(roomId, UUID.randomUUID(), "CODE", "Name", "Desc", true, null, null, null, null);
        when(viewSchoolRoomDetailsUseCase.execute(any(ViewSchoolRoomDetailsQuery.class))).thenReturn(expectedResponse);

        // Act
        SchoolRoomFromDto response = controller.schoolRoom(roomId);

        // Assert
        assertThat(response).isEqualTo(expectedResponse);
        verify(viewSchoolRoomDetailsUseCase).execute(new ViewSchoolRoomDetailsQuery(roomId));
    }

    @Test
    void schoolRooms_should_return_paginated_rooms() {
        // Arrange
        var schoolId = UUID.randomUUID();
        var page = 1;
        var size = 10;
        var rooms = List.of(new SchoolRoomFromDto(UUID.randomUUID(),schoolId, "C1", "N1", "D1", true,null,null,null,null));
        var expectedResult = new PageResult<>(rooms, page, size, 1L, 1);
        when(viewSchoolRoomsUseCase.execute(any(ViewSchoolRoomsQuery.class))).thenReturn(expectedResult);

        // Act
        PageResult<SchoolRoomFromDto> response = controller.schoolRooms(schoolId, page, size);

        // Assert
        assertThat(response).isEqualTo(expectedResult);
        verify(viewSchoolRoomsUseCase).execute(new ViewSchoolRoomsQuery(schoolId, new PageRequest(page, size)));
    }

    @Test
    void updateSchoolRoom_should_call_usecase_and_return_id() {
        // Arrange
        var roomId = UUID.randomUUID();
        var request = new UpdateSchoolRoomRequest(roomId, "New Name", "New Desc", false);
        var command = new UpdateSchoolRoomCommand(roomId, "New Name", "New Desc", false);

        roomMapperMock.when(() -> UpdateSchoolRoomCommandMapper.fromRequest(roomId, request)).thenReturn(command);
        when(updateSchoolRoomUseCase.execute(command)).thenReturn(roomId);

        // Act
        UUID result = controller.updateSchoolRoom(request);

        // Assert
        assertThat(result).isEqualTo(roomId);
        verify(updateSchoolRoomUseCase).execute(command);
    }

    @Test
    void updateSchoolGrade_should_call_usecase_and_return_id() {
        // Arrange
        var gradeId = UUID.randomUUID();
        var request = new UpdateSchoolGradeRequest(gradeId,UUID.randomUUID(), "New Grade", null, null, null);
        var command = new UpdateSchoolGradeCommand(gradeId, UUID.randomUUID(), null, null, null, null);

        gradeMapperMock.when(() -> UpdateSchoolGradeCommandMapper.fromRequest(request)).thenReturn(command);
        when(updateSchoolGradeUseCase.execute(command)).thenReturn(gradeId);

        // Act
        UUID result = controller.updateSchoolGrade(request);

        // Assert
        assertThat(result).isEqualTo(gradeId);
        verify(updateSchoolGradeUseCase).execute(command);
    }

    @Test
    void schoolGrade_should_return_grade_details() {
        // Arrange
        var gradeId = UUID.randomUUID();
        var expectedResponse = new SchoolGradeFromDto(gradeId, UUID.randomUUID(), "Grade 1", "Grade 1", null, null, null, null);
        when(viewSchoolGradeDetailsUseCase.execute(any(ViewSchoolGradeDetailsQuery.class))).thenReturn(expectedResponse);

        // Act
        SchoolGradeFromDto response = controller.schoolGrade(gradeId);

        // Assert
        assertThat(response).isEqualTo(expectedResponse);
        verify(viewSchoolGradeDetailsUseCase).execute(new ViewSchoolGradeDetailsQuery(gradeId));
    }

    @Test
    void schoolGrades_should_return_paginated_grades() {
        // Arrange
        var schoolId = UUID.randomUUID();
        var page = 1;
        var size = 10;
        var grades = List.of(new SchoolGradeFromDto(UUID.randomUUID(), schoolId, "G1", "Grade 1", null, null, null, null));
        var expectedResult = new PageResult<>(grades, page, size, 1L, 1);
        when(viewSchoolGradesUseCase.execute(any(ViewSchoolGradesQuery.class))).thenReturn(expectedResult);

        // Act
        PageResult<SchoolGradeFromDto> response = controller.schoolGrades(schoolId, page, size);

        // Assert
        assertThat(response).isEqualTo(expectedResult);
        verify(viewSchoolGradesUseCase).execute(new ViewSchoolGradesQuery(schoolId, new PageRequest(page, size)));
    }
}