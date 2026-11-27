package com.sep.vox.application.port.input.usecase.schoolroom;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.schoolroom.ViewSchoolRoomDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.mapper.SchoolRoomDtoMapper;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViewSchoolRoomDetailsUseCase implements IUseCase<ViewSchoolRoomDetailsQuery, SchoolRoomFromDto> {

    private final SchoolRoomRepository schoolRoomRepository;

    @Override
    @Transactional(readOnly = true)
    public SchoolRoomFromDto execute(ViewSchoolRoomDetailsQuery query) {
        return schoolRoomRepository.findById(query.id())
                .map(SchoolRoomDtoMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("No room found with the given ID."));
    }
}