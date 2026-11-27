package com.sep.vox.application.port.input.usecase.assessmentpolicyschool;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.assessmentpolicysystem.CreateAssessmentPolicyCommand;
import com.sep.vox.application.port.input.service.GradeLevelBandScopeGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
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
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateSchoolAssessmentPolicyUseCase implements IUseCase<List<CreateAssessmentPolicyCommand>, List<UUID>> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final SupportedLanguageRepository languageRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final GradeLevelBandScopeGuardService gradeLevelBandScopeGuardService;

    // Used to issue the next "version" number per scope across the whole batch
    private record VersionScopeKey(UUID languageId, UUID frameworkVersionId,
                                    UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId) {}

    // Rejects two Policies with the same SCOPE within a single API call. Does NOT include rubricVersionId:
    // a scope may have exactly one policy, whichever Rubric version it points to.
    private record ScopeClaimKey(UUID languageId, UUID frameworkVersionId,
                                  UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId) {}

    @Override
    @Transactional
    public List<UUID> execute(List<CreateAssessmentPolicyCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            throw new IllegalArgumentException("Danh sách Assessment Policy cần tạo không được để trống.");
        }

        UUID schoolId = requireSchoolOfCaller(commands);

        List<AssessmentPolicy> policiesToSave = new ArrayList<>();
        Map<VersionScopeKey, Integer> nextVersionByScope = new HashMap<>();
        Set<ScopeClaimKey> scopeClaimsInBatch = new HashSet<>();
        // Remember the Class -> School grade -> Grade level and band ceiling lookups for the whole batch:
        // one API call that creates many policies usually reuses just a few grade levels and a few frameworks.
        GradeLevelBandScopeGuardService.Batch bandScopeBatch = gradeLevelBandScopeGuardService.newBatch();

        for (CreateAssessmentPolicyCommand command : commands) {
            policiesToSave.add(buildDraftPolicy(command, schoolId, nextVersionByScope, scopeClaimsInBatch, bandScopeBatch));
        }

        List<AssessmentPolicy> savedPolicies = assessmentPolicyRepository.saveAll(policiesToSave);
        return savedPolicies.stream().map(ap -> ap.getId()).toList();
    }

    private UUID requireSchoolOfCaller(List<CreateAssessmentPolicyCommand> commands) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản đã bị khóa.");

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId).orElseThrow(() -> new ForbiddenException("Không thuộc trường học nào."));
        UUID schoolId = commands.get(0).schoolId();
        if (!schoolUser.getSchoolId().equals(schoolId)) throw new ForbiddenException("BẢO MẬT: Không thể tạo Policy cho trường khác.");
        return schoolId;
    }

    private AssessmentPolicy buildDraftPolicy(
            CreateAssessmentPolicyCommand command,
            UUID schoolId,
            Map<VersionScopeKey, Integer> nextVersionByScope,
            Set<ScopeClaimKey> scopeClaimsInBatch,
            GradeLevelBandScopeGuardService.Batch bandScopeBatch) {
        requireSameSchool(command, schoolId);
        FrameworkVersion frameworkVersion = requirePublishedFrameworkVersion(command);
        requireLanguageExists(command);
        requireSingleScopeInSchool(command, schoolId);
        requireTargetBandWithinCeiling(command, bandScopeBatch);
        requireValidEffectivePeriod(command);
        AssessmentPolicyStrictness strictness = command.strictness() != null ? command.strictness() : AssessmentPolicyStrictness.STANDARD;
        claimScope(command, scopeClaimsInBatch);
        requireUsableRubricVersion(command, frameworkVersion, schoolId);
        requireNoActivePolicyForScope(command, schoolId);
        int nextVersion = nextVersionFor(command, schoolId, nextVersionByScope);

        return AssessmentPolicy.draftForScope(schoolId, command.gradeLevelId(),
            command.schoolGradeId(), command.schoolClassId(), command.languageId(), command.frameworkVersionId(),
            command.rubricVersionId(), command.targetFrameworkBandId(), command.passingScore(), strictness, nextVersion,
            command.effectiveFrom(), command.effectiveTo());
    }

    private void requireSameSchool(CreateAssessmentPolicyCommand command, UUID schoolId) {
        if (!schoolId.equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Không thể tạo Policy cho trường khác.");
        }
    }

    private FrameworkVersion requirePublishedFrameworkVersion(CreateAssessmentPolicyCommand command) {
        FrameworkVersion frameworkVersion = frameworkVersionRepository.findById(command.frameworkVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Phiên bản Khung tiêu chuẩn."));
        if (frameworkVersion.getStatus() != FrameworkVersionStatus.PUBLISHED) {
            throw new IllegalArgumentException("Chỉ có thể tạo Assessment Policy trên Framework đã PUBLISHED.");
        }
        return frameworkVersion;
    }

    private void requireLanguageExists(CreateAssessmentPolicyCommand command) {
        if (!languageRepository.existsById(command.languageId())) {
            throw new ResourceNotFoundException("Ngôn ngữ chỉ định không tồn tại.");
        }
    }

    private void requireSingleScopeInSchool(CreateAssessmentPolicyCommand command, UUID schoolId) {
        int scopeCount = (command.schoolClassId() != null ? 1 : 0) + (command.schoolGradeId() != null ? 1 : 0) + (command.gradeLevelId() != null ? 1 : 0);
        if (scopeCount != 1) {
            throw new IllegalArgumentException("Phải chọn đúng 1 phạm vi áp dụng: Lớp, Khối năm học, HOẶC Khối.");
        }

        if (command.schoolClassId() != null) {
            SchoolClass schoolClass = schoolClassRepository.findById(command.schoolClassId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Lớp học."));
            if (!schoolId.equals(schoolClass.getSchoolId())) throw new ForbiddenException("Lớp học không thuộc trường của bạn.");
        } else if (command.schoolGradeId() != null) {
            SchoolGrade schoolGrade = schoolGradeRepository.findById(command.schoolGradeId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Khối năm học."));
            if (!schoolId.equals(schoolGrade.getSchoolId())) throw new ForbiddenException("Khối năm học không thuộc trường của bạn.");
        } else {
            // Grade level is a shared catalog: it only has to exist, it no longer belongs to a school.
            if (gradeLevelRepository.findById(command.gradeLevelId()).isEmpty()) {
                throw new ResourceNotFoundException("Không tìm thấy Khối.");
            }
        }
    }

    private void requireTargetBandWithinCeiling(CreateAssessmentPolicyCommand command, GradeLevelBandScopeGuardService.Batch bandScopeBatch) {
        FrameworkResultBand targetBand = frameworkResultBandRepository.findById(command.targetFrameworkBandId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Band mục tiêu."));
        if (!targetBand.getFrameworkVersionId().equals(command.frameworkVersionId())) {
            throw new IllegalArgumentException("Band mục tiêu phải thuộc đúng Khung năng lực đang chọn.");
        }

        // Band ceiling per grade level. The Class / school grade has to be traced back to its Grade level, not only
        // checked when the scope is a Grade level -- otherwise choosing a narrower scope makes the ceiling vanish.
        bandScopeBatch.assertWithinScope(
                command.gradeLevelId(), command.schoolGradeId(), command.schoolClassId(),
                command.frameworkVersionId(), targetBand);
    }

    private void requireValidEffectivePeriod(CreateAssessmentPolicyCommand command) {
        if (command.effectiveFrom() == null) throw new IllegalArgumentException("Ngày bắt đầu không được trống.");
        if (command.effectiveTo() != null && command.effectiveFrom().isAfter(command.effectiveTo())) {
            throw new IllegalArgumentException("Ngày kết thúc không được trước ngày bắt đầu.");
        }
    }

    /**
     * Each scope may have EXACTLY ONE policy in effect.
     *
     * <p>Tightened 2026-08-14. Before that, a scope could keep several policies as long as they pointed at different
     * Rubric Versions, and `version` was used as a sequence number to dodge the unique index (scope + version).
     * But when grading, findActivePolicy takes only ONE of them (ORDER BY scope narrowness, then version DESC,
     * LIMIT 1) -- so the others are never used, yet they still count in existsPublishedByRubricVersionId and keep
     * acting as a publish gate for their Rubric. Dead data that still holds power, and nothing tells the user.
     */
    private void claimScope(CreateAssessmentPolicyCommand command, Set<ScopeClaimKey> scopeClaimsInBatch) {
        ScopeClaimKey scopeClaimKey = new ScopeClaimKey(command.languageId(), command.frameworkVersionId(),
                command.gradeLevelId(), command.schoolGradeId(), command.schoolClassId());
        if (!scopeClaimsInBatch.add(scopeClaimKey)) {
            throw new ResourceDuplicatedException(
                    "Trong cùng một lần tạo có hai Assessment Policy trùng phạm vi áp dụng."
                            + " Mỗi phạm vi chỉ được một chính sách.");
        }
    }

    private void requireUsableRubricVersion(CreateAssessmentPolicyCommand command, FrameworkVersion frameworkVersion, UUID schoolId) {
        UUID rubricVersionId = command.rubricVersionId();
        RubricVersion rubricVersion = rubricVersionRepository.findById(rubricVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Phiên bản Rubric ID: " + rubricVersionId));
        Rubric rubric = rubricRepository.findById(rubricVersion.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Rubric gốc của version ID: " + rubricVersionId));

        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !schoolId.equals(rubric.getSchoolId())) {
            throw new ForbiddenException("Phiên bản Rubric không thuộc trường học của bạn.");
        }
        requireRubricVersionNotArchived(rubricVersion);
        if (!rubric.getFrameworkId().equals(frameworkVersion.getFrameworkId())) {
            throw new IllegalArgumentException("Phiên bản Rubric và Khung năng lực không khớp nhau.");
        }
    }

    /**
     * Blocks ARCHIVED, does NOT block PUBLISHED.
     *
     * <p>Loosened 2026-08-23. The old rule ("can only be attached while DRAFT") forced every policy of a Rubric
     * version to be declared BEFORE that version was published -- yet publishing is the
     * condition for the version to be usable for an assessment (the assessment creation screen only lists PUBLISHED versions).
     * That means a school had exactly one chance to declare every class that would use this criteria set; adding
     * one specialized class next year would require copying the whole Rubric. That is a direct contradiction of
     * V44 (several policies sharing one Rubric version).
     *
     * <p>Removing the PUBLISHED block breaks nothing: PublishSchoolAssessmentPolicyUseCase deliberately does not look at the
     * status of the Rubric version, existsPublishedByRubricVersionId is only a gate for publishing a VERSION (meaningless once
     * it is already published), and the grading path reads
     * assessments.assessment_policy_id directly so a new policy does not apply retroactively to attempts already graded. Conversely,
     * a PUBLISHED version is frozen -- attaching a policy to it is SAFER than attaching to a
     * DRAFT whose criteria underneath can still be edited.
     *
     * <p>ARCHIVED still has to be blocked (it used to slip through): attaching a policy to a revoked version
     * means grading students on a scale the school has abandoned.
     */
    private void requireRubricVersionNotArchived(RubricVersion rubricVersion) {
        if (rubricVersion.getStatus() == RubricStatus.ARCHIVED) {
            throw new IllegalArgumentException(
                    "Không gán được Policy vào Phiên bản Rubric đã lưu trữ (ARCHIVED).");
        }
    }

    /**
     * Several policies ARE ALLOWED to share one Rubric Version (V44 dropped the 1-1 constraint of V38): a specialised
     * class and a regular class of the same grade level need separate policies at Class scope but still grade with
     * the same set of criteria. What must stay unique is "each scope has only one policy in effect" -- checked by
     * {@link #claimScope} and by existsActiveForScopeAnyRubricVersion here.
     */
    private void requireNoActivePolicyForScope(CreateAssessmentPolicyCommand command, UUID schoolId) {
        boolean isDuplicated = assessmentPolicyRepository.existsActiveForScopeAnyRubricVersion(
                schoolId, command.languageId(), command.frameworkVersionId(),
                command.gradeLevelId(), command.schoolGradeId(), command.schoolClassId());
        if (isDuplicated) {
            throw new ResourceDuplicatedException("Phạm vi này đã có một Assessment Policy còn hiệu lực"
                    + " (DRAFT hoặc PUBLISHED). Hãy Archive bản cũ trước khi tạo bản mới.");
        }
    }

    private int nextVersionFor(CreateAssessmentPolicyCommand command, UUID schoolId, Map<VersionScopeKey, Integer> nextVersionByScope) {
        // Each new Policy in the batch takes its own version number within the same scope,
        // because the DB is only unique by scope+version and does not distinguish by Rubric Version
        VersionScopeKey versionScopeKey = new VersionScopeKey(command.languageId(), command.frameworkVersionId(),
                command.gradeLevelId(), command.schoolGradeId(), command.schoolClassId());
        int nextVersion = nextVersionByScope.computeIfAbsent(versionScopeKey, key ->
                assessmentPolicyRepository.findMaxVersionForScope(
                        schoolId, key.languageId(), key.frameworkVersionId(),
                        key.gradeLevelId(), key.schoolGradeId(), key.schoolClassId()) + 1);
        nextVersionByScope.put(versionScopeKey, nextVersion + 1);
        return nextVersion;
    }
}
