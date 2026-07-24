package com.sep.vox.application.port.input.usecase.schooluser;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class DeleteSchoolUserUseCase implements IUseCase<DeleteSchoolUserCommand, Void> {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;

    public DeleteSchoolUserUseCase(UserContextPort userContextPort, UserRepository userRepository, SchoolUserRepository schoolUserRepository) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional
    public Void execute(DeleteSchoolUserCommand input) {
        var now = OffsetDateTime.now();
        var callerId = userContextPort.getCurrentAuthenticatedUserId();

        userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        var callerSchoolUser = schoolUserRepository.findByUserId(callerId)
            .orElseThrow(() -> new IllegalArgumentException("Không có quyền thực hiện thao tác này"));
        if (!input.schoolId().equals(callerSchoolUser.getSchoolId())) {
            throw new IllegalArgumentException("Không có quyền thực hiện thao tác này");
        }
        if (callerId.equals(input.userId())) {
            throw new IllegalArgumentException("Không thể tự xóa tài khoản của chính mình");
        }

        var targetUser = userRepository.findById(input.userId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        var targetSchoolUser = schoolUserRepository.findByUserId(input.userId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        if (!input.schoolId().equals(targetSchoolUser.getSchoolId())) {
            throw new NotFoundException("Không tìm thấy người dùng");
        }

        targetUser.softDelete(callerId, now);
        userRepository.save(targetUser);

        // SchoolUser không có cờ isActive: kết thúc membership bằng endDate để vô hiệu hóa,
        // đảm bảo ràng buộc DB start_date < end_date luôn thỏa mãn.
        var startDate = targetSchoolUser.getStartDate();
        var membershipEnd = (startDate != null && !startDate.isBefore(now))
            ? startDate.plusSeconds(1)
            : now;
        targetSchoolUser.setEndDate(membershipEnd);
        schoolUserRepository.save(targetSchoolUser);

        return null;
    }
}
