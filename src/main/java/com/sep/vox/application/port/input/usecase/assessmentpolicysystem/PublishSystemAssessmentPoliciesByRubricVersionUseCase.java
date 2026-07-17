package com.sep.vox.application.port.input.usecase.assessmentpolicysystem;

import com.sep.vox.application.exception.NotFoundException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PublishSystemAssessmentPoliciesByRubricVersionUseCase
        implements IUseCase<PublishSystemAssessmentPoliciesByRubricVersionCommand, List<UUID>> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public PublishSystemAssessmentPoliciesByRubricVersionUseCase(
            AssessmentPolicyRepository assessmentPolicyRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            RubricVersionRepository rubricVersionRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

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
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản Rubric."));
        if (rubricVersion.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể publish hàng loạt Assessment Policy khi Rubric Version liên kết đang ở trạng thái DRAFT.");
        }

        // 3. Lấy toàn bộ Assessment Policy hệ thống đang DRAFT liên kết với Rubric Version này
        List<AssessmentPolicy> draftPolicies = assessmentPolicyRepository
                .findDraftSystemWideByRubricVersionId(command.rubricVersionId());
        if (draftPolicies.isEmpty()) {
            throw new NotFoundException("Không có Assessment Policy hệ thống nào đang ở trạng thái DRAFT liên kết với Rubric Version này.");
        }

        // 4. Kiểm tra và publish từng policy (rollback toàn bộ nếu có 1 policy không hợp lệ)
        OffsetDateTime now = OffsetDateTime.now();
        for (AssessmentPolicy policy : draftPolicies) {
            FrameworkVersion frameworkVersion = frameworkVersionRepository.findById(policy.getFrameworkVersionId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản Khung tiêu chuẩn (Framework Version) liên kết."));
            if (frameworkVersion.getStatus() != FrameworkVersionStatus.PUBLISHED) {
                throw new IllegalStateException("Không thể xuất bản Assessment Policy (ID: " + policy.getId()
                        + ") vì Phiên bản Khung tiêu chuẩn liên kết không còn ở trạng thái PUBLISHED.");
            }

            policy.setStatus(AssessmentPolicyStatus.PUBLISHED);
            policy.setUpdatedAt(now);
            policy.setUpdatedBy(currentUserId);
        }

        return assessmentPolicyRepository.saveAll(draftPolicies)
                .stream().map(ap -> ap.getId()).toList();
    }
}