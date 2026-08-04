package com.sep.vox.application.port.input.usecase.exam;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.ExamDeliveryModeSupport;
import com.sep.vox.application.common.InstantParser;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateClassTestCommand;
import com.sep.vox.application.port.input.service.ExamAssessmentPolicyValidator;
import com.sep.vox.application.port.input.service.ExamScheduleRoomValidator;
import com.sep.vox.application.port.input.service.ExamStreamConfigResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.exam.CreateClassTestResponse;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamDeliveryMode;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMember;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleProctor;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.exam.ResultDecisionMethod;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;

/**
 * Tạo "vỏ" bài kiểm tra trên lớp: exam DRAFT + ca thi nháp + CHAIR + danh sách học sinh của lớp.
 *
 * <p><b>Không tạo đề nào ở đây.</b> Soạn đề (câu hỏi trực tiếp / blueprint / sao chép) là bước riêng
 * ở trang chi tiết qua {@code CreateExamPaperUseCase}, giống hệt kỳ thi tập trung — nhờ vậy giáo
 * viên soạn được nhiều mã đề cho cùng một bài trên lớp rồi phân đề cho học sinh.
 */
@Service
public class CreateClassTestUseCase implements IUseCase<CreateClassTestCommand, CreateClassTestResponse> {

    private static final int MAX_CLASS_ROSTER_SIZE = 2000;

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final ExamRepository examRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamAssessmentPolicyValidator examAssessmentPolicyValidator;
    private final ExamStreamConfigResolver examStreamConfigResolver;
    private final ExamScheduleRoomValidator examScheduleRoomValidator;
    private final UserContextPort userContextPort;

    public CreateClassTestUseCase(
            SchoolClassRepository schoolClassRepository,
            SchoolClassUserRepository schoolClassUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            ExamRepository examRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamScheduleProctorRepository examScheduleProctorRepository,
            ExamMemberRepository examMemberRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamAssessmentPolicyValidator examAssessmentPolicyValidator,
            ExamStreamConfigResolver examStreamConfigResolver,
            ExamScheduleRoomValidator examScheduleRoomValidator,
            UserContextPort userContextPort) {
        this.schoolClassRepository = schoolClassRepository;
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.examRepository = examRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.examMemberRepository = examMemberRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examAssessmentPolicyValidator = examAssessmentPolicyValidator;
        this.examStreamConfigResolver = examStreamConfigResolver;
        this.examScheduleRoomValidator = examScheduleRoomValidator;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public CreateClassTestResponse execute(CreateClassTestCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        if (userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
                .noneMatch(role -> "TEACHER".equals(role.roleCode()))) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var schoolClass = schoolClassRepository.findById(command.schoolClassId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học"));

        var membership = schoolClassUserRepository.findByUserIdAndSchoolClassId(currentUserId, schoolClass.getId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));
        if (!membership.isActive()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        examAssessmentPolicyValidator.requirePublishedInSchool(
            command.assessmentPolicyId(), schoolClass.getSchoolId());
        validateOpenClose(command.openAt(), command.closeAt());

        var now = Instant.now();
        var exam = createExam(schoolClass, command, currentUserId, now);
        var schedule = createDraftSchedule(exam, command.schoolRoomId(), currentUserId, now);
        return finishSetup(exam, schedule, schoolClass, currentUserId, now);
    }

    /**
     * Các bước sau khi exam đã dựng xong.
     *
     * <p>Bài dừng lại ở DRAFT: giáo viên còn phải soạn đề, chọn phòng và xếp học sinh vào ca (chọn
     * lớp chỉ nạp danh sách học sinh, chưa xếp ca), giống hệt kỳ thi tập trung.
     * {@code validatePlanLimits} vì thế chạy lúc bấm SCHEDULE thay vì lúc tạo.
     */
    private CreateClassTestResponse finishSetup(
            Exam exam,
            ExamSchedule schedule,
            SchoolClass schoolClass,
            UUID currentUserId,
            Instant now) {
        examMemberRepository.save(new ExamMember(exam.getId(), currentUserId, ExamMemberRole.CHAIR, now, currentUserId));
        // Giám khảo mặc định là chính giáo viên tạo bài; giáo viên có thể thêm/bớt sau qua
        // endpoint proctor của ca thi. Không có guard nào cần chạy thêm: người gọi đã được xác nhận
        // là TEACHER và là thành viên active của lớp ở đầu execute().
        examScheduleProctorRepository.save(new ExamScheduleProctor(schedule.getId(), currentUserId));
        var candidateCount = assignCandidates(exam, schoolClass.getId(), currentUserId, now);
        return new CreateClassTestResponse(ExamDtoMapper.toDto(exam), candidateCount);
    }

    private CreateClassTestCommand normalize(CreateClassTestCommand input) {
        return new CreateClassTestCommand(
            input.schoolClassId(),
            StringNormalization.trimAndCollapseSpaces(input.name()),
            StringNormalization.trimAndCollapseSpaces(input.description()),
            input.openAt(),
            input.closeAt(),
            input.assessmentPolicyId(),
            input.maxAttempt(),
            input.examTimeDurationSecond(),
            input.resultDecisionMethod(),
            input.requiredStreamTypes() == null ? null : input.requiredStreamTypes().stream()
                .map(StringNormalization::normalizeCode)
                .toList(),
            input.streamTypePermission() == null ? null : StringNormalization.normalizeCode(input.streamTypePermission()),
            input.deliveryMode() == null ? null : StringNormalization.normalizeCode(input.deliveryMode()),
            input.requiresOtp(),
            input.schoolRoomId()
        );
    }

    private Exam createExam(
            SchoolClass schoolClass,
            CreateClassTestCommand command,
            UUID currentUserId,
            Instant now) {
        var code = "CT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        var openAt = parseOpenAt(command.openAt());
        var closeAt = parseCloseAt(command.closeAt());
        Integer requestedMaxAttempt = command.maxAttempt();
        int maxAttempt = requestedMaxAttempt == null ? 1 : requestedMaxAttempt;
        var streamConfig = examStreamConfigResolver.resolve(
            command.requiredStreamTypes(), command.streamTypePermission());
        // Bài trên lớp không gắn blueprint lúc tạo: giáo viên chọn cách soạn từng mã đề ở trang chi
        // tiết. Nếu sau đó muốn dùng blueprint dùng chung thì gắn qua ChangeClassTestBlueprintUseCase.
        return examRepository.save(new Exam(
            null,
            null,
            code,
            command.name(),
            command.description(),
            schoolClass.getSchoolId(),
            schoolClass.getLanguageId(),
            ExamKind.CLASS_TEST,
            // Bài trên lớp thi ngay trong phòng, chỉ khác nhau ở máy dùng để làm bài: máy của nhà
            // trường (LAB) hay máy học sinh mang theo (STUDENT_DEVICE — mặc định).
            ExamDeliveryModeSupport.parseOrDefault(command.deliveryMode(), ExamDeliveryMode.STUDENT_DEVICE),
            ExamStatus.DRAFT,
            maxAttempt,
            command.examTimeDurationSecond(),
            command.resultDecisionMethod() == null ? ResultDecisionMethod.HIGHEST : command.resultDecisionMethod(),
            openAt,
            closeAt,
            command.assessmentPolicyId(),
            // Bài trên lớp mặc định vào thẳng, giáo viên bật OTP nếu muốn siết như kỳ thi tập trung.
            command.requiresOtp() != null && command.requiresOtp(),
            now,
            // Cả hai cùng NULL nghĩa là không giám sát bằng stream — DB cho phép
            // (chk_exams_required_stream_type_and_stream_type_permission_valid).
            streamConfig.requiredStreamType(),
            streamConfig.streamTypePermission(),
            now,
            currentUserId,
            currentUserId
        ));
    }

    /**
     * Ca thi của bài trên lớp bám đúng khung mở/đóng bài. Phòng có thể để trống ở đây và chọn sau
     * qua updateExamSchedule, nhưng {@code UpdateExamStatusUseCase} sẽ chặn SCHEDULE nếu vẫn thiếu:
     * bài kiểm tra trên lớp cũng thi trong phòng, không cho làm ở nhà.
     */
    private ExamSchedule createDraftSchedule(Exam exam, UUID schoolRoomId, UUID currentUserId, Instant now) {
        if (exam.getOpenAt() == null || exam.getCloseAt() == null) {
            throw new IllegalStateException("Bài kiểm tra trên lớp phải có thời gian mở bài và đóng bài");
        }
        if (schoolRoomId != null) {
            examScheduleRoomValidator.validateForNewSchedule(
                schoolRoomId, exam, exam.getOpenAt(), exam.getCloseAt());
        }

        var schedule = ExamSchedule.createFresh(
            exam.getId(),
            schoolRoomId,
            exam.getOpenAt(),
            exam.getCloseAt(),
            currentUserId,
            now
        );
        return examScheduleRepository.save(schedule);
    }

    /**
     * Nạp toàn bộ học sinh active của lớp vào danh sách dự thi, CHƯA xếp ca và CHƯA gán đề — giống
     * hệt kỳ thi tập trung. Chọn lớp chỉ là bước "đưa vào tab học sinh"; xếp ca (và đề đi kèm) là
     * bước riêng qua endpoint assign-schedule / auto-fill.
     */
    private int assignCandidates(
            Exam exam,
            UUID schoolClassId,
            UUID currentUserId,
            Instant now) {
        // findBySchoolClassId là 1-based (PageRequest.of(page - 1, size)) → trang đầu là 1, KHÔNG phải 0.
        var roster = schoolClassUserRepository.findBySchoolClassId(schoolClassId, 1, MAX_CLASS_ROSTER_SIZE).content();

        var activeUserIds = roster.stream()
            .filter(classUser -> classUser.isActive())
            .map(classUser -> classUser.getUserId())
            .distinct()
            .toList();

        // MỘT query cho cả lớp thay vì hỏi vai trò từng học sinh — lớp 40 người là 40 query, và trần
        // roster là 2000. Giống hệt ImportExamCandidatesFromClassUseCase.
        var studentIds = userRoleQueryRepository.findUserIdsByRoleCode(activeUserIds, SchoolRoleCodes.STUDENT);

        var candidates = new ArrayList<ExamCandidate>();
        for (var userId : activeUserIds) {
            if (!studentIds.contains(userId)) {
                continue;
            }
            candidates.add(ExamCandidate.createFresh(exam.getId(), userId, currentUserId, now));
        }
        return examCandidateRepository.saveAll(candidates).size();
    }

    private void validateOpenClose(String openAt, String closeAt) {
        var open = parseOpenAt(openAt);
        var close = parseCloseAt(closeAt);
        if (open == null || close == null) {
            throw new IllegalStateException("Bài kiểm tra trên lớp phải có thời gian mở bài và đóng bài");
        }
        if (!open.isBefore(close)) {
            throw new IllegalStateException("Thời gian mở bài phải nhỏ hơn thời gian đóng bài");
        }
    }

    private Instant parseOpenAt(String value) {
        return InstantParser.parseOrNull(value, "Thời gian mở bài");
    }

    private Instant parseCloseAt(String value) {
        return InstantParser.parseOrNull(value, "Thời gian đóng bài");
    }
}
