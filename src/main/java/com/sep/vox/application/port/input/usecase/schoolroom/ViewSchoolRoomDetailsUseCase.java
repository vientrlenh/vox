package com.sep.vox.application.port.input.usecase.schoolroom;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewSchoolRoomDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.SchoolRoomResponse.SchoolRoomResponse;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import org.springframework.stereotype.Service;

@Service
public class ViewSchoolRoomDetailsUseCase implements IUseCase<ViewSchoolRoomDetailsQuery, SchoolRoomResponse> {

    private final SchoolRoomRepository schoolRoomRepository;

    public ViewSchoolRoomDetailsUseCase(SchoolRoomRepository schoolRoomRepository) {
        this.schoolRoomRepository = schoolRoomRepository;
    }

    @Override
    public SchoolRoomResponse execute(ViewSchoolRoomDetailsQuery query) {
        // 1. Tìm phòng trong Database (hàm findById này bạn đã viết trong RepositoryImpl rồi)
        SchoolRoom room = schoolRoomRepository.findById(query.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phòng học với ID này."));

        // (Tùy chọn) Nếu nghiệp vụ cần: Bạn có thể Inject UserContextPort vào đây để kiểm tra
        // xem User hiện tại có quyền xem phòng của SchoolId này không (giống luồng Update).

        // 2. Map sang Response và trả về
        return new SchoolRoomResponse(
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
        );
    }
}