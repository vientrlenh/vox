package com.sep.vox.application.port.input.usecase.schoolgrade;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolGradesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolGradeDto;

import com.sep.vox.domain.mapper.SchoolGradeDtoMapper;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewSchoolGradesUseCase implements IUseCase<ViewSchoolGradesQuery, PageResult<SchoolGradeDto>> {

    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public ViewSchoolGradesUseCase(
            SchoolGradeRepository schoolGradeRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolGradeDto> execute(ViewSchoolGradesQuery query) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        validateUserAndAccess(currentUserId, query.schoolId());

        if (!schoolRepository.existsById(query.schoolId())) {
            throw new NotFoundException("Không tìm thấy trường học.");
        }

        // 1. Gọi DB lấy danh sách phân trang (PageResult<SchoolGrade>)
        //    status null/blank → mặc định ẩn Năm học đã xóa mềm (ARCHIVED).
        //    Truyền status=ARCHIVED để lấy đúng bản đã xóa mềm.
        PageResult<SchoolGrade> pageResult = schoolGradeRepository.findAllBySchoolId(
                query.schoolId(),
                query.schoolGradeLevelId(),
                parseStatus(query.status()),
                query.page(),
                query.size()
        );

        // 2. Chuyển đổi sang DTO và trả về
        return new PageResult<>(
                pageResult.content().stream()
                        .map(SchoolGradeDtoMapper::toSchoolGradeDto)
                        .toList(),
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages()
        );
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

    private String parseStatus(String status) {
        var normalized = StringNormalization.trimAndCollapseSpaces(status);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        try {
            return SchoolGradeStatus.valueOf(normalized.toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái năm học không hợp lệ");
        }
    }
}
