package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricsystem.DeleteSystemRubricVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteSystemRubricVersionUseCase implements IUseCase<DeleteSystemRubricVersionCommand, Void> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(DeleteSystemRubricVersionCommand command) {
        requireActiveCurrentUser();
        RubricVersion version = loadDraftVersionOfSystemRubric(command.versionId());

        rubricCriterionRepository.deleteByRubricVersionId(version.getId());
        rubricResultBandRepository.deleteByRubricVersionId(version.getId());
        requireNoAssessmentPolicyUsing(version);

        rubricVersionRepository.deleteById(version.getId());
        return null;
    }

    private void requireActiveCurrentUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");
    }

    private RubricVersion loadDraftVersionOfSystemRubric(UUID versionId) {
        RubricVersion version = rubricVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản."));

        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Rubric."));

        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Không thể can thiệp vào phiên bản của trường học.");
        }

        // Hard delete is only allowed for DRAFT; other statuses use the separate Archive function.
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ có thể xóa phiên bản đang ở trạng thái DRAFT. Vui lòng dùng chức năng Lưu trữ (Archive) nếu phiên bản đã PUBLISHED.");
        }
        return version;
    }

    // Since V44 one rubric version is shared by many policies, so a wrong delete no longer leaves a single
    // policy dangling but a whole group. The missing guard was already wrong before (no foreign key watches
    // it); the consequence is now bigger, so the delete is blocked outright.
    private void requireNoAssessmentPolicyUsing(RubricVersion version) {
        if (assessmentPolicyRepository.existsPublishedByRubricVersionId(version.getId())
                || assessmentPolicyRepository.existsNotPublishedByRubricVersionId(version.getId())) {
            throw new IllegalArgumentException(
                    "Không thể xóa phiên bản này vì đang có Assessment Policy sử dụng. Hãy xóa hoặc lưu trữ các chính sách đó trước.");
        }
    }
}
