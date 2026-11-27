package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricsystem.ChangeSystemRubricVersionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.rubric.*;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import com.sep.vox.domain.service.rubric.RubricScoringConsistencyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChangeSystemRubricVersionStatusUseCase implements IUseCase<ChangeSystemRubricVersionStatusCommand, UUID> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final FrameworkRepository frameworkRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(ChangeSystemRubricVersionStatusCommand command) {
        requireActiveCurrentUser();

        RubricVersion version = loadVersionToChange(command);
        Rubric rubric = loadSystemOwnedRubric(version);
        requireTransitionAllowed(command.status(), version, rubric);

        version.setStatus(command.status());

        rubricVersionRepository.save(version);
        return version.getId();
    }

    private void requireActiveCurrentUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Lỗi tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");
    }

    private RubricVersion loadVersionToChange(ChangeSystemRubricVersionStatusCommand command) {
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản Rubric."));

        if (version.getStatus() == command.status()) {
            throw new IllegalArgumentException("Phiên bản đã đang ở trạng thái " + command.status() + ".");
        }
        return version;
    }

    private Rubric loadSystemOwnedRubric(RubricVersion version) {
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Hành động bị từ chối: Không thể chuyển trạng thái Rubric thuộc về Trường học (School).");
        }
        return rubric;
    }

    private void requireTransitionAllowed(RubricStatus targetStatus, RubricVersion version, Rubric rubric) {
        if (targetStatus == RubricStatus.PUBLISHED) {
            requirePublishable(version, rubric);
        } else if (targetStatus == RubricStatus.ARCHIVED) {
            throw new IllegalArgumentException("Hành động bị từ chối: Vui lòng dùng chức năng Lưu trữ (Archive) riêng để chuyển phiên bản sang ARCHIVED.");
        } else if (targetStatus == RubricStatus.DRAFT) {
            // Block the loophole of moving a version back to an earlier status
            throw new IllegalArgumentException("Hành động bị từ chối: Không thể chuyển một phiên bản đã Ban hành/Lưu trữ quay ngược lại trạng thái Nháp (DRAFT). Vui lòng tạo phiên bản mới.");
        }
    }

    private void requirePublishable(RubricVersion version, Rubric rubric) {
        requireDraftWithCriteria(version);
        requireActiveFramework(rubric);

        RubricScoringConsistencyValidator.assertPublishable(
                version.getTotalScoreMethod(),
                version.getScoringScaleMin(),
                version.getScoringScaleMax(),
                rubricCriterionRepository.findByRubricVersionId(version.getId()));
    }

    private void requireDraftWithCriteria(RubricVersion version) {
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ có thể ban hành (PUBLISH) phiên bản đang ở trạng thái Nháp (DRAFT).");
        }

        // A version without criteria or a scoring scale cannot grade anything: once published it would be
        // useless and could no longer be edited (only archived).
        if (rubricCriterionRepository.findByRubricVersionId(version.getId()).isEmpty()) {
            throw new IllegalArgumentException("Không thể ban hành phiên bản này vì chưa có tiêu chí (Criterion) nào.");
        }
    }

    private void requireActiveFramework(Rubric rubric) {
        Framework framework = frameworkRepository.findById(rubric.getFrameworkId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Khung tiêu chuẩn (Framework) liên kết."));
        if (!framework.isActive()) {
            throw new IllegalArgumentException("Không thể ban hành Rubric này vì Khung tiêu chuẩn (Framework) gốc đang bị vô hiệu hóa.");
        }
    }
}
