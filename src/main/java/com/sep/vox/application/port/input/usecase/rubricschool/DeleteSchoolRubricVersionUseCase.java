package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteSchoolRubricVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DeleteSchoolRubricVersionUseCase implements IUseCase<DeleteSchoolRubricVersionCommand, Void> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;       // THÊM MỚI
    private final RubricResultBandRepository rubricResultBandRepository;     // THÊM MỚI
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public DeleteSchoolRubricVersionUseCase(
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            RubricCriterionRepository rubricCriterionRepository,
            RubricResultBandRepository rubricResultBandRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteSchoolRubricVersionCommand command) {
        // 1. Xác thực tài khoản
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        // 2. Lấy thông tin phiên bản
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Rubric này."));

        // 3. Kiểm tra quyền sở hữu
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));

        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL ||
                !rubric.getSchoolId().equals(command.schoolId()) ||
                (currentUser.getSchoolId() != null && !currentUser.getSchoolId().equals(rubric.getSchoolId()))) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền can thiệp vào phiên bản của trường khác.");
        }


        // 4. LOGIC XỬ LÝ (DRAFT vs PUBLISHED)
        if (version.getStatus() == RubricStatus.DRAFT) {

            rubricCriterionRepository.deleteByRubricVersionId(version.getId());
            rubricResultBandRepository.deleteByRubricVersionId(version.getId());
            rubricVersionRepository.deleteById(version.getId());

        } else if (version.getStatus() == RubricStatus.PUBLISHED) {
            // Xóa mềm (Archive)
            version.setStatus(RubricStatus.ARCHIVED);

            OffsetDateTime now = OffsetDateTime.now();
            version.setEffectiveTo(now);
            version.setUpdatedAt(now);
            version.setUpdatedBy(currentUserId);

            rubricVersionRepository.save(version);
        } else {
            throw new IllegalStateException("Thao tác thất bại. Phiên bản này đã bị đưa vào Lưu trữ (ARCHIVED) từ trước.");
        }

        return null;
    }
}