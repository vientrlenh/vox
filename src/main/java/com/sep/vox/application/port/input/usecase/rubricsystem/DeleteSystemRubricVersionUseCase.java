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

import java.util.UUID;

@Service
public class DeleteSystemRubricVersionUseCase implements IUseCase<DeleteSystemRubricVersionCommand, Void> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public DeleteSystemRubricVersionUseCase(AssessmentPolicyRepository assessmentPolicyRepository, RubricVersionRepository rubricVersionRepository, RubricRepository rubricRepository, RubricCriterionRepository rubricCriterionRepository, RubricResultBandRepository rubricResultBandRepository, UserRepository userRepository, UserContextPort userContextPort) {
        this.assessmentPolicyRepository = assessmentPolicyRepository;
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

        // Chỉ được xóa cứng khi đang DRAFT, các trạng thái khác dùng chức năng Lưu trữ (Archive) riêng
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể xóa phiên bản đang ở trạng thái DRAFT. Vui lòng dùng chức năng Lưu trữ (Archive) nếu phiên bản đã PUBLISHED.");
        }

        rubricCriterionRepository.deleteByRubricVersionId(version.getId());
        rubricResultBandRepository.deleteByRubricVersionId(version.getId());
        // 5. Không xóa khi còn Assessment Policy trỏ vào.
        //
        // Từ V44 một phiên bản rubric dùng chung được cho nhiều chính sách, nên xóa nhầm không còn
        // chỉ làm treo một chính sách mà treo cả cụm. Bản thân việc thiếu chốt này đã sai từ trước
        // (không có khóa ngoại nào canh), chỉ là hậu quả nay lớn hơn nên phải chặn hẳn.
        if (assessmentPolicyRepository.existsByRubricVersionId(version.getId())) {
            throw new IllegalStateException(
                    "Không thể xóa phiên bản này vì đang có Assessment Policy sử dụng. Hãy xóa hoặc lưu trữ các chính sách đó trước.");
        }

        rubricVersionRepository.deleteById(version.getId());
        return null;
    }
}