package com.sep.vox.application.port.input.usecase.schoolgrade;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolGradeDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolGradeDto;
import com.sep.vox.domain.mapper.SchoolGradeDtoMapper;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewSchoolGradeDetailsUseCase implements IUseCase<ViewSchoolGradeDetailsQuery, SchoolGradeDto> {

    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolGradeLevelRepository schoolGradeLevelRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public ViewSchoolGradeDetailsUseCase(
            SchoolGradeRepository schoolGradeRepository,
            SchoolGradeLevelRepository schoolGradeLevelRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolGradeLevelRepository = schoolGradeLevelRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolGradeDto execute(ViewSchoolGradeDetailsQuery input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        validateUserAndAccess(currentUserId, input.schoolId());

        if (!schoolRepository.existsById(input.schoolId())) {
            throw new NotFoundException("Không tìm thấy trường học.");
        }

        SchoolGrade grade = schoolGradeRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy năm học/khóa học."));

        // Năm học không lưu schoolId trực tiếp; phải bắc cầu qua Khối (GradeLevel) để xác định trường sở hữu.
        var gradeLevel = schoolGradeLevelRepository.findById(grade.getSchoolGradeLevelId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy năm học/khóa học."));

        if (!gradeLevel.getSchoolId().equals(input.schoolId())) {
            throw new NotFoundException("Không tìm thấy năm học/khóa học.");
        }

        return SchoolGradeDtoMapper.toSchoolGradeDto(grade);
    }

    private void validateUserAndAccess(UUID userId, UUID targetSchoolId) {
        if (!userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc đã bị khóa.");
        }
        if (!userContextPort.isSystemAdmin()) {
            UUID userSchoolId = schoolUserRepository.findSchoolIdByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền xem năm học của trường khác."));
            if (!userSchoolId.equals(targetSchoolId)) {
                throw new ForbiddenException("Bạn không có quyền xem năm học của trường khác.");
            }
        }
    }
}
