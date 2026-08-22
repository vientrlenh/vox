package com.sep.vox.application.port.input.usecase.assessmentpolicyschool;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CloneSystemAssessmentPolicyToSchoolCommand;
import com.sep.vox.application.port.input.service.GradeLevelBandScopeGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.service.rubric.RubricCloneService;

/**
 * Trường sao một chính sách mẫu đã ban hành của hệ thống về làm chính sách riêng.
 *
 * <p>Sao chính sách luôn KÉO THEO sao bộ tiêu chí: bản mẫu trỏ vào rubric thuộc sở hữu SYSTEM, mà
 * {@code CreateSchoolAssessmentPolicyUseCase} chỉ chấp nhận rubric của chính trường đó. Cùng lý do
 * đã sinh ra {@code CloneSystemRubricToSchoolUseCase} -- ở đây gộp hai bước làm một để trường không
 * phải sao rubric rồi tự gõ lại toàn bộ thông số chính sách.
 *
 * <p>Những gì đi theo bản mẫu: ngôn ngữ, phiên bản khung, bậc mục tiêu, điểm đạt, độ chặt. Những gì
 * trường tự quyết: mã/tên bộ tiêu chí bản sao, cách tính điểm, khoảng hiệu lực, và phạm vi áp dụng
 * khi bản mẫu chưa khai khối.
 *
 * <p>Bản sao ra ở trạng thái DRAFT, giống {@code CloneSystemRubricToSchoolUseCase}: luồng tiếp theo
 * y hệt khi trường tự soạn -- ban hành chính sách rồi ban hành phiên bản rubric.
 *
 * <p>Muốn nhiều lớp cùng dùng bản sao này thì KHÔNG sao lại: từ V44 nhiều chính sách dùng chung
 * được một phiên bản rubric, nên các lớp sau chỉ cần tạo chính sách mới trỏ vào cùng
 * {@code rubricVersionId} qua {@code CreateSchoolAssessmentPolicyUseCase}.
 */
@Service
public class CloneSystemAssessmentPolicyToSchoolUseCase
        implements IUseCase<CloneSystemAssessmentPolicyToSchoolCommand, UUID> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final RubricRepository rubricRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricCloneService rubricCloneService;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final GradeLevelBandScopeGuardService gradeLevelBandScopeGuardService;

    public CloneSystemAssessmentPolicyToSchoolUseCase(
            AssessmentPolicyRepository assessmentPolicyRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            RubricRepository rubricRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricCriterionRepository rubricCriterionRepository,
            RubricCloneService rubricCloneService,
            GradeLevelRepository gradeLevelRepository,
            SchoolGradeRepository schoolGradeRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolUserRepository schoolUserRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            GradeLevelBandScopeGuardService gradeLevelBandScopeGuardService) {
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.rubricRepository = rubricRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricCloneService = rubricCloneService;
        this.gradeLevelRepository = gradeLevelRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.gradeLevelBandScopeGuardService = gradeLevelBandScopeGuardService;
    }

    /** Phạm vi bản sao sẽ áp dụng, sau khi gộp phạm vi của bản mẫu với lựa chọn của trường. */
    private record TargetScope(UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId) {}

    @Override
    @Transactional
    public UUID execute(CloneSystemAssessmentPolicyToSchoolCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được liên kết với trường học nào."));
        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền sao chép về trường khác.");
        }

        // 1. Bản mẫu phải là chính sách HỆ THỐNG và đã ban hành. Bản nháp của hệ thống là việc đang
        //    làm dở, chưa rà soát -- cùng lằn ranh CloneSystemRubricToSchoolUseCase áp cho rubric.
        AssessmentPolicy source = assessmentPolicyRepository.findById(command.sourcePolicyId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chính sách mẫu."));
        if (source.getSchoolId() != null) {
            throw new ForbiddenException("Chỉ được sao chép từ chính sách mẫu của hệ thống.");
        }
        if (source.getStatus() != AssessmentPolicyStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ được sao chép từ chính sách mẫu đã được ban hành (PUBLISHED).");
        }

        // 2. Khung của bản mẫu phải còn PUBLISHED -- chính sách mới cũng phải thoả điều kiện mà
        //    CreateSchoolAssessmentPolicyUseCase đặt ra, nếu không sẽ tạo được nhưng không ban hành được.
        var frameworkVersion = frameworkVersionRepository.findById(source.getFrameworkVersionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản Khung tiêu chuẩn của bản mẫu."));
        if (frameworkVersion.getStatus() != FrameworkVersionStatus.PUBLISHED) {
            throw new IllegalStateException("Khung tiêu chuẩn của bản mẫu không còn ở trạng thái PUBLISHED.");
        }

        // 3. Bộ tiêu chí nguồn.
        var sourceVersion = rubricVersionRepository.findById(source.getRubricVersionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Rubric của bản mẫu."));
        var sourceRubric = rubricRepository.findById(sourceVersion.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric gốc của bản mẫu."));
        if (sourceRubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Chỉ được sao chép từ bộ tiêu chí mẫu của hệ thống.");
        }
        // Sao một bản rỗng tạo ra phiên bản trường không bao giờ ban hành được (cửa "phải có tiêu
        // chí" chặn lại), và họ chỉ phát hiện ở bước cuối.
        if (rubricCriterionRepository.findByRubricVersionId(sourceVersion.getId()).isEmpty()) {
            throw new IllegalStateException("Bản mẫu này chưa có tiêu chí nào nên không sao chép được.");
        }

        TargetScope scope = resolveScope(command, source);

        // 4. Bậc mục tiêu đi theo bản mẫu, nhưng vẫn phải lọt trần bậc của Khối bên phía trường:
        //    bản mẫu là mẫu chung, trần là quy định riêng của khối đang áp.
        var targetBand = frameworkResultBandRepository.findById(source.getTargetFrameworkBandId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Band mục tiêu của bản mẫu."));
        gradeLevelBandScopeGuardService.assertWithinScope(
                scope.gradeLevelId(), scope.schoolGradeId(), scope.schoolClassId(),
                source.getFrameworkVersionId(), targetBand);

        // 5. Mỗi phạm vi chỉ được một chính sách còn hiệu lực -- ràng buộc này V44 KHÔNG đụng tới.
        if (assessmentPolicyRepository.existsActiveForScopeAnyRubricVersion(
                command.schoolId(), source.getLanguageId(), source.getFrameworkVersionId(),
                scope.gradeLevelId(), scope.schoolGradeId(), scope.schoolClassId())) {
            throw new DuplicatedException("Phạm vi này đã có một Assessment Policy còn hiệu lực"
                    + " (DRAFT hoặc PUBLISHED). Hãy Archive bản cũ trước khi sao bản mới.");
        }

        if (command.effectiveFrom() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu hiệu lực không được để trống.");
        }
        if (command.effectiveTo() != null && command.effectiveFrom().isAfter(command.effectiveTo())) {
            throw new IllegalArgumentException("Ngày kết thúc không được trước ngày bắt đầu.");
        }

        // 6. Sao bộ tiêu chí. Kiểm mã trùng trước để báo lỗi đọc được, thay vì để ràng buộc unique
        //    của DB ném ra thông báo người dùng không biết sửa gì -- giống CloneSystemRubricToSchoolUseCase.
        var safeCode = requireText(command.rubricCode(), "Mã bộ tiêu chí không được để trống.");
        var safeName = requireText(command.rubricName(), "Tên bộ tiêu chí không được để trống.");
        var safeDescription = command.rubricDescription() == null
                ? null
                : StringNormalization.trimAndCollapseSpaces(command.rubricDescription());

        if (rubricRepository.existsByOwnerTypeAndSchoolIdAndLanguageIdAndFrameworkIdAndCode(
                RubricOwnerType.SCHOOL.toString(), command.schoolId(), sourceRubric.getLanguageId(),
                sourceRubric.getFrameworkId(), safeCode)) {
            throw new DuplicatedException("Trường của bạn đã có một bộ tiêu chí mang mã '" + safeCode
                    + "' cho ngôn ngữ và khung năng lực này. Hãy dùng mã khác (ví dụ đặt theo khối:"
                    + " ENG-K10, ENG-K11).");
        }

        var clonedVersion = rubricCloneService.cloneToSchoolAsDraft(
                sourceRubric,
                sourceVersion,
                command.schoolId(),
                safeCode,
                safeName,
                safeDescription,
                command.totalScoreMethod() == null ? sourceVersion.getTotalScoreMethod() : command.totalScoreMethod(),
                currentUserId);

        // 7. Chính sách mới, DRAFT, trỏ vào bản sao rubric vừa tạo.
        Instant now = Instant.now();
        int nextVersion = assessmentPolicyRepository.findMaxVersionForScope(
                command.schoolId(), source.getLanguageId(), source.getFrameworkVersionId(),
                scope.gradeLevelId(), scope.schoolGradeId(), scope.schoolClassId()) + 1;

        AssessmentPolicy cloned = new AssessmentPolicy(
                command.schoolId(), scope.gradeLevelId(), scope.schoolGradeId(), scope.schoolClassId(),
                source.getLanguageId(), source.getFrameworkVersionId(),
                clonedVersion.getId(), source.getTargetFrameworkBandId(),
                source.getPassingScore(), source.getStrictness(), nextVersion,
                AssessmentPolicyStatus.DRAFT,
                command.effectiveFrom(), command.effectiveTo(),
                now, now, currentUserId, currentUserId);

        return assessmentPolicyRepository.save(cloned).getId();
    }

    /**
     * Bản mẫu đã khai Khối thì bản sao BẮT BUỘC giữ đúng khối đó -- cho trường đổi sang khối khác là
     * biến bản mẫu "Khối 10" thành chính sách của Khối 12 mà vẫn mang nguyên thông số soạn cho Khối
     * 10. Bản mẫu không khai khối thì trường phải tự chọn đúng một phạm vi.
     */
    private TargetScope resolveScope(CloneSystemAssessmentPolicyToSchoolCommand command, AssessmentPolicy source) {
        if (source.getGradeLevelId() != null) {
            if (command.gradeLevelId() != null || command.schoolGradeId() != null || command.schoolClassId() != null) {
                throw new IllegalArgumentException(
                        "Bản mẫu này đã gắn với một Khối cụ thể nên bản sao giữ nguyên khối đó; không nhận phạm vi khác.");
            }
            return new TargetScope(source.getGradeLevelId(), null, null);
        }

        int scopeCount = (command.gradeLevelId() != null ? 1 : 0)
                + (command.schoolGradeId() != null ? 1 : 0)
                + (command.schoolClassId() != null ? 1 : 0);
        if (scopeCount != 1) {
            throw new IllegalArgumentException(
                    "Bản mẫu không gắn Khối nào nên phải chọn đúng 1 phạm vi áp dụng: Lớp, Khối năm học, HOẶC Khối.");
        }

        if (command.schoolClassId() != null) {
            var schoolClass = schoolClassRepository.findById(command.schoolClassId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy Lớp học."));
            if (!command.schoolId().equals(schoolClass.getSchoolId())) {
                throw new ForbiddenException("Lớp học không thuộc trường của bạn.");
            }
            return new TargetScope(null, null, command.schoolClassId());
        }
        if (command.schoolGradeId() != null) {
            var schoolGrade = schoolGradeRepository.findById(command.schoolGradeId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy Khối năm học."));
            if (!command.schoolId().equals(schoolGrade.getSchoolId())) {
                throw new ForbiddenException("Khối năm học không thuộc trường của bạn.");
            }
            return new TargetScope(null, command.schoolGradeId(), null);
        }
        // Khối lớp là catalog dùng chung: chỉ cần tồn tại, không thuộc trường nào.
        if (gradeLevelRepository.findById(command.gradeLevelId()).isEmpty()) {
            throw new NotFoundException("Không tìm thấy Khối.");
        }
        return new TargetScope(command.gradeLevelId(), null, null);
    }

    private static String requireText(String raw, String messageWhenBlank) {
        var safe = raw == null ? null : StringNormalization.trimAndCollapseSpaces(raw);
        if (safe == null || safe.isBlank()) {
            throw new IllegalArgumentException(messageWhenBlank);
        }
        return safe;
    }
}
