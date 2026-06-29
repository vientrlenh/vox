package com.sep.vox.application.port.input.usecase.schoolgradelevel;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolGradeLevelDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolGradeLevelDto;
import com.sep.vox.domain.mapper.SchoolGradeLevelDtoMapper;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewSchoolGradeLevelDetailsUseCase implements IUseCase<ViewSchoolGradeLevelDetailsQuery, SchoolGradeLevelDto> {

    private final SchoolGradeLevelRepository schoolGradeLevelRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public ViewSchoolGradeLevelDetailsUseCase(
            SchoolGradeLevelRepository schoolGradeLevelRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.schoolGradeLevelRepository = schoolGradeLevelRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolGradeLevelDto execute(ViewSchoolGradeLevelDetailsQuery input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        validateUserAndAccess(currentUserId, input.schoolId());

        if (!schoolRepository.existsById(input.schoolId())) {
            throw new NotFoundException("Không tìm thấy trường học.");
        }

        SchoolGradeLevel gradeLevel = schoolGradeLevelRepository.findById(input.gradeLevelId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy khối học."));

        if (!gradeLevel.getSchoolId().equals(input.schoolId())) {
            throw new NotFoundException("Không tìm thấy khối học.");
        }

        return SchoolGradeLevelDtoMapper.toDto(gradeLevel);
    }

    private void validateUserAndAccess(UUID userId, UUID targetSchoolId) {
        if (!userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc đã bị khóa.");
        }
        schoolUserRepository.findSchoolIdByUserId(userId).ifPresent(userSchoolId -> {
            if (!userSchoolId.equals(targetSchoolId)) {
                throw new ForbiddenException("Bạn không có quyền xem khối học của trường khác.");
            }
        });
    }
}
