package com.sep.vox.application.port.input.usecase.schoolroom;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewSchoolRoomDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.mapper.SchoolRoomDtoMapper;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViewSchoolRoomDetailsUseCase implements IUseCase<ViewSchoolRoomDetailsQuery, SchoolRoomFromDto> {

    private final SchoolRoomRepository schoolRoomRepository;

    public ViewSchoolRoomDetailsUseCase(SchoolRoomRepository schoolRoomRepository) {
        this.schoolRoomRepository = schoolRoomRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolRoomFromDto execute(ViewSchoolRoomDetailsQuery query) {
        return schoolRoomRepository.findById(query.id())
                .map(SchoolRoomDtoMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phòng học với ID đã cho."));
    }
}