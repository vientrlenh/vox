package com.sep.vox.application.port.input.usecase.assessmentpolicyschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.PublishSchoolAssessmentPoliciesByRubricVersionCommand;
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
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PublishSchoolAssessmentPoliciesByRubricVersionUseCase
        implements IUseCase<PublishSchoolAssessmentPoliciesByRubricVersionCommand, List<UUID>> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public PublishSchoolAssessmentPoliciesByRubricVersionUseCase(
            AssessmentPolicyRepository assessmentPolicyRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            RubricVersionRepository rubricVersionRepository,
            SchoolRepository schoolRepository,
            SchoolUserRepository schoolUserRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.schoolRepository = schoolRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public List<UUID> execute(PublishSchoolAssessmentPoliciesByRubricVersionCommand command) {
        // 1. Kiểm tra tài khoản School Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        // 2. Kiểm tra tài khoản thuộc đúng trường học yêu cầu
        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được phân bổ vào trường học nào."));
        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền can thiệp vào Assessment Policy của trường khác.");
        }

        var school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Hành động bị từ chối: Trường học này đang bị vô hiệu hóa trên hệ thống.");
        }

        // 3. Kiểm tra Rubric Version phải tồn tại và đang ở trạng thái DRAFT
        RubricVersion rubricVersion = rubricVersionRepository.findById(command.rubricVersionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản Rubric."));
        if (rubricVersion.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể publish hàng loạt Assessment Policy khi Rubric Version liên kết đang ở trạng thái DRAFT.");
        }

        // 4. Lấy toàn bộ Assessment Policy của trường đang DRAFT liên kết với Rubric Version này
        List<AssessmentPolicy> draftPolicies = assessmentPolicyRepository
                .findDraftBySchoolIdAndRubricVersionId(command.schoolId(), command.rubricVersionId());
        if (draftPolicies.isEmpty()) {
            throw new NotFoundException("Không có Assessment Policy nào của trường học đang ở trạng thái DRAFT liên kết với Rubric Version này.");
        }

        // 5. Kiểm tra và publish từng policy (rollback toàn bộ nếu có 1 policy không hợp lệ)
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
                .stream().map(AssessmentPolicy::getId).toList();
    }
}