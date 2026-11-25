package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.CreateSchoolRubricCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateSchoolRubricUseCase implements IUseCase<CreateSchoolRubricCommand, UUID> {

    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolRepository schoolRepository;
    private final SupportedLanguageRepository languageRepository;
    private final FrameworkRepository frameworkRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(CreateSchoolRubricCommand command) {
        requireActiveSchoolMember(command);
        requireActiveLanguageAndFramework(command);

        String safeCode = StringNormalization.trimAndCollapseSpaces(command.code());
        String safeName = StringNormalization.trimAndCollapseSpaces(command.name());
        String safeDesc = command.description() != null ? StringNormalization.trimAndCollapseSpaces(command.description()) : null;

        requireCodeNotTaken(command, safeCode);

        Rubric newRubric = Rubric.forSchool(
                command.languageId(), command.frameworkId(), safeCode, safeName, safeDesc, command.schoolId());
        Rubric savedRubric = rubricRepository.save(newRubric);
        return savedRubric.getId();
    }

    private void requireActiveSchoolMember(CreateSchoolRubricCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        // Strict school membership check to prevent cross-tenant data leaks.
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được phân bổ vào trường học nào."));
        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không được quyền tạo Rubric cho một trường học khác.");
        }

        School school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin trường học."));
        if (!school.isActive()) {
            throw new IllegalArgumentException("Trường học đang bị khóa, không thể thực hiện thao tác này.");
        }
    }

    private void requireActiveLanguageAndFramework(CreateSchoolRubricCommand command) {
        if (!languageRepository.existsById(command.languageId())) {
            throw new ResourceNotFoundException("Ngôn ngữ chỉ định không tồn tại trên hệ thống.");
        }
        Framework framework = frameworkRepository.findById(command.frameworkId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Khung tiêu chuẩn (Framework)."));
        if (!framework.isActive()) {
            throw new IllegalArgumentException("Khung tiêu chuẩn (Framework) này đã bị vô hiệu hóa.");
        }
    }

    /**
     * Rejects a duplicate CODE within the same scope (school + language + framework).
     *
     * <p>Removed 2026-08-14: the old rule "a school has only one rubric per language". That rule forced
     * grades 10/11/12 to share one rubric, so a grade could only be encoded as a VERSION NUMBER (grade 10 =
     * v1, grade 11 = v2, grade 12 = v3). Then {@code version} lost its meaning of "a revision" and there was
     * no way to say "next year grade 10's own criteria are revised".
     *
     * <p>Now every grade has its own rubric, told apart by code (e.g. ENG-K10, ENG-K11), and each rubric
     * numbers its versions from 1. Nothing below relied on the old uniqueness: assessment policies point
     * straight at rubricVersionId instead of looking up a rubric by school + language, and the SYSTEM side
     * never had this rule (CreateSystemRubricUseCase). The real constraint is the unique index
     * (owner_type, school_id, language_id, framework_id, code); it is checked here to report a proper
     * error instead of letting the database throw a raw constraint violation.
     */
    private void requireCodeNotTaken(CreateSchoolRubricCommand command, String safeCode) {
        boolean isCodeTaken = rubricRepository.existsByOwnerTypeAndSchoolIdAndLanguageIdAndFrameworkIdAndCode(
                RubricOwnerType.SCHOOL.toString(), command.schoolId(), command.languageId(),
                command.frameworkId(), safeCode);
        if (isCodeTaken) {
            throw new ResourceDuplicatedException("Trường của bạn đã có một bộ tiêu chí đánh giá mang mã '"
                    + safeCode + "' cho ngôn ngữ và khung năng lực này. Hãy dùng mã khác"
                    + " (ví dụ đặt theo khối: ENG-K10, ENG-K11).");
        }
    }
}