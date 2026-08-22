package com.sep.vox.application.port.input.usecase.assessmentpolicysystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSystemAssessmentPolicyDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentPolicyDto;
import com.sep.vox.domain.mapper.AssessmentPolicyDtoMapper;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViewSystemAssessmentPolicyDetailsUseCase implements IUseCase<ViewSystemAssessmentPolicyDetailsQuery, AssessmentPolicyDto> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public ViewSystemAssessmentPolicyDetailsUseCase(
            AssessmentPolicyRepository assessmentPolicyRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public AssessmentPolicyDto execute(ViewSystemAssessmentPolicyDetailsQuery query) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản bị khóa.");
        }

        AssessmentPolicy policy = assessmentPolicyRepository.findById(query.policyId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Assessment Policy này."));

        if (policy.getSchoolId() != null) {
            throw new ForbiddenException("Assessment Policy này không thuộc về Hệ thống.");
        }

        // Cùng lằn ranh với ViewSystemAssessmentPoliciesUseCase: trường chỉ xem được bản mẫu đã ban
        // hành. Thiếu chốt này thì giấu bản nháp khỏi danh sách là vô nghĩa -- đoán id là xem được.
        if (!userContextPort.isSystemAdmin() && policy.getStatus() != AssessmentPolicyStatus.PUBLISHED) {
            throw new ForbiddenException("Chỉ xem được chính sách mẫu đã ban hành (PUBLISHED).");
        }

        return AssessmentPolicyDtoMapper.toDto(policy);
    }
}
