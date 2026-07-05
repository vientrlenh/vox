package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteSystemRubricVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DeleteSystemRubricVersionUseCase implements IUseCase<DeleteSystemRubricVersionCommand, Void> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public DeleteSystemRubricVersionUseCase(RubricVersionRepository rubricVersionRepository, RubricRepository rubricRepository, RubricCriterionRepository rubricCriterionRepository, RubricResultBandRepository rubricResultBandRepository, UserRepository userRepository, UserContextPort userContextPort) {
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteSystemRubricVersionCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");

        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản."));

        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric."));

        // CHECK QUYỀN SYSTEM
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Không thể can thiệp vào phiên bản của trường học.");
        }

        // Delete CadeCache
        if (version.getStatus() == RubricStatus.DRAFT) {
            rubricCriterionRepository.deleteByRubricVersionId(version.getId());
            rubricResultBandRepository.deleteByRubricVersionId(version.getId());
            rubricVersionRepository.deleteById(version.getId());
        } else if (version.getStatus() == RubricStatus.PUBLISHED) {
            version.setStatus(RubricStatus.ARCHIVED);
            OffsetDateTime now = OffsetDateTime.now();
            // Không cho effectiveTo lùi về trước effectiveFrom nếu version chưa tới ngày hiệu lực
            version.setEffectiveTo(now.isBefore(version.getEffectiveFrom()) ? version.getEffectiveFrom() : now);
            version.setUpdatedAt(now);
            version.setUpdatedBy(currentUserId);
            rubricVersionRepository.save(version);
        } else {
            throw new IllegalStateException("Phiên bản đã bị lưu trữ từ trước.");
        }
        return null;
    }
}