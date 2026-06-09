package com.sep.vox.application.port.input.usecase.schoolclassuser;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateSchoolClassUserStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schoolclassuser.UpdateSchoolClassUserStatusResponse;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class UpdateSchoolClassUserStatusUseCase implements IUseCase<UpdateSchoolClassUserStatusCommand, UpdateSchoolClassUserStatusResponse> {

    private final SchoolClassUserRepository schoolClassUserRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public UpdateSchoolClassUserStatusUseCase(
            SchoolClassUserRepository schoolClassUserRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UpdateSchoolClassUserStatusResponse execute(UpdateSchoolClassUserStatusCommand input) {
        validateCommand(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = findCurrentUser(currentUserId);
        var schoolId = getSchoolId(currentUser);

        validateRequestedSchool(input.schoolId(), schoolId);
        validateSchool(schoolId);
        validateSchoolClass(input.classId(), schoolId);
        validateTargetUser(input.userId(), schoolId);
        var membership = findMembership(input.userId(), input.classId());

        if (input.isActive()) {
            activate(membership);
        } else {
            deactivate(membership);
        }

        return new UpdateSchoolClassUserStatusResponse(input.classId());
    }

    private void validateCommand(UpdateSchoolClassUserStatusCommand input) {
        if (input.schoolId() == null) {
            throw new IllegalArgumentException("Trường học không được để trống");
        }
        if (input.classId() == null) {
            throw new IllegalArgumentException("Lớp học không được để trống");
        }
        if (input.userId() == null) {
            throw new IllegalArgumentException("Người dùng không được để trống");
        }
    }

    private void activate(SchoolClassUser membership) {
        if (!membership.isActive() || membership.getLeftAt() != null) {
            membership.activate();
            schoolClassUserRepository.save(membership);
        }
    }

    private void deactivate(SchoolClassUser membership) {
        if (membership.isActive()) {
            membership.deactivate(OffsetDateTime.now());
            schoolClassUserRepository.save(membership);
        }
    }

    private User findCurrentUser(UUID currentUserId) {
        var user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng hiện tại"));
        validateCurrentUserIsActive(user);
        return user;
    }

    private void validateCurrentUserIsActive(User currentUser) {
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Người dùng hiện tại không hoạt động");
        }
    }

    private UUID getSchoolId(User currentUser) {
        var schoolId = currentUser.getSchoolId();
        if (schoolId == null) {
            throw new IllegalStateException("Người dùng hiện tại không thuộc trường nào");
        }
        return schoolId;
    }

    private void validateRequestedSchool(UUID requestedSchoolId, UUID currentSchoolId) {
        if (!Objects.equals(requestedSchoolId, currentSchoolId)) {
            throw new IllegalArgumentException("Trường học không khớp với người dùng hiện tại");
        }
    }

    private void validateSchool(UUID schoolId) {
        var school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));
        if (!school.isActive()) {
            throw new IllegalStateException("Trường học không hoạt động");
        }
    }

    private SchoolClass validateSchoolClass(UUID classId, UUID schoolId) {
        var schoolClass = schoolClassRepository.findById(classId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học"));
        if (!Objects.equals(schoolClass.getSchoolId(), schoolId)) {
            throw new NotFoundException("Không tìm thấy lớp học");
        }
        if (schoolClass.getStatus() != SchoolClassStatus.ACTIVE) {
            throw new IllegalStateException("Lớp học không hoạt động");
        }
        return schoolClass;
    }

    private void validateTargetUser(UUID userId, UUID schoolId) {
        var targetUser = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        if (targetUser.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Người dùng không hoạt động");
        }
        if (!Objects.equals(targetUser.getSchoolId(), schoolId)) {
            throw new IllegalArgumentException("Người dùng không thuộc trường hiện tại");
        }
    }

    private SchoolClassUser findMembership(UUID userId, UUID classId) {
        return schoolClassUserRepository.findByUserIdAndSchoolClassId(userId, classId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng trong lớp học"));
    }
}
