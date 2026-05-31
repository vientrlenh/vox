package com.sep.vox.application.port.input.usecase.schooluser;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.schooluser.SchoolUserResponseMapper;
import com.sep.vox.application.port.input.command.ListSchoolUsersCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.SchoolUserQueryRepository;
import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ListSchoolUsersUseCase implements IUseCase<ListSchoolUsersCommand, PageResult<SchoolUserResponse>> {

    private static final List<String> ALLOWED_ROLE_CODES = List.of("STUDENT", "TEACHER");

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserQueryRepository schoolUserQueryRepository;

    public ListSchoolUsersUseCase(
            UserContextPort userContextPort,
            UserRepository userRepository,
            SchoolUserQueryRepository schoolUserQueryRepository) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.schoolUserQueryRepository = schoolUserQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolUserResponse> execute(ListSchoolUsersCommand input) {
        var callerId = userContextPort.getCurrentAuthenticatedUserId();

        var caller = userRepository.findById(callerId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        if (!input.schoolId().equals(caller.getSchoolId())) {
            throw new IllegalArgumentException("Không có quyền thực hiện thao tác này");
        }

        var usersPage = schoolUserQueryRepository.findBySchoolIdAndRoleCodes(
            input.schoolId(),
            ALLOWED_ROLE_CODES,
            new PageRequest(input.page(), input.size())
        );

        var responses = usersPage.content().stream()
            .map(SchoolUserResponseMapper::toResponse)
            .toList();

        return new PageResult<>(
            responses,
            usersPage.page(),
            usersPage.size(),
            usersPage.totalElements(),
            usersPage.totalPages()
        );
    }
}
