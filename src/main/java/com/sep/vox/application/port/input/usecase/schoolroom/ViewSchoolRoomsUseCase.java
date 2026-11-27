package com.sep.vox.application.port.input.usecase.schoolroom;

import com.sep.vox.application.port.input.query.schoolroom.ViewSchoolRoomsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.MyClassAccessGuard;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.mapper.SchoolRoomDtoMapper;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViewSchoolRoomsUseCase implements IUseCase<ViewSchoolRoomsQuery, PageResult<SchoolRoomFromDto>> {

    private final SchoolRoomRepository schoolRoomRepository;
    private final MyClassAccessGuard myClassAccessGuard;

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolRoomFromDto> execute(ViewSchoolRoomsQuery query) {
        // schoolId is a parameter sent by the client, not derived from the token -- the caller must be stopped from
        // reading the rooms of another school. Required since this query was opened to TEACHER too (teachers need
        // the room list to choose a room for a class test).
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