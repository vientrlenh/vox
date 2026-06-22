package com.sep.vox.application.port.input.usecase.schoolroom;

import com.sep.vox.application.port.input.query.ViewSchoolRoomsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
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

    public ViewSchoolRoomsUseCase(SchoolRoomRepository schoolRoomRepository) {
        this.schoolRoomRepository = schoolRoomRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolRoomFromDto> execute(ViewSchoolRoomsQuery query) {

        // Cần đảm bảo bạn đã tạo hàm findAllBySchoolId(schoolId, pageRequest) trong SchoolRoomRepository
        PageResult<SchoolRoom> pageResult = schoolRoomRepository.findAllBySchoolId(
                query.schoolId(),
                query.pageRequest()
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