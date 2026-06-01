package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.application.port.input.query.GetSchoolRoomByIdQuery;
import com.sep.vox.application.port.input.query.GetSchoolRoomsQuery;
import com.sep.vox.application.port.input.usecase.schoolroom.GetSchoolRoomByIdUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.GetSchoolRoomsUseCase;
import com.sep.vox.application.response.SchoolRoomResponse.SchoolRoomResponse;
import com.sep.vox.domain.common.PageResult;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class SchoolRoomGraphQlController {

    private final GetSchoolRoomByIdUseCase getSchoolRoomByIdUseCase;
    private final GetSchoolRoomsUseCase getSchoolRoomsUseCase;

    public SchoolRoomGraphQlController(GetSchoolRoomByIdUseCase getSchoolRoomByIdUseCase, GetSchoolRoomsUseCase getSchoolRoomsUseCase) {
        this.getSchoolRoomByIdUseCase = getSchoolRoomByIdUseCase;
        this.getSchoolRoomsUseCase = getSchoolRoomsUseCase;
    }

    // 1. SỬA CHỖ NÀY: Xóa (name = "RoomById")
    // Tự động map với query "getSchoolRoomById" trong schema vì tên hàm trùng nhau
    @QueryMapping
    public SchoolRoomResponse getSchoolRoomById(@Argument UUID id) {
        var query = new GetSchoolRoomByIdQuery(id);
        return getSchoolRoomByIdUseCase.execute(query);
    }

    // Tự động map với query "getSchoolRooms" trong schema
    @QueryMapping
//  @PreAuthorize("hasRole('SYSTEM_ADMIN')") // Mở lại khi bạn truyền được JWT Token vào GraphiQL nhé
    public PageResult<SchoolRoomResponse> getSchoolRooms(
            @Argument Integer page,
            @Argument Integer size) {

        int validPage = (page != null && page >= 0) ? page : 0;
        int validSize = (size != null && size > 0) ? size : 10;

        var query = new GetSchoolRoomsQuery(validPage, validSize);

        return getSchoolRoomsUseCase.execute(query);
    }
}