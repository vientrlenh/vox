package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.school.SchoolClassUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateClassTestCommand;
import com.sep.vox.application.port.input.service.AssessmentPolicyValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleRoomValidator;
import com.sep.vox.application.port.input.service.AssessmentStreamConfigResolver;
import com.sep.vox.application.port.input.service.SchoolSubscriptionActiveGuardService;
import com.sep.vox.application.port.input.service.SubscriptionPeriodGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.assessment.CreateClassTestResponse;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentDeliveryMode;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMember;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.ResultDecisionMethod;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.service.assessment.AssessmentDeliveryModeSupport;

/**
 * Creates the "shell" of a class test: DRAFT assessment + draft session + CHAIR + the list of students of the class.
 *
 * <p><b>No paper is created here.</b> Authoring papers (direct questions / blueprint / copy) is a separate step
 * on the detail page through {@code CreateAssessmentPaperUseCase}, exactly like a centralized assessment -- that way the teacher
 * can author several paper variants for the same class test and then assign them to students.
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
        CreateClassTestCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        if (userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
                .noneMatch(role -> "TEACHER".equals(role.roleCode()))) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        SchoolClass schoolClass = schoolClassRepository.findById(command.schoolClassId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học"));

        SchoolClassUser membership = schoolClassUserRepository.findByUserIdAndSchoolClassId(currentUserId, schoolClass.getId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));
        if (!membership.isActive()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        schoolSubscriptionActiveGuardService.requireActiveForSchool(schoolClass.getSchoolId(), "tạo Class Test");

        assessmentPolicyValidator.requirePublishedInSchool(
            command.assessmentPolicyId(), schoolClass.getSchoolId());
        validateOpenClose(command.openAt(), command.closeAt(), schoolClass.getSchoolId());

        Instant now = Instant.now();
        Assessment assessment = createAssessment(schoolClass, command, currentUserId, now);
        AssessmentSchedule schedule = createDraftSchedule(assessment, command.schoolRoomId(), currentUserId, now);
        return finishSetup(assessment, schedule, schoolClass, currentUserId, now);
    }

    /**
     * The steps after the assessment has been built.
     *
     * <p>The test stops at DRAFT: the teacher still has to author papers, choose a room and place students into the session (choosing
     * a class only loads the student list, it does not place them in a session), exactly like a centralized assessment.
     * {@code validatePlanLimits} therefore runs when SCHEDULE is pressed rather than at creation.
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
        int candidateCount = assignCandidates(assessment, schoolClass.getId(), currentUserId, now);
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
        String code = "CT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        Instant openAt = DateMapper.toInstant(command.openAt());
        Instant closeAt = DateMapper.toInstant(command.closeAt());
        Integer requestedMaxAttempt = command.maxAttempt();
        int maxAttempt = requestedMaxAttempt == null ? 1 : requestedMaxAttempt;
        AssessmentStreamConfigResolver.StreamConfig streamConfig = assessmentStreamConfigResolver.resolve(
            command.requiredStreamTypes(), command.streamTypePermission());
        // Bài trên lớp không gắn blueprint lúc tạo: giáo viên chọn cách soạn từng mã đề ở trang chi
        // tiết. Nếu sau đó muốn dùng blueprint dùng chung thì gắn qua ChangeClassTestBlueprintUseCase.
        Assessment classTest = new Assessment(
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
            command.resultDecisionMethod() == null ? ResultDecisionMethod.HIGHEST : command.resultDecisionMethod(),
            openAt,
            closeAt,
            command.assessmentPolicyId(),
            // Bài trên lớp mặc định vào thẳng, giáo viên bật OTP nếu muốn siết như kỳ thi tập trung.
            command.requiresOtp() != null && command.requiresOtp(),
            now,
            // Both being NULL means no stream proctoring -- the DB allows it
            // (chk_assessments_required_stream_type_and_stream_type_permission_valid).
            streamConfig.requiredStreamType(),
            streamConfig.streamTypePermission(),
            now,
            currentUserId,
            currentUserId
        );
        // Assigned through the setter: the Assessment constructor is already very long and has many variants, and adding a parameter
        // means editing every caller just to carry one optional field.
        classTest.setAiConfidenceThresholdPercent(command.aiConfidenceThresholdPercent());
        return assessmentRepository.save(classTest);
    }

    /**
     * The session of a class test follows exactly the open/close window of the test. The room may be left empty here and chosen later
     * through updateAssessmentSchedule, but {@code UpdateAssessmentStatusUseCase} blocks SCHEDULE if it is still missing:
     * a class test is also taken in a room, it is not allowed to be done at home.
     */
    private AssessmentSchedule createDraftSchedule(Assessment assessment, UUID schoolRoomId, UUID currentUserId, Instant now) {
        if (assessment.getOpenAt() == null || assessment.getCloseAt() == null) {
            throw new IllegalStateException("Bài kiểm tra trên lớp phải có thời gian mở bài và đóng bài");
        }
        if (schoolRoomId != null) {
            assessmentScheduleRoomValidator.validateForNewSchedule(
                schoolRoomId, assessment, assessment.getOpenAt(), assessment.getCloseAt());
        }

        AssessmentSchedule schedule = AssessmentSchedule.createFresh(
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
        List<SchoolClassUser> roster = schoolClassUserRepository.findBySchoolClassId(schoolClassId, 1, MAX_CLASS_ROSTER_SIZE).content();

        List<UUID> activeUserIds = roster.stream()
            .filter(classUser -> classUser.isActive())
            .map(classUser -> classUser.getUserId())
            .distinct()
            .toList();

        // ONE query for the whole class instead of asking each student's role -- a class of 40 would be 40 queries, and the
        // roster cap is 2000. Exactly the same as ImportAssessmentCandidatesFromClassUseCase.
        Set<UUID> studentIds = userRoleQueryRepository.findUserIdsByRoleCode(activeUserIds, SchoolRoleCodes.STUDENT);

        List<AssessmentCandidate> candidates = new ArrayList<AssessmentCandidate>();
        for (UUID userId : activeUserIds) {
            if (!studentIds.contains(userId)) {
                continue;
            }
            candidates.add(AssessmentCandidate.createFresh(assessment.getId(), userId, currentUserId, now));
        }
        return assessmentCandidateRepository.saveAll(candidates).size();
    }

    private void validateOpenClose(String openAt, String closeAt, UUID schoolId) {
        Instant open = DateMapper.toInstant(openAt);
        Instant close = DateMapper.toInstant(closeAt);
        if (open == null || close == null) {
            throw new IllegalStateException("Bài kiểm tra trên lớp phải có thời gian mở bài và đóng bài");
        }
        if (!open.isBefore(close)) {
            throw new IllegalStateException("Thời gian mở bài phải nhỏ hơn thời gian đóng bài");
        }
        subscriptionPeriodGuardService.requireWithinSubscriptionPeriod(schoolId, open, close);
    }
}
