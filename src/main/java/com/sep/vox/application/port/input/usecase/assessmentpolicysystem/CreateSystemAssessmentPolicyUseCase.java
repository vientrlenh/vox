package com.sep.vox.application.port.input.usecase.assessmentpolicysystem;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateAssessmentPolicyCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CreateSystemAssessmentPolicyUseCase implements IUseCase<CreateAssessmentPolicyCommand, List<UUID>> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final SupportedLanguageRepository languageRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public CreateSystemAssessmentPolicyUseCase(
            AssessmentPolicyRepository assessmentPolicyRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            SupportedLanguageRepository languageRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.languageRepository = languageRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public List<UUID> execute(CreateAssessmentPolicyCommand command) {
        // 1. Validate System Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản đã bị khóa.");

        // 2. Validate Framework & Language
        FrameworkVersion frameworkVersion = frameworkVersionRepository.findById(command.frameworkVersionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản Khung tiêu chuẩn."));
        if (frameworkVersion.getStatus() != FrameworkVersionStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ có thể tạo Policy trên Framework đã PUBLISHED.");
        }
        if (!languageRepository.existsById(command.languageId())) {
            throw new NotFoundException("Ngôn ngữ không tồn tại.");
        }

        // 3. Validate Band
        FrameworkResultBand targetBand = frameworkResultBandRepository.findById(command.targetFrameworkBandId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Band mục tiêu."));
        FrameworkResultBand minimumBand = frameworkResultBandRepository.findById(command.minimumFrameworkBandId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Band tối thiểu."));
        if (!targetBand.getFrameworkVersionId().equals(command.frameworkVersionId()) || !minimumBand.getFrameworkVersionId().equals(command.frameworkVersionId())) {
            throw new IllegalStateException("Band mục tiêu/tối thiểu phải thuộc Khung năng lực đang chọn.");
        }
        if (minimumBand.getOrder() > targetBand.getOrder()) {
            throw new IllegalStateException("Band tối thiểu không được cao hơn Band mục tiêu.");
        }

        // 4. Validate Date
        if (command.effectiveFrom() == null) throw new IllegalArgumentException("Ngày bắt đầu không được để trống.");
        if (command.effectiveTo() != null && command.effectiveFrom().isAfter(command.effectiveTo())) {
            throw new IllegalArgumentException("Ngày kết thúc không được trước ngày bắt đầu.");
        }

        int nextVersion = assessmentPolicyRepository.findMaxVersionForScope(
                null, command.languageId(), command.frameworkVersionId(), null, null, null) + 1;

        OffsetDateTime now = OffsetDateTime.now();
        AssessmentPolicyStrictness strictness = command.strictness() != null ? command.strictness() : AssessmentPolicyStrictness.STANDARD;

        // 5. XỬ LÝ VÒNG LẶP RUBRIC
        List<AssessmentPolicy> policiesToSave = new ArrayList<>();

        for (UUID rubricVersionId : command.rubricVersionIds()) {
            RubricVersion rubricVersion = rubricVersionRepository.findById(rubricVersionId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản Rubric ID: " + rubricVersionId));
            Rubric rubric = rubricRepository.findById(rubricVersion.getRubricId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric gốc của version ID: " + rubricVersionId));

            if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
                throw new IllegalStateException("Chỉ được dùng Rubric SYSTEM cho luồng System Admin.");
            }
            if (rubricVersion.getStatus() == RubricStatus.PUBLISHED) {
                throw new IllegalStateException("Chỉ được gán Policy khi Phiên bản Rubric còn ở trạng thái DRAFT.");
            }
            if (!rubric.getFrameworkId().equals(frameworkVersion.getFrameworkId())) {
                throw new IllegalStateException("Phiên bản Rubric và Khung năng lực không khớp nhau.");
            }

            boolean isDuplicated = assessmentPolicyRepository.existsActiveForScope(
                    null, command.languageId(), command.frameworkVersionId(), null, null, null, rubricVersionId);
            if (isDuplicated) {
                throw new DuplicatedException("Đã tồn tại Policy cho Rubric ID: " + rubricVersionId + ". Hãy Archive bản cũ.");
            }

            AssessmentPolicy newPolicy = new AssessmentPolicy(
                    null, null, null, null,
                    command.languageId(), command.frameworkVersionId(),
                    rubricVersionId, // Gắn ID từ vòng lặp
                    command.targetFrameworkBandId(), command.minimumFrameworkBandId(),
                    command.passingScore(), strictness, nextVersion, AssessmentPolicyStatus.DRAFT,
                    command.effectiveFrom(), command.effectiveTo(),
                    now, now, currentUserId, currentUserId
            );
            policiesToSave.add(newPolicy);
        }

        // 6. Lưu 1 lần xuống DB
        List<AssessmentPolicy> savedPolicies = assessmentPolicyRepository.saveAll(policiesToSave);
        return savedPolicies.stream().map(AssessmentPolicy::getId).toList();
    }
}