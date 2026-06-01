package com.sep.vox.application.port.input.usecase.schoolroom;

import com.sep.vox.application.port.input.query.GetSchoolRoomsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.SchoolRoomResponse.SchoolRoomResponse;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.schoolroom.SchoolRoom;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ViewSchoolRoomsUseCase implements IUseCase<GetSchoolRoomsQuery, PageResult<SchoolRoomResponse>> {

    private final SchoolRoomRepository schoolRoomRepository;

    public ViewSchoolRoomsUseCase(SchoolRoomRepository schoolRoomRepository) {
        this.schoolRoomRepository = schoolRoomRepository;
    }

    @Override
    public PageResult<SchoolRoomResponse> execute(GetSchoolRoomsQuery query) {

        // 1. Gọi xuống Repository để lấy dữ liệu phân trang (Trả về PageResult<SchoolRoom>)
        // Tùy vào interface hiện tại của bạn, có thể truyền thẳng page, size hoặc bọc qua PageRequest
        PageResult<SchoolRoom> pagedRooms = schoolRoomRepository.findAll(query.page(), query.size());

        // 2. Map cái list content bên trong từ Model sang Response
        List<SchoolRoomResponse> mappedContent = pagedRooms.content().stream()
                .map(room -> new SchoolRoomResponse(
                        room.getId(),
                        room.getSchoolId(),
                        room.getCode(),
                        room.getName(),
                        room.getDescription(),
                        room.isActive()
                ))
                .toList();

        // 3. Đóng gói lại vào class PageResult có sẵn của bạn và trả về
        return new PageResult<>(
                mappedContent,
                pagedRooms.page(),
                pagedRooms.size(),
                pagedRooms.totalElements(),
                pagedRooms.totalPages()
        );
    }
}