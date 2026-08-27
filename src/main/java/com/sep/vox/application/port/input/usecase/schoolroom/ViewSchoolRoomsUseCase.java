package com.sep.vox.application.port.input.usecase.schoolroom;

import com.sep.vox.application.port.input.query.ViewSchoolRoomsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.MyClassAccessGuard;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.mapper.SchoolRoomDtoMapper;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViewSchoolRoomsUseCase implements IUseCase<ViewSchoolRoomsQuery, PageResult<SchoolRoomFromDto>> {

    private final SchoolRoomRepository schoolRoomRepository;
    private final MyClassAccessGuard myClassAccessGuard;

    public ViewSchoolRoomsUseCase(
            SchoolRoomRepository schoolRoomRepository,
            MyClassAccessGuard myClassAccessGuard) {
        this.schoolRoomRepository = schoolRoomRepository;
        this.myClassAccessGuard = myClassAccessGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolRoomFromDto> execute(ViewSchoolRoomsQuery query) {
        // schoolId là tham số do client gửi lên, không phải suy ra từ token — phải chặn người gọi
        // đọc phòng của trường khác. Bắt buộc từ khi query này mở cho cả TEACHER (giáo viên cần
        // danh sách phòng để chọn phòng cho bài kiểm tra trên lớp).
        myClassAccessGuard.requireSchoolMembership(query.schoolId());

        PageResult<SchoolRoom> pageResult = schoolRoomRepository.findBySchoolId(
                query.schoolId(),
                query.page(), 
                query.size()
        );

        return new PageResult<>(
                pageResult.content().stream()
                        .map(SchoolRoomDtoMapper::toDto)
                        .toList(),
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages()
        );
    }
}