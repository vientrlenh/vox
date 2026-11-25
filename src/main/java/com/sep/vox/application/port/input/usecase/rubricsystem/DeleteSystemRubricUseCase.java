package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricsystem.DeleteSystemRubricCommand;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteSystemRubricUseCase implements IUseCase<DeleteSystemRubricCommand, Void> {

    private final RubricRepository rubricRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricCriterionRepository rubricCriterionRepository; // Added
    private final RubricResultBandRepository rubricResultBandRepository; // Added
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(DeleteSystemRubricCommand command) {
        requireActiveCurrentUser();
        Rubric rubric = loadSystemOwnedRubric(command.rubricId());

        List<RubricVersion> versions = rubricVersionRepository.findByRubricId(rubric.getId());
        requireAllVersionsDraft(versions);

        // Every version is DRAFT (never used), so deleting the whole tree is safe.
        deleteVersionsWithChildren(versions);

        rubricRepository.deleteById(rubric.getId());

        return null;
    }

    private void requireActiveCurrentUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }
    }

    private Rubric loadSystemOwnedRubric(UUID rubricId) {
        Rubric rubric = rubricRepository.findById(rubricId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric hệ thống này."));

        // Hard rule: a System Admin must never delete a school's rubric.
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Hành động bị từ chối: System Admin không thể can thiệp vào Rubric của các trường học.");
        }
        return rubric;
    }

    private void requireAllVersionsDraft(List<RubricVersion> versions) {
        boolean hasLockedVersion = versions.stream()
                .anyMatch(v -> v.getStatus() != RubricStatus.DRAFT);

        if (hasLockedVersion) {
            throw new IllegalArgumentException("Không thể xóa Rubric này vì bên trong đang có phiên bản đã được Ban hành (PUBLISHED) hoặc Lưu trữ (ARCHIVED).");
        }
    }

    private void deleteVersionsWithChildren(List<RubricVersion> versions) {
        if (!versions.isEmpty()) {
            for (RubricVersion version : versions) {
                rubricCriterionRepository.deleteByRubricVersionId(version.getId());
                rubricResultBandRepository.deleteByRubricVersionId(version.getId());
                rubricVersionRepository.deleteById(version.getId());
            }
        }
    }
}
