package com.sep.vox.application.port.input.usecase.schoolroom;

import com.sep.vox.application.port.input.query.ViewSchoolRoomsBySchoolIdQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.SchoolRoomResponse.SchoolRoomResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ViewSchoolRoomsUseCase implements IUseCase<ViewSchoolRoomsBySchoolIdQuery, PageResult<SchoolRoomResponse>> {

    private final SchoolRoomRepository schoolRoomRepository;

    public ViewSchoolRoomsUseCase(SchoolRoomRepository schoolRoomRepository) {
        this.schoolRoomRepository = schoolRoomRepository;
    }

    @Override
    public PageResult<SchoolRoomResponse> execute(ViewSchoolRoomsBySchoolIdQuery query) {
        // Lấy từ DB lên
        PageResult<SchoolRoom> pagedRooms = schoolRoomRepository.findBySchoolId(
                query.schoolId(), query.page(), query.size());

        // Map sang Response
        List<SchoolRoomResponse> mappedContent = pagedRooms.content().stream()
                .map(room -> new SchoolRoomResponse(
                        room.getId(),
                        room.getSchoolId(),
                        room.getCode(),
                        room.getName(),
                        room.getDescription(),
                        room.isActive(),
                        room.getCreatedAt(),
                        room.getCreatedBy(),
                        room.getUpdatedAt(),
                        room.getUpdatedBy()
                ))
                .toList();

        // Bọc lại
        return new PageResult<>(
                mappedContent,
                pagedRooms.page(),
                pagedRooms.size(),
                pagedRooms.totalElements(),
                pagedRooms.totalPages()
        );
    }
}