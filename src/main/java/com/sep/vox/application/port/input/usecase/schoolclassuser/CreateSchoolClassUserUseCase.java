package com.sep.vox.application.port.input.usecase.schoolclassuser;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateSchoolClassUserCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schoolclassuser.CreateSchoolClassUserResponse;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class CreateSchoolClassUserUseCase implements IUseCase<CreateSchoolClassUserCommand, CreateSchoolClassUserResponse> {

    private final SchoolClassUserRepository schoolClassUserRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    public CreateSchoolClassUserUseCase(
            SchoolClassUserRepository schoolClassUserRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository) {
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional
    public CreateSchoolClassUserResponse execute(CreateSchoolClassUserCommand input) {
        validateCommand(input);
        var now = Instant.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = findCurrentUser(currentUserId);
        var schoolId = getSchoolId(currentUser);

        validateRequestedSchool(input.schoolId(), schoolId);
        validateSchool(schoolId);
        validateSchoolClass(input.classId(), schoolId);
        validateTargetUser(input.userId(), schoolId);

        // Thêm thành viên = join hoặc re-join. Bản ghi (userId, classId) là duy nhất nên
        // học sinh đã rời lớp (isActive=false) phải được kích hoạt lại thay vì bị chặn.
        var existing = schoolClassUserRepository.findByUserIdAndSchoolClassId(input.userId(), input.classId())
            .orElse(null);
        if (existing != null) {
            if (existing.isActive()) {
                throw new DuplicatedException("Người dùng đã thuộc lớp học");
            }
            existing.activate();
            existing.setAssignedBy(currentUserId);
            var saved = schoolClassUserRepository.save(existing);
            return new CreateSchoolClassUserResponse(saved.getId());
        }

        var schoolClassUser = new SchoolClassUser(
            input.userId(),
            input.classId(),
            true,
            now,
            null,
            currentUserId
        );
        try {
            var saved = schoolClassUserRepository.save(schoolClassUser);
            return new CreateSchoolClassUserResponse(saved.getId());
        } catch (DataIntegrityViolationException e) {
            // Chống race-condition: hai request cùng thêm một thành viên vượt qua check rồi đụng unique index.
            throw new DuplicatedException("Người dùng đã thuộc lớp học");
        }
    }

    private void validateCommand(CreateSchoolClassUserCommand input) {
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
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalStateException("Người dùng hiện tại không thuộc trường nào"));
        return schoolUser.getSchoolId();
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
        if (!targetUser.canBeAssignedToSchoolClass()) {
            throw new IllegalStateException("Người dùng đã bị khoá hoặc vô hiệu hoá");
        }
        SchoolUser targetSchoolUser = schoolUserRepository.findByUserId(targetUser.getId()).orElse(null);
        if (!Objects.equals(targetSchoolUser != null ? targetSchoolUser.getSchoolId() : null, schoolId)) {
            throw new IllegalArgumentException("Người dùng không thuộc trường hiện tại");
        }
    }
}
