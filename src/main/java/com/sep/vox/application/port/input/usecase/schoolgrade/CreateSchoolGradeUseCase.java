package com.sep.vox.application.port.input.usecase.schoolgrade;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateSchoolGradeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class CreateSchoolGradeUseCase implements IUseCase<CreateSchoolGradeCommand, UUID> {

    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public CreateSchoolGradeUseCase(SchoolGradeRepository schoolGradeRepository, SchoolRepository schoolRepository, UserRepository userRepository, UserContextPort userContextPort) {
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional // Giống hệt RegisterUseCase
    public UUID execute(CreateSchoolGradeCommand command) {

        // 1. Kiểm tra logic ngày tháng (Ràng buộc DB)
        if (!command.startDate().isBefore(command.endDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu (startDate) phải diễn ra trước ngày kết thúc (endDate).");
        }

        // 2. Validate User Context
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        if (currentUser.getSchoolId() != null && !currentUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền tạo năm học cho trường khác.");
        }

        // 3. Kiểm tra Trường học tồn tại
        if (schoolRepository.findById(command.schoolId()).isEmpty()) {
            throw new NotFoundException("Không tìm thấy trường học với ID đã cho.");
        }

        // 4. Kiểm tra mã Code Unique (Ràng buộc DB)
        String normalizedCode = StringNormalization.normalizeCode(command.code());
        if (schoolGradeRepository.existsBySchoolIdAndCode(command.schoolId(), normalizedCode)) {
            throw new DuplicatedException("Mã năm học này đã tồn tại trong trường.");
        }

        //
        OffsetDateTime now = OffsetDateTime.now();

        SchoolGrade newGrade = new SchoolGrade(
                command.schoolId(),
                normalizedCode,
                StringNormalization.trimAndCollapseSpaces(command.name()),
                command.description() != null ? StringNormalization.trimAndCollapseSpaces(command.description()) : null,
                command.startDate(),
                command.endDate(),
                SchoolGradeStatus.INACTIVE,
                now,
                now,
                currentUserId,
                currentUserId
        );

        SchoolGrade savedGrade = schoolGradeRepository.save(newGrade);

        return savedGrade.getId();
    }
}