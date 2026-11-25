package com.sep.vox.application.port.input.usecase.schooluser;

import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.user.Role;
import java.time.Instant;
import java.util.List;

import java.util.UUID;
import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.UserCreatedPayloadV1;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.schooluser.CreateSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schooluser.CreateSchoolUserResponse;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.common.UserTypeConstant;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;

@Service
@RequiredArgsConstructor
public class CreateSchoolUserUseCase implements IUseCase<CreateSchoolUserCommand, CreateSchoolUserResponse> {

    private static final List<String> ALLOWED_ROLE_CODES = List.of("STUDENT", "TEACHER");

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolRepository schoolRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    @Transactional
    public CreateSchoolUserResponse execute(CreateSchoolUserCommand input) {
        Instant now = Instant.now();
        UUID callerId = userContextPort.getCurrentAuthenticatedUserId();

        requireCallerBelongsToSchool(callerId, input.schoolId());

        CreateSchoolUserCommand command = normalize(input);
        School school = findActiveSchool(command.schoolId());
        Role role = findAllowedRole(command.roleCode());
        rejectDuplicateEmailOrPhone(command);

        User savedUser = saveNewUser(command);

        userRoleRepository.save(UserRole.assigned(savedUser.getId(), role.getId(), now));

        linkToSchool(savedUser, command, now);
        saveUserCreatedEvent(savedUser, command, school, now);

        return new CreateSchoolUserResponse(savedUser.getId());
    }

    private void requireCallerBelongsToSchool(UUID callerId, UUID schoolId) {
        User caller = userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        SchoolUser callerSchoolUser = schoolUserRepository.findByUserId(caller.getId())
            .orElseThrow(() -> new IllegalArgumentException("Không có quyền thực hiện thao tác này"));
        if (!schoolId.equals(callerSchoolUser.getSchoolId())) {
            throw new IllegalArgumentException("Không có quyền thực hiện thao tác này");
        }
    }

    private School findActiveSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trường học"));
        if (!school.isActive()) {
            throw new IllegalArgumentException("Trường học không hoạt động");
        }
        return school;
    }

    private Role findAllowedRole(String roleCode) {
        if (!ALLOWED_ROLE_CODES.contains(roleCode)) {
            throw new IllegalArgumentException("Vai trò không hợp lệ, chỉ chấp nhận STUDENT hoặc TEACHER");
        }
        return roleRepository.findByCode(roleCode)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò: " + roleCode));
    }

    private void rejectDuplicateEmailOrPhone(CreateSchoolUserCommand command) {
        if (userRepository.findByEmail(command.email()).isPresent()) {
            throw new ResourceDuplicatedException("Email đã tồn tại");
        }
        if (userRepository.findByPhone(command.phone()).isPresent()) {
            throw new ResourceDuplicatedException("Số điện thoại đã tồn tại");
        }
    }

    private User saveNewUser(CreateSchoolUserCommand command) {
        User user = User.create(command.email(), command.phone(), command.fullName(),
            command.dateOfBirth(), command.address(), null);

        try {
            return userRepository.save(user);
        } catch (DataConflictException e) {
            throw new ResourceDuplicatedException("Email hoặc số điện thoại đã tồn tại");
        }
    }

    private void linkToSchool(User savedUser, CreateSchoolUserCommand command, Instant now) {
        if ("STUDENT".equals(command.roleCode())) {
            if (command.startDate() == null || command.endDate() == null) {
                throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc là bắt buộc đối với học sinh");
            }
            if (!command.startDate().isBefore(command.endDate())) {
                throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc");
            }
            Instant startDate = command.startDate().atStartOfDay(DateMapper.DEFAULT_INPUT_ZONE).toInstant();
            Instant endDate = command.endDate().atStartOfDay(DateMapper.DEFAULT_INPUT_ZONE).toInstant();
            saveSchoolUser(SchoolUser.create(savedUser.getId(), command.schoolId(), startDate, endDate));
        } else {
            Instant startDate = command.startDate() != null
                ? command.startDate().atStartOfDay(DateMapper.DEFAULT_INPUT_ZONE).toInstant()
                : now;
            saveSchoolUser(SchoolUser.create(savedUser.getId(), command.schoolId(), startDate, null));
        }
    }

    private void saveUserCreatedEvent(User savedUser, CreateSchoolUserCommand command, School school, Instant now) {
        // Write the outbox row in the same transaction as the user creation: if the user exists, the event exists.
        // The consumer generates the password set-up token right before it sends the mail, so the token is never
        // stored as plaintext in the outboxes table or in the Kafka topic.
        UserCreatedPayloadV1 event = new UserCreatedPayloadV1(
            savedUser.getId(),
            command.email(),
            command.fullName(),
            school.getName(),
            UserTypeConstant.SCHOOL_USER
        );
        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.USER,
            savedUser.getId(),
            EventTypeConstant.USER_CREATED,
            jsonSerializationPort.toJson(event),
            now
        ));
    }

    private void saveSchoolUser(SchoolUser schoolUser) {
        try {
            schoolUserRepository.save(schoolUser);
        } catch (DataConflictException e) {
            throw new IllegalArgumentException("Không thể gán người dùng vào trường: dữ liệu không hợp lệ");
        }
    }

    private CreateSchoolUserCommand normalize(CreateSchoolUserCommand input) {
        return new CreateSchoolUserCommand(
            input.schoolId(),
            StringNormalization.normalizeEmail(input.email()),
            StringNormalization.normalizePhone(input.phone()),
            StringNormalization.trimAndCollapseSpaces(input.fullName()),
            input.dateOfBirth(),
            input.address() != null ? StringNormalization.trimAndCollapseSpaces(input.address()) : null,
            input.roleCode() != null ? input.roleCode().trim().toUpperCase() : null,
            input.startDate(),
            input.endDate()
        );
    }
}