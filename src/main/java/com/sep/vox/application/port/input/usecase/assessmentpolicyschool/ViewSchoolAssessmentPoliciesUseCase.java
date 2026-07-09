package com.sep.vox.application.port.input.usecase.assessmentpolicyschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolAssessmentPoliciesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.AssessmentPolicyDto;
import com.sep.vox.domain.mapper.AssessmentPolicyDtoMapper;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViewSchoolAssessmentPoliciesUseCase implements IUseCase<ViewSchoolAssessmentPoliciesQuery, PageResult<AssessmentPolicyDto>> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;

    public ViewSchoolAssessmentPoliciesUseCase(
            AssessmentPolicyRepository assessmentPolicyRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            SchoolRepository schoolRepository,
            SchoolUserRepository schoolUserRepository) {
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.schoolRepository = schoolRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssessmentPolicyDto> execute(ViewSchoolAssessmentPoliciesQuery query) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản bị khóa.");
        }

        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được liên kết với bất kỳ trường học nào."));
        if (!schoolUser.getSchoolId().equals(query.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền truy cập dữ liệu của trường khác.");
        }

        var school = schoolRepository.findById(query.schoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Hành động bị từ chối: Trường học này đang bị vô hiệu hóa.");
        }

        String safeStatus = null;
        if (query.status() != null && !query.status().isBlank()) {
            try {
                safeStatus = AssessmentPolicyStatus.valueOf(query.status().trim().toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Trạng thái (status) không hợp lệ. Chỉ chấp nhận DRAFT, PUBLISHED, ARCHIVED.");
            }
        }

        if (query.effectiveFrom() != null && query.effectiveTo() != null && query.effectiveFrom().isAfter(query.effectiveTo())) {
            throw new IllegalArgumentException("Khoảng thời gian effectiveFrom/effectiveTo không hợp lệ.");
        }

        var policyPage = assessmentPolicyRepository.findAllBySchoolId(query.schoolId(), safeStatus, query.languageId(), query.rubricVersionId(),
                query.effectiveFrom(), query.effectiveTo(), query.page(), query.size());
        return AssessmentPolicyDtoMapper.toDtoPage(policyPage);
    }
}
