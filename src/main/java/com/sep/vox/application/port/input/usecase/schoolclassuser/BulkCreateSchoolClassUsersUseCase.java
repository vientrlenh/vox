package com.sep.vox.application.port.input.usecase.schoolclassuser;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.BulkCreateSchoolClassUsersCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schoolclassuser.BulkCreateSchoolClassUserFailure;
import com.sep.vox.application.response.input.schoolclassuser.BulkCreateSchoolClassUsersResponse;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * Thêm nhiều người dùng vào một lớp học trong một lần gọi.
 *
 * <p>Bối cảnh (người gọi, trường học, lớp học) chỉ được kiểm tra một lần và lỗi ở bước này làm
 * hỏng cả request. Ngược lại, lỗi của từng người dùng (không tồn tại, không hoạt động, khác
 * trường, đã ở trong lớp) chỉ loại người đó ra và được trả về trong danh sách {@code failed} —
 * những người hợp lệ vẫn được thêm.
 */
@Service
public class BulkCreateSchoolClassUsersUseCase
        implements IUseCase<BulkCreateSchoolClassUsersCommand, BulkCreateSchoolClassUsersResponse> {

    private final SchoolClassUserRepository schoolClassUserRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    public BulkCreateSchoolClassUsersUseCase(
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
    public BulkCreateSchoolClassUsersResponse execute(BulkCreateSchoolClassUsersCommand input) {
        var requestedUserIds = validateCommand(input);
        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = findCurrentUser(currentUserId);
        var schoolId = getSchoolId(currentUser);

        validateRequestedSchool(input.schoolId(), schoolId);
        validateSchool(schoolId);
        validateSchoolClass(input.classId(), schoolId);

        // Nạp theo lô để số truy vấn không tăng theo số người dùng được chọn.
        var usersById = userRepository.findByIdIn(requestedUserIds).stream()
            .collect(Collectors.toMap(user -> user.getId(), user -> user, (first, second) -> first));
        var schoolIdsByUserId = schoolUserRepository.findByUserIdIn(requestedUserIds).stream()
            .collect(Collectors.toMap(
                schoolUser -> schoolUser.getUserId(), schoolUser -> schoolUser.getSchoolId(), (first, second) -> first));
        var membershipsByUserId = schoolClassUserRepository
            .findByUserIdInAndSchoolClassIdIn(requestedUserIds, List.of(input.classId())).stream()
            .collect(Collectors.toMap(
                membership -> membership.getUserId(), membership -> membership, (first, second) -> first));

        var addedUserIds = new ArrayList<UUID>();
        var failed = new ArrayList<BulkCreateSchoolClassUserFailure>();
        var toSave = new ArrayList<SchoolClassUser>();

        for (var userId : requestedUserIds) {
            var reason = rejectionReason(userId, usersById, schoolIdsByUserId, membershipsByUserId, schoolId);
            if (reason != null) {
                failed.add(new BulkCreateSchoolClassUserFailure(userId, reason));
                continue;
            }

            // Thêm thành viên = join hoặc re-join: học sinh đã rời lớp được kích hoạt lại
            // thay vì tạo bản ghi mới, vì (userId, classId) là duy nhất.
            var existing = membershipsByUserId.get(userId);
            if (existing != null) {
                existing.activate();
                toSave.add(existing);
            } else {
                toSave.add(new SchoolClassUser(userId, input.classId(), true, now, null, currentUserId));
            }
            addedUserIds.add(userId);
        }

        if (!toSave.isEmpty()) {
            try {
                schoolClassUserRepository.saveAll(toSave);
            } catch (DataIntegrityViolationException e) {
                // Chống race-condition: một request khác vừa thêm cùng thành viên và đụng unique index.
                throw new DuplicatedException("Một số người dùng đã thuộc lớp học");
            }
        }

        return new BulkCreateSchoolClassUsersResponse(List.copyOf(addedUserIds), List.copyOf(failed));
    }

    private String rejectionReason(
            UUID userId,
            Map<UUID, User> usersById,
            Map<UUID, UUID> schoolIdsByUserId,
            Map<UUID, SchoolClassUser> membershipsByUserId,
            UUID schoolId) {
        var targetUser = usersById.get(userId);
        if (targetUser == null) {
            return "Không tìm thấy người dùng";
        }
        if (targetUser.getStatus() != UserStatus.ACTIVE) {
            return "Người dùng không hoạt động";
        }
        if (!Objects.equals(schoolIdsByUserId.get(userId), schoolId)) {
            return "Người dùng không thuộc trường hiện tại";
        }
        var existing = membershipsByUserId.get(userId);
        if (existing != null && existing.isActive()) {
            return "Người dùng đã thuộc lớp học";
        }
        return null;
    }

    private List<UUID> validateCommand(BulkCreateSchoolClassUsersCommand input) {
        if (input.schoolId() == null) {
            throw new IllegalArgumentException("Trường học không được để trống");
        }
        if (input.classId() == null) {
            throw new IllegalArgumentException("Lớp học không được để trống");
        }
        if (input.userIds() == null || input.userIds().isEmpty()) {
            throw new IllegalArgumentException("Danh sách người dùng không được để trống");
        }

        var distinctUserIds = input.userIds().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (distinctUserIds.isEmpty()) {
            throw new IllegalArgumentException("Danh sách người dùng không được để trống");
        }
        return List.copyOf(distinctUserIds);
    }

    private User findCurrentUser(UUID currentUserId) {
        var user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng hiện tại"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Người dùng hiện tại không hoạt động");
        }
        return user;
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
}
