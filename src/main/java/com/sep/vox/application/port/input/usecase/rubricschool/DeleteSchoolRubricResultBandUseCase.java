package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteSchoolRubricResultBandCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.*;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeleteSchoolRubricResultBandUseCase implements IUseCase<DeleteSchoolRubricResultBandCommand, Void> {

    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public DeleteSchoolRubricResultBandUseCase(
            RubricResultBandRepository rubricResultBandRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteSchoolRubricResultBandCommand command) {
        // 1. Auth
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản bị khóa.");
        }

        // 2. Check Version (DRAFT)
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Rubric."));
        if (version.getStatus() != RubricStatus.DRAFT) {
            // FIX: Đổi chữ "Tiêu chí" -> "Thang điểm"
            throw new IllegalStateException("Chỉ được xóa Thang điểm khi phiên bản đang là bản Nháp (DRAFT).");
        }

        // 3. Check School Ownership
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL ||
                !rubric.getSchoolId().equals(command.schoolId()) ||
                (currentUser.getSchoolId() != null && !currentUser.getSchoolId().equals(rubric.getSchoolId()))) {
            throw new ForbiddenException("Bạn không có quyền xóa dữ liệu của trường khác.");
        }

        // 4. Lấy ResultBand ra kiểm tra (Đã đổi tên biến thành resultBand)
        RubricResultBand resultBand = rubricResultBandRepository.findById(command.resultBandId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Thang điểm kết quả này."));

        if (!resultBand.getRubricVersionId().equals(version.getId())) {
            throw new IllegalArgumentException("Thang điểm kết quả này không thuộc về phiên bản Rubric đang chọn.");
        }

        // 5. FIX: Gọi ĐÚNG Repository để xóa cứng Thang điểm
        rubricResultBandRepository.deleteById(resultBand.getId());

        return null;
    }
}