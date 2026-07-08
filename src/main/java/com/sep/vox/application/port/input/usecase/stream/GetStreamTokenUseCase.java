package com.sep.vox.application.port.input.usecase.stream;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.RoleConstant;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.GetStreamTokenCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.StreamTokenProvider;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class GetStreamTokenUseCase implements IUseCase<GetStreamTokenCommand, String> {

    private static final Set<String> ALLOWED_STREAM_TYPES = Set.of("camera", "screen");

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamRepository examRepository;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final StreamTokenProvider streamTokenProvider; 


    public GetStreamTokenUseCase(UserContextPort userContextPort, UserRepository userRepository, UserRoleQueryRepository userRoleQueryRepository, ExamScheduleRepository examScheduleRepository, ExamRepository examRepository, ExamScheduleProctorRepository examScheduleProctorRepository, ExamMemberRepository examMemberRepository, ExamCandidateRepository examCandidateRepository, StreamTokenProvider streamTokenProvider) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examRepository = examRepository;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.examMemberRepository = examMemberRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.streamTokenProvider = streamTokenProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public String execute(GetStreamTokenCommand input) {
        validateCommand(input);

        var now = OffsetDateTime.now();
        var userId = userContextPort.getCurrentAuthenticatedUserId();
        var roles = getValidUserRoles(userId);
        var streamRole = resolveStreamRole(roles);

        var exam = examRepository.findById(input.examId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ thi"));

        var activeSchedules = examScheduleRepository.findByExamIdAndInSchedule(input.examId(), now)
            .stream()
            .collect(Collectors.toMap(s -> s.getId(), s -> s));
        
        var requestedSchedules = input.roomIds().stream()
            .map(roomId -> {
                var schedule = activeSchedules.get(roomId);
                if (schedule == null) {
                    throw new ForbiddenException("Phòng không thuộc kỳ thi hoặc không trong giờ thi: " + roomId);
                }
                authorizeRoom(streamRole, userId, input.examId(), exam.getSchoolId(), schedule);
                return schedule;
            })
            .toList();
        
        var streamTypes = resolveStreamTypes(streamRole, input.streamTypes());

        var windowStart = requestedSchedules.stream()
            .map(s -> s.getStartDate())
            .min(OffsetDateTime::compareTo)
            .orElseThrow();
        var windowEnd = requestedSchedules.stream()
            .map(s -> s.getEndDate())
            .max(OffsetDateTime::compareTo)
            .orElseThrow();

        return streamTokenProvider.generateToken(
            userId.toString(), 
            input.roomIds().stream().map(r -> r.toString()).toList(), 
            input.examId().toString(), 
            List.of(streamRole), 
            streamTypes, 
            windowStart, 
            windowEnd
        );
    }


    private void validateCommand(GetStreamTokenCommand input) {
        if (input.roomIds().isEmpty()) {
            throw new IllegalArgumentException("Danh sách phòng không được để trống");
        }
    }

    private List<String> getValidUserRoles(UUID userId) {
        userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
            .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại hoặc đã bị khóa"));
        return userRoleQueryRepository.findByUserIdWithRoleInfo(userId)
            .stream()
            .map(role -> role.roleCode())
            .toList();
    }

    private String resolveStreamRole(List<String> roles) {
        if (roles.contains(RoleConstant.SYSTEM_ADMIN_ROLE)) {
            throw new ForbiddenException("Bạn không có quyền lấy token giám sát/thi");
        }
        if (roles.contains(RoleConstant.SCHOOL_ADMIN_ROLE)) return RoleConstant.SCHOOL_ADMIN_ROLE;
        if (roles.contains(RoleConstant.TEACHER_ROLE)) return RoleConstant.TEACHER_ROLE;
        if (roles.contains(RoleConstant.STUDENT_ROLE)) return RoleConstant.STUDENT_ROLE;
        throw new ForbiddenException("Vai trò không được phép");
    }

    private void authorizeRoom(String streamRole, UUID userId, UUID examId, UUID examSchoolId, ExamSchedule schedule) {
        switch (streamRole) {
            case RoleConstant.SCHOOL_ADMIN_ROLE -> {
                var mySchoolId = userContextPort.getCurrentSchoolId();
                if (mySchoolId == null || !mySchoolId.equals(examSchoolId)) {
                    throw new ForbiddenException("Kỳ thi không thuộc trường bạn quản lý");
                }
            }
            case RoleConstant.TEACHER_ROLE -> {
                var isChair = examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR);
                if (!isChair && !examScheduleProctorRepository.existsByScheduleIdAndTeacherId(schedule.getId(), userId)) {
                    throw new ForbiddenException("Bạn không chủ trì kỳ thi và không gác ca này");
                }
            }
            case RoleConstant.STUDENT_ROLE -> {
                if (!examCandidateRepository.existsByScheduleIdAndStudentId(schedule.getId(), userId)) {
                    throw new ForbiddenException("Bạn không thuộc ca thi này");
                }
            }
            default -> throw new ForbiddenException("Vai trò không hỗ trợ");
        }
    }

    private List<String> resolveStreamTypes(String streamRole, List<String> requested) {
        if (!RoleConstant.STUDENT_ROLE.equals(streamRole)) {
            return List.of();
        }
        if (requested.isEmpty()) {
            return List.copyOf(ALLOWED_STREAM_TYPES);
        }
        var invalid = requested.stream().filter(t -> !ALLOWED_STREAM_TYPES.contains(t)).toList();
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException("Kiểu stream không hợp lệ: " + invalid);
        }
        return requested.stream().distinct().toList();
    }
}
