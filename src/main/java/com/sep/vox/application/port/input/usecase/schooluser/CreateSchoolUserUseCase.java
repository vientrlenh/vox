package com.sep.vox.application.port.input.usecase.schooluser;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.common.UserStatusValidator;
import com.sep.vox.application.event.SchoolUserPasswordSetUpEmailRequestedEvent;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.PasswordSetUpTokenPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schooluser.CreateSchoolUserResponse;
import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.PasswordSetUpTokenRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;

import java.time.ZoneOffset;

@Service
public class CreateSchoolUserUseCase implements IUseCase<CreateSchoolUserCommand, CreateSchoolUserResponse> {

    private static final List<String> ALLOWED_ROLE_CODES = List.of("STUDENT", "TEACHER");

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordSetUpTokenPort passwordSetUpTokenPort;
    private final PasswordSetUpTokenRepository passwordSetUpTokenRepository;
    private final EventPublisherPort eventPublisherPort;

    public CreateSchoolUserUseCase(
            UserContextPort userContextPort,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            SchoolUserRepository schoolUserRepository,
            SchoolRepository schoolRepository,
            PasswordSetUpTokenPort passwordSetUpTokenPort,
            PasswordSetUpTokenRepository passwordSetUpTokenRepository,
            EventPublisherPort eventPublisherPort) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.schoolRepository = schoolRepository;
        this.passwordSetUpTokenPort = passwordSetUpTokenPort;
        this.passwordSetUpTokenRepository = passwordSetUpTokenRepository;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    @Transactional
    public CreateSchoolUserResponse execute(CreateSchoolUserCommand input) {
        var now = OffsetDateTime.now();
        var callerId = userContextPort.getCurrentAuthenticatedUserId();

        var caller = userRepository.findById(callerId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        UserStatusValidator.requireActive(caller);
        if (!input.schoolId().equals(caller.getSchoolId())) {
            throw new IllegalArgumentException("Không có quyền thực hiện thao tác này");
        }

        var command = normalize(input);
        var school = schoolRepository.findById(command.schoolId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));

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

        User user = command.roleCode().equals("STUDENT")
            ? User.createStudent(command.email(), command.phone(), command.fullName(), command.dateOfBirth(), command.address(), null, callerId, command.schoolId(), now)
            : User.createTeacher(command.email(), command.phone(), command.fullName(), command.dateOfBirth(), command.address(), null, callerId, command.schoolId(), now);

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatedException("Email hoặc số điện thoại đã tồn tại");
        }

        userRoleRepository.save(new UserRole(savedUser.getId(), role.getId(), now));

        if ("STUDENT".equals(command.roleCode()) && command.studentId() != null) {
            if (command.startDate() == null || command.endDate() == null) {
                throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc là bắt buộc đối với học sinh");
            }
            var startDate = command.startDate().atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
            var endDate = command.endDate().atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
            schoolUserRepository.save(
                SchoolUser.create(command.studentId(), command.schoolId(), savedUser.getId(), startDate, endDate)
            );
        }

        var generatedPasswordSetUpToken = passwordSetUpTokenPort.generateToken();
        passwordSetUpTokenRepository.save(PasswordSetUpToken.create(savedUser.getId(), generatedPasswordSetUpToken.hashedToken()));
        eventPublisherPort.publish(new SchoolUserPasswordSetUpEmailRequestedEvent(
            command.email(),
            command.fullName(),
            school.getName(),
            savedUser.getId(),
            generatedPasswordSetUpToken.rawToken()
        ));

        return new CreateSchoolUserResponse(savedUser.getId());
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
            input.studentId() != null ? input.studentId().trim() : null,
            input.startDate(),
            input.endDate()
        );
    }
}