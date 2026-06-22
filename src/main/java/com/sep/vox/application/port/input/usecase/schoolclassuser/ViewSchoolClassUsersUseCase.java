package com.sep.vox.application.port.input.usecase.schoolclassuser;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolClassUsersQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassUserDto;
import com.sep.vox.domain.mapper.SchoolClassUserDtoMapper;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewSchoolClassUsersUseCase implements IUseCase<ViewSchoolClassUsersQuery, PageResult<SchoolClassUserDto>> {

    private final SchoolClassUserRepository schoolClassUserRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    public ViewSchoolClassUsersUseCase(
            SchoolClassUserRepository schoolClassUserRepository,
            SchoolClassRepository schoolClassRepository,
            UserRepository userRepository,
            SchoolRepository schoolRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository) {
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolClassUserDto> execute(ViewSchoolClassUsersQuery input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Trạng thái người dùng không hợp lệ");
        }
        var schoolId = getSchoolId(currentUserId);
        validateSchool(schoolId);
        validateSchoolClass(input.schoolClassId(), schoolId);

        var result = schoolClassUserRepository.findBySchoolClassId(input.schoolClassId(), input.page(), input.size());

        return SchoolClassUserDtoMapper.toSchoolClassUserDtoPage(result);
    }

    private UUID getSchoolId(UUID userId) {
        return schoolUserRepository.findByUserId(userId)
            .map(SchoolUser::getSchoolId)
            .orElseThrow(() -> new IllegalStateException("Người dùng hiện tại không thuộc trường nào"));
    }

    private void validateSchool(UUID schoolId) {
        var school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));
        if (!school.isActive()) {
            throw new IllegalStateException("Trường học không hoạt động");
        }
    }

    private void validateSchoolClass(UUID schoolClassId, UUID schoolId) {
        var schoolClass = schoolClassRepository.findById(schoolClassId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học"));
        if (!Objects.equals(schoolClass.getSchoolId(), schoolId)) {
            throw new NotFoundException("Không tìm thấy lớp học");
        }
    }

}
