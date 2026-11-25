package com.sep.vox.application.port.input.usecase.school;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.school.DeleteSchoolCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteSchoolUseCase implements IUseCase<DeleteSchoolCommand, UUID> {

    private final SchoolRepository schoolRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRoomRepository schoolRoomRepository;

    // Remember to inject the Grade and Class repositories here as well

    @Override
    @Transactional // Required so the data is locked
    public UUID execute(DeleteSchoolCommand command) {

        // Lock the school data (avoids a clash when someone is updating it)
        School school = schoolRepository.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trường học với ID đã cho."));

        requireActiveCurrentUser();

        if (!school.isActive()) {
            throw new IllegalArgumentException("Trường học này đã bị vô hiệu hóa (xóa) từ trước rồi.");
        }

        rejectWhenSchoolHasActiveDependencies(school);

        school.setActive(false);

        schoolRepository.save(school);

        return school.getId();
    }

    private void requireActiveCurrentUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản của bạn."));

        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }
    }

    private void rejectWhenSchoolHasActiveDependencies(School school) {
        boolean hasActiveRooms = schoolRoomRepository.existsBySchoolIdAndIsActive(school.getId(), true);
        if (hasActiveRooms) {
            throw new IllegalArgumentException("Không thể xóa. Trường này vẫn còn Phòng học đang hoạt động.");
        }

        boolean hasActiveGrades = schoolGradeRepository.existsBySchoolIdAndStatus(school.getId(), SchoolGradeStatus.ACTIVE.name());
        if (hasActiveGrades) {
            throw new IllegalArgumentException("Không thể xóa. Trường này vẫn còn Khối/Năm học đang hoạt động.");
        }

        boolean hasActiveClasses = schoolClassRepository.existsBySchoolIdAndStatus(school.getId(), SchoolClassStatus.ACTIVE.name());
        if (hasActiveClasses) {
            throw new IllegalArgumentException("Không thể xóa. Trường này vẫn còn Lớp học đang hoạt động.");
        }
    }
}