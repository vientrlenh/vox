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
    private final SchoolRepository schoolRepository; // BỔ SUNG

    public DeleteSchoolRubricResultBandUseCase(
            RubricResultBandRepository rubricResultBandRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            SchoolRepository schoolRepository) { // BỔ SUNG
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.schoolRepository = schoolRepository;
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

        // BỔ SUNG: Kiểm tra xem trường học có đang hoạt động không
        var school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Hành động bị từ chối: Trường học này đang bị vô hiệu hóa trên hệ thống.");
        }

        // 4. Lấy ResultBand ra kiểm tra
        RubricResultBand resultBand = rubricResultBandRepository.findById(command.resultBandId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Thang điểm kết quả này."));

        if (!resultBand.getRubricVersionId().equals(version.getId())) {
            throw new IllegalArgumentException("Thang điểm kết quả này không thuộc về phiên bản Rubric đang chọn.");
        }

        // 5. Xóa cứng Thang điểm (Không cần Cascade Delete vì không có FK reference trực tiếp ID của bảng này)
        rubricResultBandRepository.deleteById(resultBand.getId());

        return null;
    }
}