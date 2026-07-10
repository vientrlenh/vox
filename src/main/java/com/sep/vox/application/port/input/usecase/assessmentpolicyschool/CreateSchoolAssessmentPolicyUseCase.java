package com.sep.vox.application.port.input.usecase.assessmentpolicyschool;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
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
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CreateSchoolAssessmentPolicyUseCase implements IUseCase<List<CreateAssessmentPolicyCommand>, List<UUID>> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final SupportedLanguageRepository languageRepository;
    private final SchoolGradeLevelRepository schoolGradeLevelRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    // Dùng để phát số "version" kế tiếp theo scope, xuyên suốt toàn bộ batch
    private record VersionScopeKey(UUID languageId, UUID frameworkVersionId,
                                    UUID schoolGradeLevelId, UUID schoolGradeId, UUID schoolClassId) {}

    // Dùng để chặn trùng (scope + Rubric Version) ngay trong cùng 1 lần gọi API (nhiều Policy trong 1 request)
    private record RubricScopeKey(UUID languageId, UUID frameworkVersionId,
                                   UUID schoolGradeLevelId, UUID schoolGradeId, UUID schoolClassId, UUID rubricVersionId) {}

    public CreateSchoolAssessmentPolicyUseCase(
            AssessmentPolicyRepository assessmentPolicyRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            SupportedLanguageRepository languageRepository,
            SchoolGradeLevelRepository schoolGradeLevelRepository,
            SchoolGradeRepository schoolGradeRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolUserRepository schoolUserRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.languageRepository = languageRepository;
        this.schoolGradeLevelRepository = schoolGradeLevelRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public List<UUID> execute(List<CreateAssessmentPolicyCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            throw new IllegalArgumentException("Danh sách Assessment Policy cần tạo không được để trống.");
        }

        // 1. Xác thực tài khoản
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản đã bị khóa.");

        var schoolUser = schoolUserRepository.findByUserId(currentUserId).orElseThrow(() -> new ForbiddenException("Không thuộc trường học nào."));
        UUID schoolId = commands.get(0).schoolId();
        if (!schoolUser.getSchoolId().equals(schoolId)) throw new ForbiddenException("BẢO MẬT: Không thể tạo Policy cho trường khác.");

        OffsetDateTime now = OffsetDateTime.now();
        List<AssessmentPolicy> policiesToSave = new ArrayList<>();
        Map<VersionScopeKey, Integer> nextVersionByScope = new HashMap<>();
        Set<RubricScopeKey> rubricScopesClaimed = new HashSet<>();

        for (CreateAssessmentPolicyCommand command : commands) {
            if (!schoolId.equals(command.schoolId())) {
                throw new ForbiddenException("BẢO MẬT: Không thể tạo Policy cho trường khác.");
            }

            // 2. Validate Framework
            FrameworkVersion frameworkVersion = frameworkVersionRepository.findFrameworkVersionById(command.frameworkVersionId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản Khung tiêu chuẩn."));
            if (frameworkVersion.getStatus() != FrameworkVersionStatus.PUBLISHED) {
                throw new IllegalStateException("Chỉ có thể tạo Assessment Policy trên Framework đã PUBLISHED.");
            }

            // 3. Validate Ngôn ngữ
            if (!languageRepository.existsById(command.languageId())) {
                throw new NotFoundException("Ngôn ngữ chỉ định không tồn tại.");
            }

            // 4. Validate Scope
            int scopeCount = (command.schoolClassId() != null ? 1 : 0) + (command.schoolGradeId() != null ? 1 : 0) + (command.schoolGradeLevelId() != null ? 1 : 0);
            if (scopeCount != 1) {
                throw new IllegalArgumentException("Phải chọn đúng 1 phạm vi áp dụng: Lớp, Khối năm học, HOẶC Khối.");
            }

            if (command.schoolClassId() != null) {
                SchoolClass schoolClass = schoolClassRepository.findById(command.schoolClassId()).orElseThrow(() -> new NotFoundException("Không tìm thấy Lớp học."));
                if (!schoolId.equals(schoolClass.getSchoolId())) throw new ForbiddenException("Lớp học không thuộc trường của bạn.");
            } else if (command.schoolGradeId() != null) {
                SchoolGrade schoolGrade = schoolGradeRepository.findById(command.schoolGradeId()).orElseThrow(() -> new NotFoundException("Không tìm thấy Khối năm học."));
                SchoolGradeLevel gradeLevel = schoolGradeLevelRepository.findById(schoolGrade.getSchoolGradeLevelId()).orElseThrow(() -> new NotFoundException("Lỗi cấu trúc cấp học."));
                if (!schoolId.equals(gradeLevel.getSchoolId())) throw new ForbiddenException("Khối năm học không thuộc trường của bạn.");
            } else {
                SchoolGradeLevel gradeLevel = schoolGradeLevelRepository.findById(command.schoolGradeLevelId()).orElseThrow(() -> new NotFoundException("Không tìm thấy Khối."));
                if (!schoolId.equals(gradeLevel.getSchoolId())) throw new ForbiddenException("Khối không thuộc trường của bạn.");
            }

            // 5. Validate Band
            FrameworkResultBand targetBand = frameworkResultBandRepository.findById(command.targetFrameworkBandId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy Band mục tiêu."));
            FrameworkResultBand minimumBand = frameworkResultBandRepository.findById(command.minimumFrameworkBandId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy Band tối thiểu."));
            if (!targetBand.getFrameworkVersionId().equals(command.frameworkVersionId()) || !minimumBand.getFrameworkVersionId().equals(command.frameworkVersionId())) {
                throw new IllegalStateException("Band mục tiêu/tối thiểu phải thuộc đúng Khung năng lực đang chọn.");
            }
            if (minimumBand.getOrder() > targetBand.getOrder()) {
                throw new IllegalStateException("Band tối thiểu không được cao hơn Band mục tiêu.");
            }

            // 6. Validate Date
            if (command.effectiveFrom() == null) throw new IllegalArgumentException("Ngày bắt đầu không được trống.");
            if (command.effectiveTo() != null && command.effectiveFrom().isAfter(command.effectiveTo())) {
                throw new IllegalArgumentException("Ngày kết thúc không được trước ngày bắt đầu.");
            }

            AssessmentPolicyStrictness strictness = command.strictness() != null ? command.strictness() : AssessmentPolicyStrictness.STANDARD;

            // Mỗi Policy tạo mới trong batch sẽ chiếm 1 số version riêng cho cùng scope,
            // vì DB chỉ unique theo scope+version, không phân biệt theo Rubric Version
            VersionScopeKey versionScopeKey = new VersionScopeKey(command.languageId(), command.frameworkVersionId(),
                    command.schoolGradeLevelId(), command.schoolGradeId(), command.schoolClassId());

            // 7. XỬ LÝ VÒNG LẶP RUBRIC (1 Rubric Version tương ứng đúng 1 Assessment Policy)
            for (UUID rubricVersionId : command.rubricVersionIds()) {
                RubricScopeKey rubricScopeKey = new RubricScopeKey(command.languageId(), command.frameworkVersionId(),
                        command.schoolGradeLevelId(), command.schoolGradeId(), command.schoolClassId(), rubricVersionId);
                if (!rubricScopesClaimed.add(rubricScopeKey)) {
                    throw new DuplicatedException("Bị trùng Rubric Version ID trong cùng 1 lần tạo: " + rubricVersionId);
                }

                RubricVersion rubricVersion = rubricVersionRepository.findById(rubricVersionId)
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản Rubric ID: " + rubricVersionId));
                Rubric rubric = rubricRepository.findById(rubricVersion.getRubricId())
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric gốc của version ID: " + rubricVersionId));

                if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !schoolId.equals(rubric.getSchoolId())) {
                    throw new ForbiddenException("Phiên bản Rubric không thuộc trường học của bạn.");
                }
                if (rubricVersion.getStatus() == RubricStatus.PUBLISHED) {
                    throw new IllegalStateException("Chỉ được gán Policy khi Phiên bản Rubric còn ở trạng thái DRAFT.");
                }
                if (!rubric.getFrameworkId().equals(frameworkVersion.getFrameworkId())) {
                    throw new IllegalStateException("Phiên bản Rubric và Khung năng lực không khớp nhau.");
                }

                boolean isDuplicated = assessmentPolicyRepository.existsActiveForScope(
                        schoolId, command.languageId(), command.frameworkVersionId(),
                        command.schoolGradeLevelId(), command.schoolGradeId(), command.schoolClassId(),
                        rubricVersionId);
                if (isDuplicated) {
                    throw new DuplicatedException("Đã tồn tại Assessment Policy cho Rubric ID: " + rubricVersionId + " trong phạm vi này. Hãy Archive bản cũ.");
                }

                int nextVersion = nextVersionByScope.computeIfAbsent(versionScopeKey, key ->
                        assessmentPolicyRepository.findMaxVersionForScope(
                                schoolId, key.languageId(), key.frameworkVersionId(),
                                key.schoolGradeLevelId(), key.schoolGradeId(), key.schoolClassId()) + 1);
                nextVersionByScope.put(versionScopeKey, nextVersion + 1);

                AssessmentPolicy newPolicy = new AssessmentPolicy(
                        schoolId, command.schoolGradeLevelId(), command.schoolGradeId(), command.schoolClassId(),
                        command.languageId(), command.frameworkVersionId(),
                        rubricVersionId, // Gắn ID từ vòng lặp
                        command.targetFrameworkBandId(), command.minimumFrameworkBandId(),
                        command.passingScore(), strictness, nextVersion, AssessmentPolicyStatus.DRAFT,
                        command.effectiveFrom(), command.effectiveTo(),
                        now, now, currentUserId, currentUserId
                );
                policiesToSave.add(newPolicy);
            }
        }

        // 8. Lưu 1 lần xuống DB
        List<AssessmentPolicy> savedPolicies = assessmentPolicyRepository.saveAll(policiesToSave);
        return savedPolicies.stream().map(ap -> ap.getId()).toList();
    }
}
