package com.sep.vox.application.port.input.usecase.assessmentpolicysystem;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.PublishSystemAssessmentPoliciesByRubricVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublishSystemAssessmentPoliciesByRubricVersionUseCase
        implements IUseCase<PublishSystemAssessmentPoliciesByRubricVersionCommand, List<UUID>> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public List<UUID> execute(PublishSystemAssessmentPoliciesByRubricVersionCommand command) {
        // 1. Kiểm tra tài khoản System Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        // 2. Kiểm tra Rubric Version phải tồn tại và đang ở trạng thái DRAFT
        RubricVersion rubricVersion = rubricVersionRepository.findById(command.rubricVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Phiên bản Rubric."));
        if (rubricVersion.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ có thể publish hàng loạt Assessment Policy khi Rubric Version liên kết đang ở trạng thái DRAFT.");
        }

        // 3. Lấy toàn bộ Assessment Policy hệ thống đang DRAFT liên kết với Rubric Version này
        List<AssessmentPolicy> draftPolicies = assessmentPolicyRepository
                .findDraftSystemWideByRubricVersionId(command.rubricVersionId());
        if (draftPolicies.isEmpty()) {
            throw new ResourceNotFoundException("Không có Assessment Policy hệ thống nào đang ở trạng thái DRAFT liên kết với Rubric Version này.");
        }

        // 4. Validate and publish each policy (roll everything back if one policy is invalid)
        for (AssessmentPolicy policy : draftPolicies) {
            FrameworkVersion frameworkVersion = frameworkVersionRepository.findById(policy.getFrameworkVersionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Phiên bản Khung tiêu chuẩn (Framework Version) liên kết."));
            if (frameworkVersion.getStatus() != FrameworkVersionStatus.PUBLISHED) {
                throw new IllegalArgumentException("Không thể xuất bản Assessment Policy (ID: " + policy.getId()
                        + ") vì Phiên bản Khung tiêu chuẩn liên kết không còn ở trạng thái PUBLISHED.");
            }

            policy.setStatus(AssessmentPolicyStatus.PUBLISHED);
        }

        return assessmentPolicyRepository.saveAll(draftPolicies)
                .stream().map(ap -> ap.getId()).toList();
    }
}