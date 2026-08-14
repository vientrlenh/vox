package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateSchoolRubricCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CreateSchoolRubricUseCase implements IUseCase<CreateSchoolRubricCommand, UUID> {

    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolRepository schoolRepository;
    private final SupportedLanguageRepository languageRepository;
    private final FrameworkRepository frameworkRepository;
    private final UserContextPort userContextPort;

    public CreateSchoolRubricUseCase(
            RubricRepository rubricRepository,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            SchoolRepository schoolRepository,
            SupportedLanguageRepository languageRepository,
            FrameworkRepository frameworkRepository,
            UserContextPort userContextPort) {
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.schoolRepository = schoolRepository;
        this.languageRepository = languageRepository;
        this.frameworkRepository = frameworkRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(CreateSchoolRubricCommand command) {
        // 1. Kiểm tra tài khoản School Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        // 2. Kiểm tra mối liên kết trường học chặt chẽ nhằm chống leak/cross-tenant data
        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được phân bổ vào trường học nào."));
        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không được quyền tạo Rubric cho một trường học khác.");
        }

        var school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin trường học."));
        if (!school.isActive()) {
            throw new IllegalStateException("Trường học đang bị khóa, không thể thực hiện thao tác này.");
        }

        // 3. Khảo sát ngôn ngữ và khung tiêu chuẩn
        if (!languageRepository.existsById(command.languageId())) {
            throw new NotFoundException("Ngôn ngữ chỉ định không tồn tại trên hệ thống.");
        }

        Framework framework = frameworkRepository.findById(command.frameworkId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Khung tiêu chuẩn (Framework)."));
        if (!framework.isActive()) {
            throw new IllegalStateException("Khung tiêu chuẩn (Framework) này đã bị vô hiệu hóa.");
        }

        String safeCode = StringNormalization.trimAndCollapseSpaces(command.code());
        String safeName = StringNormalization.trimAndCollapseSpaces(command.name());
        String safeDesc = command.description() != null ? StringNormalization.trimAndCollapseSpaces(command.description()) : null;

        // 4. Chặn trùng MÃ trong cùng phạm vi (trường + ngôn ngữ + khung năng lực).
        //
        // Bỏ 2026-08-14 luật cũ "mỗi trường chỉ một bộ tiêu chí cho mỗi ngôn ngữ". Luật đó ép khối
        // 10/11/12 phải dùng chung một bộ, nên khối chỉ còn cách mã hoá bằng SỐ HIỆU PHIÊN BẢN
        // (khối 10 = v1, khối 11 = v2, khối 12 = v3). Lúc đó `version` mất nghĩa "lần sửa đổi" và
        // không còn cách nào diễn tả "sang năm sửa lại tiêu chí của riêng khối 10".
        //
        // Nay mỗi khối có một BỘ riêng, phân biệt bằng mã (VD ENG-K10, ENG-K11), và mỗi bộ tự đánh
        // số phiên bản từ 1. Không có gì phía dưới dựa vào tính duy nhất cũ: chính sách đánh giá
        // trỏ thẳng rubricVersionId chứ không tra bộ theo trường+ngôn ngữ, và phía SYSTEM vốn đã
        // không có luật này (CreateSystemRubricUseCase). Ràng buộc thật nằm ở unique index
        // (owner_type, school_id, language_id, framework_id, code) -- kiểm ở đây để báo lỗi tử tế
        // thay vì để DB ném constraint violation thô.
        boolean isCodeTaken = rubricRepository.existsByOwnerTypeAndSchoolIdAndLanguageIdAndFrameworkIdAndCode(
                RubricOwnerType.SCHOOL.toString(), command.schoolId(), command.languageId(),
                command.frameworkId(), safeCode);
        if (isCodeTaken) {
            throw new DuplicatedException("Trường của bạn đã có một bộ tiêu chí đánh giá mang mã '"
                    + safeCode + "' cho ngôn ngữ và khung năng lực này. Hãy dùng mã khác"
                    + " (ví dụ đặt theo khối: ENG-K10, ENG-K11).");
        }

        // 5. Build thực thể Rubric gắn chặt schoolId
        Rubric newRubric = new Rubric(
                command.languageId(),
                command.frameworkId(),
                safeCode,
                safeName,
                safeDesc,
                RubricOwnerType.SCHOOL,
                command.schoolId()
        );

        Rubric savedRubric = rubricRepository.save(newRubric);
        return savedRubric.getId();
    }
}