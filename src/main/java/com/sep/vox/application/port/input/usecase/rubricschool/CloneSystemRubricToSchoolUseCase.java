package com.sep.vox.application.port.input.usecase.rubricschool;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CloneSystemRubricToSchoolCommand;
import com.sep.vox.application.port.input.command.CreateAssessmentPolicyCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.CreateSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.service.rubric.RubricCloneService;

/**
 * Trường sao một bản mẫu đã ban hành của hệ thống về làm rubric riêng, kèm luôn các chính sách chấm
 * mẫu gắn với phiên bản đó.
 *
 * <p>Vì sao phải sao chứ không dùng thẳng bản của hệ thống: {@code CreateSchoolAssessmentPolicyUseCase}
 * từ chối gắn chính sách của trường vào phiên bản rubric không thuộc trường đó. Nên sao chép không
 * phải tối ưu hoá cho tiện, nó là con đường duy nhất để một bản mẫu đi vào được việc chấm bài.
 *
 * <h2>Vì sao chính sách nằm chung ở đây</h2>
 *
 * <p>Bản sao rubric ra ở trạng thái DRAFT, mà một phiên bản DRAFT chưa ban hành được nếu chưa có
 * chính sách nào liên kết đã PUBLISHED ({@code ChangeSchoolRubricVersionStatusUseCase}). Nghĩa là
 * "sao rubric" luôn kéo theo "tạo chính sách" ngay sau đó -- gộp vào một bước là mô tả đúng thứ tự
 * bắt buộc chứ không phải gói tiện ích. Đường ngược lại (sao chính sách rồi tự nhân bản rubric) đã
 * bị gỡ vì nó tạo ra bản sao rubric thứ hai mỗi lần trường muốn thêm một chính sách.
 *
 * <p>Việc tạo chính sách KHÔNG viết lại ở đây mà uỷ cho {@code CreateSchoolAssessmentPolicyUseCase}:
 * mọi luật (khung còn PUBLISHED, bậc mục tiêu thuộc đúng khung và không vượt trần của khối, đúng một
 * phạm vi, mỗi phạm vi một chính sách, đánh số version theo phạm vi) đã nằm trọn ở đó. Nhân bản
 * chúng sang đây là tạo ra hai bộ luật sẽ trôi lệch nhau.
 *
 * <p>Cả hai use case đều {@code @Transactional} nên lời gọi bên trong nhập vào cùng giao dịch: một
 * chính sách hỏng validate sẽ cuộn lại cả bản sao rubric, không để lại rubric mồ côi.
 */
@Service
public class CloneSystemRubricToSchoolUseCase implements IUseCase<CloneSystemRubricToSchoolCommand, UUID> {

    private final RubricRepository rubricRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricCloneService rubricCloneService;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final CreateSchoolAssessmentPolicyUseCase createSchoolAssessmentPolicyUseCase;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public CloneSystemRubricToSchoolUseCase(
            RubricRepository rubricRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricCriterionRepository rubricCriterionRepository,
            RubricCloneService rubricCloneService,
            AssessmentPolicyRepository assessmentPolicyRepository,
            CreateSchoolAssessmentPolicyUseCase createSchoolAssessmentPolicyUseCase,
            SchoolUserRepository schoolUserRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.rubricRepository = rubricRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricCloneService = rubricCloneService;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.createSchoolAssessmentPolicyUseCase = createSchoolAssessmentPolicyUseCase;
        this.schoolUserRepository = schoolUserRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(CloneSystemRubricToSchoolCommand command) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new UnauthorizedException("Lỗi tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản bị khóa.");
        }

        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
            .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được liên kết với bất kỳ trường học nào."));
        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền sao chép về trường khác.");
        }

        var sourceVersion = rubricVersionRepository.findById(command.sourceRubricVersionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Rubric mẫu."));
        var sourceRubric = rubricRepository.findById(sourceVersion.getRubricId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric gốc của bản mẫu."));

        if (sourceRubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Chỉ được sao chép từ bộ tiêu chí mẫu của hệ thống.");
        }
        // Chỉ nhận bản đã ban hành: bản nháp của hệ thống là việc đang làm dở, chưa được rà soát, và
        // không nên lọt ra ngoài phạm vi system admin.
        if (sourceVersion.getStatus() != RubricStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ được sao chép từ phiên bản mẫu đã được ban hành (PUBLISHED).");
        }
        // Sao một bản rỗng sẽ tạo ra phiên bản mà trường không bao giờ ban hành được (cửa "phải có
        // tiêu chí" chặn lại), và họ chỉ phát hiện ra ở bước cuối cùng.
        if (rubricCriterionRepository.findByRubricVersionId(sourceVersion.getId()).isEmpty()) {
            throw new IllegalStateException("Bản mẫu này chưa có tiêu chí nào nên không sao chép được.");
        }

        // Cùng cách chuẩn hoá với CreateSchoolRubricUseCase: hai đường cùng ghi vào một ràng buộc
        // unique, nên chúng phải hiểu "mã trùng nhau" theo đúng một nghĩa.
        var safeCode = StringNormalization.trimAndCollapseSpaces(command.code());
        var safeName = StringNormalization.trimAndCollapseSpaces(command.name());
        var safeDescription = command.description() == null
            ? null
            : StringNormalization.trimAndCollapseSpaces(command.description());
        if (safeCode == null || safeCode.isBlank()) {
            throw new IllegalArgumentException("Mã bộ tiêu chí không được để trống.");
        }
        if (safeName == null || safeName.isBlank()) {
            throw new IllegalArgumentException("Tên bộ tiêu chí không được để trống.");
        }

        // Kiểm sớm để báo lỗi đọc được, thay vì để ràng buộc unique của DB ném ra một thông báo mà
        // người dùng không hiểu phải sửa gì.
        var isCodeTaken = rubricRepository.existsByOwnerTypeAndSchoolIdAndLanguageIdAndFrameworkIdAndCode(
            RubricOwnerType.SCHOOL.toString(),
            command.schoolId(),
            sourceRubric.getLanguageId(),
            sourceRubric.getFrameworkId(),
            safeCode);
        if (isCodeTaken) {
            throw new DuplicatedException("Trường của bạn đã có một bộ tiêu chí mang mã '" + safeCode
                + "' cho ngôn ngữ và khung năng lực này. Hãy dùng mã khác (ví dụ đặt theo khối:"
                + " ENG-K10, ENG-K11).");
        }

        var targetMethod = command.totalScoreMethod() == null
            ? sourceVersion.getTotalScoreMethod()
            : command.totalScoreMethod();

        var clonedVersion = rubricCloneService.cloneToSchoolAsDraft(
            sourceRubric,
            sourceVersion,
            command.schoolId(),
            safeCode,
            safeName,
            safeDescription,
            targetMethod,
            currentUserId);

        createPoliciesFromTemplates(command, sourceVersion.getId(), clonedVersion.getId());

        return clonedVersion.getId();
    }

    /**
     * Dựng chính sách của trường từ các chính sách mẫu mà trường đã chọn, trỏ vào bản sao rubric vừa
     * tạo.
     *
     * <p>Chỉ kiểm hai thứ mà {@code CreateSchoolAssessmentPolicyUseCase} không thể tự biết: bản mẫu
     * có hợp lệ và có thuộc đúng phiên bản đang sao hay không, và phạm vi có tuân luật kế thừa Khối
     * hay không. Mọi luật còn lại để nguyên bên kia.
     */
    private void createPoliciesFromTemplates(
            CloneSystemRubricToSchoolCommand command, UUID sourceVersionId, UUID clonedVersionId) {

        var requested = command.policies();
        if (requested == null || requested.isEmpty()) {
            // Sao bộ tiêu chí "trần": hợp lệ, nhưng phiên bản này sẽ nằm DRAFT tới khi trường tự gắn
            // một chính sách cho nó.
            return;
        }

        // Chỉ nạp một lần rồi tra trong bộ nhớ: danh sách chính sách mẫu của một phiên bản luôn nhỏ
        // (hiện là 1 bản mỗi phiên bản), và làm thế thì phép kiểm "thuộc đúng phiên bản đang sao"
        // không cần thêm truy vấn nào.
        List<AssessmentPolicy> templates = assessmentPolicyRepository
                .findPublishedSystemWideByRubricVersionId(sourceVersionId);

        var policyCommands = requested.stream()
                .map(choice -> toCreateCommand(command, choice, templates, clonedVersionId))
                .toList();

        createSchoolAssessmentPolicyUseCase.execute(policyCommands);
    }

    private CreateAssessmentPolicyCommand toCreateCommand(
            CloneSystemRubricToSchoolCommand command,
            CloneSystemRubricToSchoolCommand.PolicyToClone choice,
            List<AssessmentPolicy> templates,
            UUID clonedVersionId) {

        if (choice.sourcePolicyId() == null) {
            throw new IllegalArgumentException("Thiếu id chính sách mẫu cần sao.");
        }

        // Tra trong danh sách của ĐÚNG phiên bản đang sao, nên tự nó đã chặn việc gửi lên id của một
        // chính sách mẫu thuộc phiên bản khác -- bản sao sẽ mang thông số soạn cho bộ tiêu chí khác.
        var template = templates.stream()
                .filter(candidate -> candidate.getId().equals(choice.sourcePolicyId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy chính sách mẫu đã ban hành nào thuộc phiên bản bộ tiêu chí này."));
        if (template.getStatus() != AssessmentPolicyStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ được sao từ chính sách mẫu đã được ban hành (PUBLISHED).");
        }

        var scope = resolveScope(template, choice);

        return new CreateAssessmentPolicyCommand(
                command.schoolId(),
                template.getFrameworkVersionId(),
                clonedVersionId,
                template.getLanguageId(),
                scope.gradeLevelId(),
                scope.schoolGradeId(),
                scope.schoolClassId(),
                template.getTargetFrameworkBandId(),
                template.getPassingScore(),
                template.getStrictness(),
                choice.effectiveFrom(),
                choice.effectiveTo());
    }

    /**
     * Bản mẫu đã khai Khối thì bản sao BẮT BUỘC giữ đúng khối đó -- cho trường đổi sang khối khác là
     * biến bản mẫu "Khối 10" thành chính sách của Khối 12 mà vẫn mang nguyên thông số soạn cho Khối
     * 10. Bản mẫu không khai khối thì trường tự chọn, và phép kiểm "đúng một phạm vi" nằm ở
     * {@code CreateSchoolAssessmentPolicyUseCase} nên không lặp lại ở đây.
     */
    private TargetScope resolveScope(
            AssessmentPolicy template, CloneSystemRubricToSchoolCommand.PolicyToClone choice) {

        if (template.getGradeLevelId() != null) {
            if (choice.gradeLevelId() != null || choice.schoolGradeId() != null || choice.schoolClassId() != null) {
                throw new IllegalArgumentException(
                        "Chính sách mẫu này đã gắn với một Khối cụ thể nên bản sao giữ nguyên khối đó;"
                                + " không nhận phạm vi khác.");
            }
            return new TargetScope(template.getGradeLevelId(), null, null);
        }
        return new TargetScope(choice.gradeLevelId(), choice.schoolGradeId(), choice.schoolClassId());
    }

    /** Phạm vi bản sao sẽ áp dụng, sau khi gộp phạm vi của bản mẫu với lựa chọn của trường. */
    private record TargetScope(UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId) {}
}
