package com.sep.vox.application.port.input.usecase.assessment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.CreateClassTestCommand;
import com.sep.vox.application.port.input.service.AssessmentPolicyValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleRoomValidator;
import com.sep.vox.application.port.input.service.AssessmentStreamConfigResolver;
import com.sep.vox.application.port.input.service.SchoolSubscriptionActiveGuardService;
import com.sep.vox.application.port.input.service.SubscriptionPeriodGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.assessment.CreateClassTestResponse;
import com.sep.vox.application.shared.DateMapper;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentDeliveryMode;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMember;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultDecisionMethod;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;
import com.sep.vox.domain.service.assessment.AssessmentDeliveryModeSupport;

/**
 * Tạo "vỏ" bài kiểm tra trên lớp: assessment DRAFT + ca thi nháp + CHAIR + danh sách học sinh của lớp.
 *
 * <p><b>Không tạo đề nào ở đây.</b> Soạn đề (câu hỏi trực tiếp / blueprint / sao chép) là bước riêng
 * ở trang chi tiết qua {@code CreateAssessmentPaperUseCase}, giống hệt kỳ thi tập trung — nhờ vậy giáo
 * viên soạn được nhiều mã đề cho cùng một bài trên lớp rồi phân đề cho học sinh.
 */
@Service
public class CreateClassTestUseCase implements IUseCase<CreateClassTestCommand, CreateClassTestResponse> {

    private static final int MAX_CLASS_ROSTER_SIZE = 2000;

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentPolicyValidator assessmentPolicyValidator;
    private final AssessmentStreamConfigResolver assessmentStreamConfigResolver;
    private final AssessmentScheduleRoomValidator assessmentScheduleRoomValidator;
    private final AssessmentScheduleProctorConflictValidator assessmentScheduleProctorConflictValidator;
    private final SubscriptionPeriodGuardService subscriptionPeriodGuardService;
    private final UserContextPort userContextPort;
    private final SchoolSubscriptionActiveGuardService schoolSubscriptionActiveGuardService;

    public CreateClassTestUseCase(
            SchoolClassRepository schoolClassRepository,
            SchoolClassUserRepository schoolClassUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            AssessmentRepository assessmentRepository,
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentScheduleProctorRepository assessmentScheduleProctorRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentPolicyValidator assessmentPolicyValidator,
            AssessmentStreamConfigResolver assessmentStreamConfigResolver,
            AssessmentScheduleRoomValidator assessmentScheduleRoomValidator,
            AssessmentScheduleProctorConflictValidator assessmentScheduleProctorConflictValidator,
            SubscriptionPeriodGuardService subscriptionPeriodGuardService,
            UserContextPort userContextPort,
            SchoolSubscriptionActiveGuardService schoolSubscriptionActiveGuardService) {
        this.schoolClassRepository = schoolClassRepository;
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentScheduleProctorRepository = assessmentScheduleProctorRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentPolicyValidator = assessmentPolicyValidator;
        this.assessmentStreamConfigResolver = assessmentStreamConfigResolver;
        this.assessmentScheduleRoomValidator = assessmentScheduleRoomValidator;
        this.assessmentScheduleProctorConflictValidator = assessmentScheduleProctorConflictValidator;
        this.subscriptionPeriodGuardService = subscriptionPeriodGuardService;
        this.userContextPort = userContextPort;
        this.schoolSubscriptionActiveGuardService = schoolSubscriptionActiveGuardService;
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
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));

        var membership = schoolClassUserRepository.findByUserIdAndSchoolClassId(currentUserId, schoolClass.getId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));
        if (!membership.isActive()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        schoolSubscriptionActiveGuardService.requireActiveForSchool(schoolClass.getSchoolId(), "tạo Class Test");

        assessmentPolicyValidator.requirePublishedInSchool(
            command.assessmentPolicyId(), schoolClass.getSchoolId());
        validateOpenClose(command.openAt(), command.closeAt(), schoolClass.getSchoolId());

        var now = Instant.now();
        var assessment = createAssessment(schoolClass, command, currentUserId, now);
        var schedule = createDraftSchedule(assessment, command.schoolRoomId(), currentUserId, now);
        return finishSetup(assessment, schedule, schoolClass, currentUserId, now);
    }

    /**
     * Các bước sau khi assessment đã dựng xong.
     *
     * <p>Bài dừng lại ở DRAFT: giáo viên còn phải soạn đề, chọn phòng và xếp học sinh vào ca (chọn
     * lớp chỉ nạp danh sách học sinh, chưa xếp ca), giống hệt kỳ thi tập trung.
     * {@code validatePlanLimits} vì thế chạy lúc bấm SCHEDULE thay vì lúc tạo.
     */
    private CreateClassTestResponse finishSetup(
            Assessment assessment,
            AssessmentSchedule schedule,
            SchoolClass schoolClass,
            UUID currentUserId,
            Instant now) {
        assessmentMemberRepository.save(new AssessmentMember(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR, now, currentUserId));
        // Giám khảo mặc định là chính giáo viên tạo bài; giáo viên có thể thêm/bớt sau qua
        // endpoint proctor của ca thi. Quyền thì không phải kiểm thêm (người gọi đã được xác nhận là
        // TEACHER và là thành viên active của lớp ở đầu execute()), nhưng lịch thì có: luật "không
        // gác hai ca trùng giờ" là toàn trường nên bài trên lớp này có thể đâm vào một ca thi khác.
        assessmentScheduleProctorConflictValidator.requireTeacherFree(
            currentUserId, schedule.getStartDate(), schedule.getEndDate(), schedule.getId());
        assessmentScheduleProctorRepository.save(new AssessmentScheduleProctor(schedule.getId(), currentUserId));
        var candidateCount = assignCandidates(assessment, schoolClass.getId(), currentUserId, now);
        return new CreateClassTestResponse(AssessmentDtoMapper.toDto(assessment), candidateCount);
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
            input.assessmentTimeDurationSecond(),
            input.resultDecisionMethod(),
            input.requiredStreamTypes() == null ? null : input.requiredStreamTypes().stream()
                .map(StringNormalization::normalizeCode)
                .toList(),
            input.streamTypePermission() == null ? null : StringNormalization.normalizeCode(input.streamTypePermission()),
            input.deliveryMode() == null ? null : StringNormalization.normalizeCode(input.deliveryMode()),
            input.requiresOtp(),
            input.schoolRoomId()
,
            input.aiConfidenceThresholdPercent()
        );
    }

    private Assessment createAssessment(
            SchoolClass schoolClass,
            CreateClassTestCommand command,
            UUID currentUserId,
            Instant now) {
        var code = "CT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        var openAt = DateMapper.toInstant(command.openAt());
        var closeAt = DateMapper.toInstant(command.closeAt());
        Integer requestedMaxAttempt = command.maxAttempt();
        int maxAttempt = requestedMaxAttempt == null ? 1 : requestedMaxAttempt;
        var streamConfig = assessmentStreamConfigResolver.resolve(
            command.requiredStreamTypes(), command.streamTypePermission());
        // Bài trên lớp không gắn blueprint lúc tạo: giáo viên chọn cách soạn từng mã đề ở trang chi
        // tiết. Nếu sau đó muốn dùng blueprint dùng chung thì gắn qua ChangeClassTestBlueprintUseCase.
        var classTest = new Assessment(
            null,
            null,
            code,
            command.name(),
            command.description(),
            schoolClass.getSchoolId(),
            schoolClass.getLanguageId(),
            AssessmentKind.CLASS_TEST,
            // Bài trên lớp thi ngay trong phòng, chỉ khác nhau ở máy dùng để làm bài: máy của nhà
            // trường (LAB) hay máy học sinh mang theo (STUDENT_DEVICE — mặc định).
            AssessmentDeliveryModeSupport.parseOrDefault(command.deliveryMode(), AssessmentDeliveryMode.STUDENT_DEVICE),
            AssessmentStatus.DRAFT,
            maxAttempt,
            command.assessmentTimeDurationSecond(),
            command.resultDecisionMethod() == null ? AssessmentResultDecisionMethod.HIGHEST : command.resultDecisionMethod(),
            openAt,
            closeAt,
            command.assessmentPolicyId(),
            // Bài trên lớp mặc định vào thẳng, giáo viên bật OTP nếu muốn siết như kỳ thi tập trung.
            command.requiresOtp() != null && command.requiresOtp(),
            now,
            // Cả hai cùng NULL nghĩa là không giám sát bằng stream — DB cho phép
            // (chk_assessments_required_stream_type_and_stream_type_permission_valid).
            streamConfig.requiredStreamType(),
            streamConfig.streamTypePermission(),
            now,
            currentUserId,
            currentUserId
        );
        // Gán qua setter: constructor Assessment đã rất dài và có nhiều biến thể, thêm tham số vào là
        // phải sửa mọi nơi gọi chỉ để mang một trường tuỳ chọn.
        classTest.setAiConfidenceThresholdPercent(command.aiConfidenceThresholdPercent());
        return assessmentRepository.save(classTest);
    }

    /**
     * Ca thi của bài trên lớp bám đúng khung mở/đóng bài. Phòng có thể để trống ở đây và chọn sau
     * qua updateAssessmentSchedule, nhưng {@code UpdateAssessmentStatusUseCase} sẽ chặn SCHEDULE nếu vẫn thiếu:
     * bài kiểm tra trên lớp cũng thi trong phòng, không cho làm ở nhà.
     */
    private AssessmentSchedule createDraftSchedule(Assessment assessment, UUID schoolRoomId, UUID currentUserId, Instant now) {
        if (assessment.getOpenAt() == null || assessment.getCloseAt() == null) {
            throw new IllegalStateException("Bài kiểm tra trên lớp phải có thời gian mở bài và đóng bài");
        }
        if (schoolRoomId != null) {
            assessmentScheduleRoomValidator.validateForNewSchedule(
                schoolRoomId, assessment, assessment.getOpenAt(), assessment.getCloseAt());
        }

        var schedule = AssessmentSchedule.createFresh(
            assessment.getId(),
            schoolRoomId,
            assessment.getOpenAt(),
            assessment.getCloseAt(),
            currentUserId,
            now
        );
        return assessmentScheduleRepository.save(schedule);
    }

    /**
     * Nạp toàn bộ học sinh active của lớp vào danh sách dự thi, CHƯA xếp ca và CHƯA gán đề — giống
     * hệt kỳ thi tập trung. Chọn lớp chỉ là bước "đưa vào tab học sinh"; xếp ca (và đề đi kèm) là
     * bước riêng qua endpoint assign-schedule / auto-fill.
     */
    private int assignCandidates(
            Assessment assessment,
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
        // roster là 2000. Giống hệt ImportAssessmentCandidatesFromClassUseCase.
        var studentIds = userRoleQueryRepository.findUserIdsByRoleCode(activeUserIds, SchoolRoleCodes.STUDENT);

        var candidates = new ArrayList<AssessmentCandidate>();
        for (var userId : activeUserIds) {
            if (!studentIds.contains(userId)) {
                continue;
            }
            candidates.add(AssessmentCandidate.createFresh(assessment.getId(), userId, currentUserId, now));
        }
        return assessmentCandidateRepository.saveAll(candidates).size();
    }

    private void validateOpenClose(String openAt, String closeAt, UUID schoolId) {
        var open = DateMapper.toInstant(openAt);
        var close = DateMapper.toInstant(closeAt);
        if (open == null || close == null) {
            throw new IllegalStateException("Bài kiểm tra trên lớp phải có thời gian mở bài và đóng bài");
        }
        if (!open.isBefore(close)) {
            throw new IllegalStateException("Thời gian mở bài phải nhỏ hơn thời gian đóng bài");
        }
        subscriptionPeriodGuardService.requireWithinSubscriptionPeriod(schoolId, open, close);
    }
}
