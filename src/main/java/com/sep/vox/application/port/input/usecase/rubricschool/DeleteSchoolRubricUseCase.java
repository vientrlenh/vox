package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteSchoolRubricCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeleteSchoolRubricUseCase implements IUseCase<DeleteSchoolRubricCommand, Void> {

    private final RubricRepository rubricRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;

    public DeleteSchoolRubricUseCase(
            RubricRepository rubricRepository,
            RubricVersionRepository rubricVersionRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            SchoolRepository schoolRepository,
            SchoolUserRepository schoolUserRepository) {
        this.rubricRepository = rubricRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.schoolRepository = schoolRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional
    public Void execute(DeleteSchoolRubricCommand command) {
        // 1. Xác thực tài khoản
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        // 2. Lấy Rubric gốc
        Rubric rubric = rubricRepository.findById(command.rubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric này."));

        // 3. Chốt chặn an ninh: bằng SchoolUser, cùng pattern UpdateSchoolRubricUseCase
        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được liên kết với bất kỳ trường học nào."));

        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL ||
                !rubric.getSchoolId().equals(command.schoolId()) ||
                !schoolUser.getSchoolId().equals(rubric.getSchoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền xóa Rubric của trường khác.");
        }

        var school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Hành động bị từ chối: Trường học này đang bị vô hiệu hóa trên hệ thống.");
        }

        // 4. Chỉ cho xóa cứng khi Rubric chưa có version nào -- RubricCriterion/RubricResultBand luôn
        // là con của RubricVersion (không gắn thẳng vào Rubric), nên rỗng version đã đủ suy ra rỗng
        // cả criterion/result-band, không cần query thêm 2 bảng đó.
        if (!rubricVersionRepository.findByRubricId(rubric.getId()).isEmpty()) {
            throw new IllegalStateException(
                "Không thể xóa Rubric này vì đang có phiên bản (version) bên trong. "
                    + "Vui lòng xóa hết các phiên bản DRAFT hoặc Lưu trữ (Archive) các phiên bản đã PUBLISHED trước.");
        }

        rubricRepository.deleteById(rubric.getId());

        return null;
    }
}
