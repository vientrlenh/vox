package com.sep.vox.application.port.input.usecase.schooluser;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.UserCreatedPayloadV1;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateSchoolUserCommand;
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

    public CreateSchoolUserUseCase(
            UserContextPort userContextPort,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            SchoolUserRepository schoolUserRepository,
            SchoolRepository schoolRepository,
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.schoolRepository = schoolRepository;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public CreateSchoolUserResponse execute(CreateSchoolUserCommand input) {
        var now = OffsetDateTime.now();
        var callerId = userContextPort.getCurrentAuthenticatedUserId();

        var caller = userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        var callerSchoolUser = schoolUserRepository.findByUserId(caller.getId())
            .orElseThrow(() -> new IllegalArgumentException("Không có quyền thực hiện thao tác này"));
        if (!input.schoolId().equals(callerSchoolUser.getSchoolId())) {
            throw new IllegalArgumentException("Không có quyền thực hiện thao tác này");
        }

        var command = normalize(input);
        var school = schoolRepository.findById(command.schoolId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));
        if (!school.isActive()) {
            throw new IllegalStateException("Trường học không hoạt động");
        }

        if (!ALLOWED_ROLE_CODES.contains(command.roleCode())) {
            throw new IllegalArgumentException("Vai trò không hợp lệ, chỉ chấp nhận STUDENT hoặc TEACHER");
        }
        var role = roleRepository.findByCode(command.roleCode())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy vai trò: " + command.roleCode()));

        if (userRepository.findByEmail(command.email()).isPresent()) {
            throw new DuplicatedException("Email đã tồn tại");
        }
        if (userRepository.findByPhone(command.phone()).isPresent()) {
            throw new DuplicatedException("Số điện thoại đã tồn tại");
        }

        User user = User.create(command.email(), command.phone(), command.fullName(),
            command.dateOfBirth(), command.address(), null, callerId, now);

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatedException("Email hoặc số điện thoại đã tồn tại");
        }

        userRoleRepository.save(new UserRole(savedUser.getId(), role.getId(), now));

        if ("STUDENT".equals(command.roleCode())) {
            if (command.startDate() == null || command.endDate() == null) {
                throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc là bắt buộc đối với học sinh");
            }
            if (!command.startDate().isBefore(command.endDate())) {
                throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc");
            }
            var startDate = command.startDate().atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
            var endDate = command.endDate().atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
            saveSchoolUser(SchoolUser.create(savedUser.getId(), command.schoolId(), startDate, endDate));
        } else {
            var startDate = command.startDate() != null
                ? command.startDate().atStartOfDay(ZoneOffset.UTC).toOffsetDateTime()
                : now;
            saveSchoolUser(SchoolUser.create(savedUser.getId(), command.schoolId(), startDate, null));
        }

        // Ghi outbox trong cùng transaction với việc tạo user: user tồn tại thì event
        // chắc chắn tồn tại. Token đặt mật khẩu do consumer sinh ngay trước khi gửi mail,
        // để nó không nằm plaintext trong bảng outboxes lẫn trong topic Kafka.
        var event = new UserCreatedPayloadV1(
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

        return new CreateSchoolUserResponse(savedUser.getId());
    }

    private void saveSchoolUser(SchoolUser schoolUser) {
        try {
            schoolUserRepository.save(schoolUser);
        } catch (DataIntegrityViolationException e) {
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