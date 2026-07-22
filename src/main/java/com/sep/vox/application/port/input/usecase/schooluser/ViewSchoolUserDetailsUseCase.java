package com.sep.vox.application.port.input.usecase.schooluser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolUserDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.mapper.SchoolUserDtoMapper;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewSchoolUserDetailsUseCase implements IUseCase<ViewSchoolUserDetailsQuery, SchoolUserDto> {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;

    public ViewSchoolUserDetailsUseCase(
            UserContextPort userContextPort,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolUserDto execute(ViewSchoolUserDetailsQuery input) {
        var callerId = userContextPort.getCurrentAuthenticatedUserId();

        if (!userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Trạng thái người dùng không hợp lệ");
        }

        if (!userContextPort.isSystemAdmin() && !schoolUserRepository.existsBySchoolIdAndUserId(input.schoolId(), callerId)) {
            throw new ForbiddenException("Quyền truy cập không hợp lệ");
        }

        var schoolUser = schoolUserRepository.findBySchoolIdAndUserId(input.schoolId(), input.userId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng nhà trường theo yêu cầu"));

        // Không trả về người dùng đã bị xóa mềm (DISABLED)
        if (userRepository.existsByIdAndStatus(input.userId(), UserStatus.DISABLED)) {
            throw new NotFoundException("Không tìm thấy người dùng nhà trường theo yêu cầu");
        }

        return SchoolUserDtoMapper.toSchoolUserDto(schoolUser);
    }
}
