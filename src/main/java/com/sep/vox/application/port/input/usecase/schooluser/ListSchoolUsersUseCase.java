package com.sep.vox.application.port.input.usecase.schooluser;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ListSchoolUsersCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.mapper.SchoolUserDtoMapper;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ListSchoolUsersUseCase implements IUseCase<ListSchoolUsersCommand, PageResult<SchoolUserDto>> {

    private static final List<String> ALLOWED_ROLE_CODES = List.of("STUDENT", "TEACHER");

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;

    public ListSchoolUsersUseCase(
            UserContextPort userContextPort,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolUserDto> execute(ListSchoolUsersCommand input) {
        var callerId = userContextPort.getCurrentAuthenticatedUserId();

        var caller = userRepository.findById(callerId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        var schoolUsersPage = schoolUserRepository.findBySchoolId(input.schoolId(), input.page(), input.size());

        return SchoolUserDtoMapper.toSchoolUserPageDto(schoolUsersPage);
    }
}
