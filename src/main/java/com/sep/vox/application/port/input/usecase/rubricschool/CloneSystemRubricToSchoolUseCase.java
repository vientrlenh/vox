package com.sep.vox.application.port.input.usecase.rubricschool;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CloneSystemRubricToSchoolCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.service.rubric.RubricCloneService;

/**
 * Trường sao một bản mẫu đã ban hành của hệ thống về làm rubric riêng.
 *
 * <p>Vì sao phải sao chứ không dùng thẳng bản của hệ thống: {@code CreateSchoolAssessmentPolicyUseCase}
 * từ chối gắn chính sách của trường vào phiên bản rubric không thuộc trường đó. Nên sao chép không
 * phải tối ưu hoá cho tiện, nó là con đường duy nhất để một bản mẫu đi vào được việc chấm bài.
 *
 * <p>Bản sao ra ở trạng thái DRAFT, và luồng tiếp theo giống hệt khi trường tự soạn: gắn chính sách
 * cho phiên bản DRAFT này -> ban hành chính sách -> ban hành phiên bản rubric.
 */
@Service
public class CloneSystemRubricToSchoolUseCase implements IUseCase<CloneSystemRubricToSchoolCommand, UUID> {

    private final RubricRepository rubricRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricCloneService rubricCloneService;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public CloneSystemRubricToSchoolUseCase(
            RubricRepository rubricRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricCriterionRepository rubricCriterionRepository,
            RubricCloneService rubricCloneService,
            SchoolUserRepository schoolUserRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.rubricRepository = rubricRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricCloneService = rubricCloneService;
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

        return clonedVersion.getId();
    }
}
