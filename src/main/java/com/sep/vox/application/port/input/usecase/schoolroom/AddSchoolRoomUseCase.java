package com.sep.vox.application.port.input.usecase.schoolroom;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.AddSchoolRoomCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
@Service
@RequiredArgsConstructor
public class AddSchoolRoomUseCase implements IUseCase<AddSchoolRoomCommand, UUID> {

    private final SchoolRoomRepository schoolRoomRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UUID execute(AddSchoolRoomCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        // 1. Validate User & Permission (Guard Clause)
        validateUserAndAccess(currentUserId, command.schoolId());

        // 2. Kiểm tra sự tồn tại của trường
        if (!schoolRepository.existsById(command.schoolId())) {
            throw new NotFoundException("Không tìm thấy trường học.");
        }

        // 3. Chuẩn hóa & Kiểm tra trùng lặp
        String safeCode = StringNormalization.normalizeCode(command.code());
        if (schoolRoomRepository.existsBySchoolIdAndCode(command.schoolId(), safeCode)) {
            throw new DuplicatedException("Mã phòng học đã tồn tại trong trường.");
        }

        // 4. Lưu dữ liệu
        SchoolRoom savedRoom = saveRoom(command, safeCode, currentUserId);

        // 5. Trả về response
        return savedRoom.getId();
    }

    private void validateUserAndAccess(UUID userId, UUID targetSchoolId) {
        if (!userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc bị khóa.");
        }

        // Nếu là System Admin (không có SchoolUser), có thể cho qua.
        // Nếu là School Admin, phải check trùng schoolId.
        schoolUserRepository.findSchoolIdByUserId(userId).ifPresent(userSchoolId -> {
            if (!userSchoolId.equals(targetSchoolId)) {
                throw new ForbiddenException("Bạn không có quyền quản lý trường học này.");
            }
        });
    }

    private SchoolRoom saveRoom(AddSchoolRoomCommand command, String code, UUID creatorId) {
        SchoolRoom newRoom = SchoolRoom.builder()
            .schoolId(command.schoolId())
            .code(code)
            .name(StringNormalization.trimAndCollapseSpaces(command.name()))
            .description(StringNormalization.trimAndCollapseSpaces(command.description()))
            .active(true)
            .build();
        return schoolRoomRepository.save(newRoom);
    }
}