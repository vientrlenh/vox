package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.UpdateSchoolRubricCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateSchoolRubricUseCase implements IUseCase<UpdateSchoolRubricCommand, UUID> {

    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public UUID execute(UpdateSchoolRubricCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        requireActiveUser(currentUserId);
        requireOwnedBySchoolAdmin(command, currentUserId);

        String finalName = normalizeName(command.name());
        // A blank description means keep the stored one (the SQL leaves it untouched); an explicit null clears it.
        // Same rule as UpdateSchoolRubricCriterionUseCase/UpdateSchoolRubricResultBandUseCase.
        boolean descriptionBlank = command.description() != null && command.description().isBlank();
        boolean descriptionProvided = command.descriptionProvided() && !descriptionBlank;
        String finalDesc = (command.description() != null && !descriptionBlank)
                ? StringNormalization.trimAndCollapseSpaces(command.description())
                : null;

        rubricRepository.updateRubricAtomic(
                command.rubricId(),
                finalName,
                finalDesc,
                descriptionProvided
        );

        return command.rubricId();
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String finalName = StringNormalization.trimAndCollapseSpaces(name);
        if (finalName.isBlank()) {
            throw new IllegalArgumentException("Tên Rubric không hợp lệ hoặc bị để trống.");
        }
        return finalName;
    }

    private void requireActiveUser(UUID currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản bị khóa.");
        }
    }

    private void requireOwnedBySchoolAdmin(UpdateSchoolRubricCommand command, UUID currentUserId) {
        Rubric rubric = rubricRepository.findById(command.rubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric này."));

        // Security checkpoint: ownership is checked through SchoolUser.
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được liên kết với bất kỳ trường học nào."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL ||
                !rubric.getSchoolId().equals(command.schoolId()) ||
                !schoolUser.getSchoolId().equals(rubric.getSchoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền chỉnh sửa Rubric của trường khác.");
        }

        School school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Hành động bị từ chối: Trường học này đang bị vô hiệu hóa trên hệ thống.");
        }
    }
}