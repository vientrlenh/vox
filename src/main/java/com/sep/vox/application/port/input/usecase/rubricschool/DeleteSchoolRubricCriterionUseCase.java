package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteSchoolRubricCriterionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeleteSchoolRubricCriterionUseCase implements IUseCase<DeleteSchoolRubricCriterionCommand, Void> {

    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;


    public DeleteSchoolRubricCriterionUseCase(RubricCriterionRepository rubricCriterionRepository, RubricVersionRepository rubricVersionRepository, RubricRepository rubricRepository, UserRepository userRepository, UserContextPort userContextPort, SchoolRepository schoolRepository) {
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.schoolRepository = schoolRepository;
    }

    @Override
    @Transactional
    public Void execute(DeleteSchoolRubricCriterionCommand command) {
        // 1. Auth
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");

        // 2. Check Version (DRAFT)
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Version."));

        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được xóa Tiêu chí khi phiên bản đang là bản Nháp (DRAFT).");
        }

        // 3. Check School Ownership
        Rubric rubric = rubricRepository.findById(version.getRubricId()).orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(command.schoolId()) || (currentUser.getSchoolId() != null && !currentUser.getSchoolId().equals(rubric.getSchoolId()))) {
            throw new ForbiddenException("Bạn không có quyền xóa dữ liệu của trường khác.");
        }

        // (Bổ sung) Kiểm tra xem trường học có đang hoạt động không
        var school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học."));

        if (!school.isActive()) {
            throw new ForbiddenException("Hành động bị từ chối: Trường học này đang bị vô hiệu hóa trên hệ thống.");
        }

        // 4. Lấy Criterion ra kiểm tra xem nó có đúng thuộc Version này không
        RubricCriterion criterion = rubricCriterionRepository.findById(command.criterionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Tiêu chí này."));

        if (!criterion.getRubricVersionId().equals(version.getId())) {
            throw new IllegalArgumentException("Tiêu chí này không thuộc về phiên bản Rubric đang chọn.");
        }

        // 5. Xóa cứng Tiêu chí
        rubricCriterionRepository.deleteById(criterion.getId());

        return null;
    }
}