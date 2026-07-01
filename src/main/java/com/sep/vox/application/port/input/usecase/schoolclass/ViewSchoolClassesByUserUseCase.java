package com.sep.vox.application.port.input.usecase.schoolclass;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolClassesByUserQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.mapper.SchoolClassDtoMapper;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewSchoolClassesByUserUseCase implements IUseCase<ViewSchoolClassesByUserQuery, PageResult<SchoolClassDto>> {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolClassRepository schoolClassRepository;

    public ViewSchoolClassesByUserUseCase(
            UserContextPort userContextPort,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            SchoolClassRepository schoolClassRepository) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.schoolClassRepository = schoolClassRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolClassDto> execute(ViewSchoolClassesByUserQuery input) {
        var callerId = userContextPort.getCurrentAuthenticatedUserId();

        if (!userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Trạng thái người dùng không hợp lệ");
        }
        if (!schoolUserRepository.existsBySchoolIdAndUserId(input.schoolId(), callerId)) {
            throw new ForbiddenException("Quyền truy cập không hợp lệ");
        }
        if (!schoolUserRepository.existsBySchoolIdAndUserId(input.schoolId(), input.userId())) {
            throw new NotFoundException("Không tìm thấy người dùng trong trường học");
        }

        var schoolClassesPage = schoolClassRepository.findByUserId(
            input.schoolId(),
            input.userId(),
            input.page(),
            input.size()
        );

        return SchoolClassDtoMapper.toDtoPage(schoolClassesPage);
    }
}
