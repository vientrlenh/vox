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

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CreateSystemAssessmentPolicyUseCase implements IUseCase<List<CreateAssessmentPolicyCommand>, List<UUID>> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final SupportedLanguageRepository languageRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    // Dùng để phát số "version" kế tiếp theo scope, xuyên suốt toàn bộ batch.
    //
    // Từ 2026-08-22 có thêm gradeLevelId: policy hệ thống giờ khai được theo KHỐI. Khối lớp
    // đã là catalog dùng chung toàn hệ thống (V41) và không gắn với niên khóa nào, nên một chính
    // sách mẫu cho "Khối 10" vẫn đúng khi trường mở niên khóa năm sau -- không phải khai lại.
    // Vẫn cho phép null = mẫu áp cho mọi khối, giữ nguyên hành vi cũ.
    private record VersionScopeKey(UUID languageId, UUID frameworkVersionId, UUID gradeLevelId) {}

    public CreateSystemAssessmentPolicyUseCase(
            AssessmentPolicyRepository assessmentPolicyRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            SupportedLanguageRepository languageRepository,
            GradeLevelRepository gradeLevelRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.languageRepository = languageRepository;
        this.gradeLevelRepository = gradeLevelRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public List<UUID> execute(List<CreateAssessmentPolicyCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            throw new IllegalArgumentException("Danh sách Assessment Policy cần tạo không được để trống.");
        }

        // 1. Validate System Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản đã bị khóa.");

        Instant now = Instant.now();
        List<AssessmentPolicy> policiesToSave = new ArrayList<>();
        Map<VersionScopeKey, Integer> nextVersionByScope = new HashMap<>();
        Set<VersionScopeKey> scopeClaimsInBatch = new HashSet<>();

        for (CreateAssessmentPolicyCommand command : commands) {
            // 2. Validate Framework & Language
            FrameworkVersion frameworkVersion = frameworkVersionRepository.findById(command.frameworkVersionId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản Khung tiêu chuẩn."));
            if (frameworkVersion.getStatus() != FrameworkVersionStatus.PUBLISHED) {
                throw new IllegalStateException("Chỉ có thể tạo Policy trên Framework đã PUBLISHED.");
            }
            if (!languageRepository.existsById(command.languageId())) {
                throw new NotFoundException("Ngôn ngữ không tồn tại.");
            }

            // 2b. Khối (tùy chọn). Luồng SYSTEM không có trường nên KHÔNG nhận niên khóa/lớp --
            //     hai thứ đó thuộc về một trường cụ thể. Chỉ Khối là khái niệm toàn hệ thống.
            if (command.schoolGradeId() != null || command.schoolClassId() != null) {
                throw new IllegalArgumentException(
                        "Chính sách hệ thống chỉ giới hạn được tới cấp Khối; Niên khóa và Lớp thuộc phạm vi của từng trường.");
            }
            if (command.gradeLevelId() != null
                    && gradeLevelRepository.findById(command.gradeLevelId()).isEmpty()) {
                throw new NotFoundException("Không tìm thấy Khối.");
            }

            // 3. Validate Band
            FrameworkResultBand targetBand = frameworkResultBandRepository.findById(command.targetFrameworkBandId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy Band mục tiêu."));
            if (!targetBand.getFrameworkVersionId().equals(command.frameworkVersionId())) {
                throw new IllegalStateException("Band mục tiêu phải thuộc Khung năng lực đang chọn.");
            }

            // 4. Validate Date
            if (command.effectiveFrom() == null) throw new IllegalArgumentException("Ngày bắt đầu không được để trống.");
            if (command.effectiveTo() != null && command.effectiveFrom().isAfter(command.effectiveTo())) {
                throw new IllegalArgumentException("Ngày kết thúc không được trước ngày bắt đầu.");
            }

            AssessmentPolicyStrictness strictness = command.strictness() != null ? command.strictness() : AssessmentPolicyStrictness.STANDARD;

            // Mỗi Policy tạo mới trong batch sẽ chiếm 1 số version riêng cho cùng scope (ngôn ngữ + framework),
            // vì DB chỉ unique theo scope+version, không phân biệt theo Rubric Version
            VersionScopeKey versionScopeKey = new VersionScopeKey(command.languageId(), command.frameworkVersionId(),
                    command.gradeLevelId());

            // 5. Mỗi phạm vi (ngôn ngữ + framework version) chỉ được ĐÚNG MỘT chính sách còn hiệu
            // lực. Bổ sung 2026-08-14: trước đó luồng SYSTEM không có phép kiểm trùng nào cả --
            // không chặn trong batch, cũng không hỏi DB -- nên policy hệ thống chồng lên nhau tự do
            // trong khi findActivePolicy chỉ dùng được một bản. Xem lý do đầy đủ ở
            // CreateSchoolAssessmentPolicyUseCase.
            if (!scopeClaimsInBatch.add(versionScopeKey)) {
                throw new DuplicatedException(
                        "Trong cùng một lần tạo có hai Assessment Policy trùng ngôn ngữ và Khung"
                                + " tiêu chuẩn. Mỗi phạm vi chỉ được một chính sách.");
            }
            if (assessmentPolicyRepository.existsActiveForScopeAnyRubricVersion(
                    null, command.languageId(), command.frameworkVersionId(),
                    command.gradeLevelId(), null, null)) {
                throw new DuplicatedException("Phạm vi này đã có một Assessment Policy hệ thống còn"
                        + " hiệu lực (DRAFT hoặc PUBLISHED). Hãy Archive bản cũ trước khi tạo bản mới.");
            }

            UUID rubricVersionId = command.rubricVersionId();
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

            // Nhiều chính sách ĐƯỢC PHÉP dùng chung 1 Phiên bản Rubric (V44 gỡ ràng buộc 1-1 của
            // V38): lớp chuyên và lớp thường cùng khối cần chính sách riêng theo phạm vi Lớp nhưng
            // vẫn chấm bằng cùng một bộ tiêu chí. Cái phải giữ duy nhất là "mỗi phạm vi chỉ một
            // chính sách còn hiệu lực" -- đã kiểm ở ScopeClaimKey và existsActiveForScopeAnyRubricVersion
            // bên trên.

            int nextVersion = nextVersionByScope.computeIfAbsent(versionScopeKey, key ->
                    assessmentPolicyRepository.findMaxVersionForScope(
                            null, key.languageId(), key.frameworkVersionId(),
                            key.gradeLevelId(), null, null) + 1);
            nextVersionByScope.put(versionScopeKey, nextVersion + 1);

            AssessmentPolicy newPolicy = new AssessmentPolicy(
                    null, command.gradeLevelId(), null, null,
                    command.languageId(), command.frameworkVersionId(),
                    rubricVersionId,
                    command.targetFrameworkBandId(),
                    command.passingScore(), strictness, nextVersion, AssessmentPolicyStatus.DRAFT,
                    command.effectiveFrom(), command.effectiveTo(),
                    now, now, currentUserId, currentUserId
            );
            policiesToSave.add(newPolicy);
        }

        // 6. Lưu 1 lần xuống DB
        List<AssessmentPolicy> savedPolicies = assessmentPolicyRepository.saveAll(policiesToSave);
        return savedPolicies.stream().map(ap -> ap.getId()).toList();
    }
}
