package com.sep.vox.application.port.input.usecase.assessmentpolicyschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.GetGradeLevelBandCeilingQuery;
import com.sep.vox.application.port.input.service.GradeLevelBandScopeGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.GradeLevelBandCeilingDto;
import com.sep.vox.domain.mapper.FrameworkResultBandDtoMapper;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Bản đọc dùng cho màn hình Tạo/Sửa Assessment Policy: hiện trần bậc TRƯỚC khi người dùng bấm
// submit, thay vì chỉ biết bị chặn sau khi CreateSchoolAssessmentPolicyUseCase/
// UpdateSchoolAssessmentPolicyUseCase ném lỗi. Validate ownership scope y hệt 2 use case đó để
// không lộ trần bậc của khối/lớp/niên khóa thuộc trường khác.
@Service
public class GetGradeLevelBandCeilingUseCase implements IUseCase<GetGradeLevelBandCeilingQuery, GradeLevelBandCeilingDto> {

    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final GradeLevelBandScopeGuardService gradeLevelBandScopeGuardService;

    public GetGradeLevelBandCeilingUseCase(
            UserRepository userRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository,
            GradeLevelRepository gradeLevelRepository,
            SchoolGradeRepository schoolGradeRepository,
            SchoolClassRepository schoolClassRepository,
            GradeLevelBandScopeGuardService gradeLevelBandScopeGuardService) {
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.gradeLevelRepository = gradeLevelRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.gradeLevelBandScopeGuardService = gradeLevelBandScopeGuardService;
    }

    @Override
    @Transactional(readOnly = true)
    public GradeLevelBandCeilingDto execute(GetGradeLevelBandCeilingQuery query) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Không thuộc trường học nào."));
        if (!schoolUser.getSchoolId().equals(query.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Không thể xem trần bậc của trường khác.");
        }

        int scopeCount = (query.schoolClassId() != null ? 1 : 0)
                + (query.schoolGradeId() != null ? 1 : 0)
                + (query.gradeLevelId() != null ? 1 : 0);
        if (scopeCount > 1) {
            throw new IllegalArgumentException("Chỉ được truyền đúng 1 phạm vi: Lớp, Khối năm học, HOẶC Khối.");
        }

        if (query.schoolClassId() != null) {
            SchoolClass schoolClass = schoolClassRepository.findById(query.schoolClassId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy Lớp học."));
            if (!query.schoolId().equals(schoolClass.getSchoolId())) {
                throw new ForbiddenException("Lớp học không thuộc trường của bạn.");
            }
        } else if (query.schoolGradeId() != null) {
            SchoolGrade schoolGrade = schoolGradeRepository.findById(query.schoolGradeId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy Khối năm học."));
            if (!query.schoolId().equals(schoolGrade.getSchoolId())) {
                throw new ForbiddenException("Khối năm học không thuộc trường của bạn.");
            }
        } else if (query.gradeLevelId() != null) {
            if (gradeLevelRepository.findById(query.gradeLevelId()).isEmpty()) {
                throw new NotFoundException("Không tìm thấy Khối.");
            }
        }

        return gradeLevelBandScopeGuardService
                .resolveCeiling(query.gradeLevelId(), query.schoolGradeId(), query.schoolClassId(), query.frameworkVersionId())
                .map(ceiling -> new GradeLevelBandCeilingDto(
                        FrameworkResultBandDtoMapper.toDto(ceiling.defaultBand()),
                        FrameworkResultBandDtoMapper.toDto(ceiling.hardMaxBand())))
                .orElse(null);
    }
}
