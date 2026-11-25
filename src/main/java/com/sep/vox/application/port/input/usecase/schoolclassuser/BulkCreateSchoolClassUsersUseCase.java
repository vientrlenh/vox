package com.sep.vox.application.port.input.usecase.schoolclassuser;

import com.sep.vox.domain.model.school.School;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.schoolclassuser.BulkCreateSchoolClassUsersCommand;
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
@RequiredArgsConstructor
public class BulkCreateSchoolClassUsersUseCase
        implements IUseCase<BulkCreateSchoolClassUsersCommand, BulkCreateSchoolClassUsersResponse> {

    private final SchoolClassUserRepository schoolClassUserRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public BulkCreateSchoolClassUsersResponse execute(BulkCreateSchoolClassUsersCommand input) {
        List<UUID> requestedUserIds = validateCommand(input);
        Instant now = Instant.now();
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = findCurrentUser(currentUserId);
        UUID schoolId = getSchoolId(currentUser);

        validateRequestedSchool(input.schoolId(), schoolId);
        validateSchool(schoolId);
        validateSchoolClass(input.classId(), schoolId);

        RequestedUserLookups lookups = loadLookups(requestedUserIds, input.classId());
        MembershipOutcome outcome = decideMemberships(
            requestedUserIds, lookups, schoolId, input.classId(), now, currentUserId);

        saveMemberships(outcome.toSave());

        return new BulkCreateSchoolClassUsersResponse(List.copyOf(outcome.addedUserIds()), List.copyOf(outcome.failed()));
    }

    // Loaded in bulk so the number of queries does not grow with the number of selected users.
    private RequestedUserLookups loadLookups(List<UUID> requestedUserIds, UUID classId) {
        Map<UUID, User> usersById = userRepository.findByIdIn(requestedUserIds).stream()
            .collect(Collectors.toMap(user -> user.getId(), user -> user, (first, second) -> first));
        Map<UUID, UUID> schoolIdsByUserId = schoolUserRepository.findByUserIdIn(requestedUserIds).stream()
            .collect(Collectors.toMap(
                schoolUser -> schoolUser.getUserId(), schoolUser -> schoolUser.getSchoolId(), (first, second) -> first));
        Map<UUID, SchoolClassUser> membershipsByUserId = schoolClassUserRepository
            .findByUserIdInAndSchoolClassIdIn(requestedUserIds, List.of(classId)).stream()
            .collect(Collectors.toMap(
                membership -> membership.getUserId(), membership -> membership, (first, second) -> first));
        return new RequestedUserLookups(usersById, schoolIdsByUserId, membershipsByUserId);
    }

    private MembershipOutcome decideMemberships(List<UUID> requestedUserIds, RequestedUserLookups lookups,
            UUID schoolId, UUID classId, Instant now, UUID currentUserId) {
        List<UUID> addedUserIds = new ArrayList<UUID>();
        List<BulkCreateSchoolClassUserFailure> failed = new ArrayList<BulkCreateSchoolClassUserFailure>();
        List<SchoolClassUser> toSave = new ArrayList<SchoolClassUser>();

        for (UUID userId : requestedUserIds) {
            String reason = rejectionReason(
                userId, lookups.usersById(), lookups.schoolIdsByUserId(), lookups.membershipsByUserId(), schoolId);
            if (reason != null) {
                failed.add(new BulkCreateSchoolClassUserFailure(userId, reason));
                continue;
            }

            toSave.add(joinOrRejoin(lookups.membershipsByUserId().get(userId), userId, classId, now, currentUserId));
            addedUserIds.add(userId);
        }
        return new MembershipOutcome(addedUserIds, failed, toSave);
    }

    // Adding a member = join or re-join: a student who left the class is reactivated instead of getting
    // a new row, because (userId, classId) is unique.
    private SchoolClassUser joinOrRejoin(
            SchoolClassUser existing, UUID userId, UUID classId, Instant now, UUID currentUserId) {
        if (existing == null) {
            return SchoolClassUser.joined(userId, classId, now, currentUserId);
        }
        existing.activate();
        return existing;
    }

    private void saveMemberships(List<SchoolClassUser> toSave) {
        if (toSave.isEmpty()) {
            return;
        }
        try {
            schoolClassUserRepository.saveAll(toSave);
        } catch (DataConflictException e) {
            // Race-condition guard: another request just added the same member and hit the unique index.
            throw new ResourceDuplicatedException("Một số người dùng đã thuộc lớp học");
        }
    }

    private record RequestedUserLookups(
            Map<UUID, User> usersById,
            Map<UUID, UUID> schoolIdsByUserId,
            Map<UUID, SchoolClassUser> membershipsByUserId) {
    }

    private record MembershipOutcome(
            List<UUID> addedUserIds,
            List<BulkCreateSchoolClassUserFailure> failed,
            List<SchoolClassUser> toSave) {
    }

    private String rejectionReason(
            UUID userId,
            Map<UUID, User> usersById,
            Map<UUID, UUID> schoolIdsByUserId,
            Map<UUID, SchoolClassUser> membershipsByUserId,
            UUID schoolId) {
        User targetUser = usersById.get(userId);
        if (targetUser == null) {
            return "Không tìm thấy người dùng";
        }
        if (!targetUser.canBeAssignedToSchoolClass()) {
            return "Người dùng đã bị khoá hoặc vô hiệu hoá";
        }
        if (!Objects.equals(schoolIdsByUserId.get(userId), schoolId)) {
            return "Người dùng không thuộc trường hiện tại";
        }
        SchoolClassUser existing = membershipsByUserId.get(userId);
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

        Set<UUID> distinctUserIds = input.userIds().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (distinctUserIds.isEmpty()) {
            throw new IllegalArgumentException("Danh sách người dùng không được để trống");
        }
        return List.copyOf(distinctUserIds);
    }

    private User findCurrentUser(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng hiện tại"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Người dùng hiện tại không hoạt động");
        }
        return user;
    }

    private UUID getSchoolId(User currentUser) {
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("Người dùng hiện tại không thuộc trường nào"));
        return schoolUser.getSchoolId();
    }

    private void validateRequestedSchool(UUID requestedSchoolId, UUID currentSchoolId) {
        if (!Objects.equals(requestedSchoolId, currentSchoolId)) {
            throw new IllegalArgumentException("Trường học không khớp với người dùng hiện tại");
        }
    }

    private void validateSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trường học"));
        if (!school.isActive()) {
            throw new IllegalArgumentException("Trường học không hoạt động");
        }
    }

    private SchoolClass validateSchoolClass(UUID classId, UUID schoolId) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
        if (!Objects.equals(schoolClass.getSchoolId(), schoolId)) {
            throw new ResourceNotFoundException("Không tìm thấy lớp học");
        }
        if (schoolClass.getStatus() != SchoolClassStatus.ACTIVE) {
            throw new IllegalArgumentException("Lớp học không hoạt động");
        }
        return schoolClass;
    }
}
