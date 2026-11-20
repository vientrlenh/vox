package com.sep.vox.application.usecase.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.CreateClassTestCommand;
import com.sep.vox.application.port.input.service.AssessmentPolicyValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleRoomValidator;
import com.sep.vox.application.port.input.service.AssessmentStreamConfigResolver;
import com.sep.vox.application.port.input.service.SchoolSubscriptionActiveGuardService;
import com.sep.vox.application.port.input.service.SubscriptionPeriodGuardService;
import com.sep.vox.application.port.input.usecase.assessment.CreateClassTestUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.assessment.CreateClassTestResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentDeliveryMode;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.AssessmentStreamTypePermission;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.valueobject.ClassCode;

/**
 * Bài kiểm tra trên lớp giờ đi cùng quy trình với kỳ thi tập trung: tạo bài chỉ dựng "vỏ" (chưa có
 * mã đề nào), còn soạn đề / chọn phòng / xếp ca / lên lịch là các bước riêng sau đó.
 */
class CreateClassTestSetupTests {

    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID CLASS_ID = UUID.randomUUID();
    private static final UUID POLICY_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    private AssessmentRepository assessmentRepository;
    private AssessmentScheduleRepository assessmentScheduleRepository;
    private AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private SubscriptionPeriodGuardService subscriptionPeriodGuardService;
    private CreateClassTestUseCase useCase;

    @BeforeEach
    void setUp() {
        SchoolClassRepository schoolClassRepository = mock(SchoolClassRepository.class);
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        AssessmentPolicyRepository assessmentPolicyRepository = mock(AssessmentPolicyRepository.class);
        UserContextPort userContextPort = mock(UserContextPort.class);
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        assessmentScheduleProctorRepository = mock(AssessmentScheduleProctorRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        subscriptionPeriodGuardService = mock(SubscriptionPeriodGuardService.class);

        useCase = new CreateClassTestUseCase(
            schoolClassRepository,
            schoolClassUserRepository,
            userRoleQueryRepository,
            assessmentRepository,
            assessmentScheduleRepository,
            assessmentScheduleProctorRepository,
            mock(AssessmentMemberRepository.class),
            assessmentCandidateRepository,
            new AssessmentPolicyValidator(assessmentPolicyRepository),
            new AssessmentStreamConfigResolver(),
            mock(AssessmentScheduleRoomValidator.class),
            // A real validator on a mocked proctor repo: the default is "the teacher is free", and
            // a dedicated test below switches on the schedule-conflict flag to check the rule.
            new AssessmentScheduleProctorConflictValidator(assessmentScheduleProctorRepository),
            subscriptionPeriodGuardService,
            userContextPort,
            mock(SchoolSubscriptionActiveGuardService.class)
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(TEACHER_ID);
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(TEACHER_ID)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), TEACHER_ID, UUID.randomUUID(), Instant.now(), "TEACHER", "Giáo viên")
        ));
        when(schoolClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(schoolClass()));
        when(schoolClassUserRepository.findByUserIdAndSchoolClassId(TEACHER_ID, CLASS_ID))
            .thenReturn(Optional.of(new SchoolClassUser(TEACHER_ID, CLASS_ID, true, Instant.now(), Instant.now(), TEACHER_ID)));
        when(assessmentPolicyRepository.findById(POLICY_ID))
            .thenReturn(Optional.of(policy()));

        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> {
            Assessment assessment = inv.getArgument(0);
            assessment.setId(UUID.randomUUID());
            return assessment;
        });
        when(assessmentScheduleRepository.save(any(AssessmentSchedule.class))).thenAnswer(inv -> {
            AssessmentSchedule schedule = inv.getArgument(0);
            schedule.setId(UUID.randomUUID());
            return schedule;
        });
        when(assessmentCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<AssessmentCandidate> arg = inv.getArgument(0);
            return List.copyOf(arg);
        });

        // The class has exactly one active student.
        when(schoolClassUserRepository.findBySchoolClassId(CLASS_ID, 1, 2000)).thenReturn(new PageResult<>(
            List.of(new SchoolClassUser(STUDENT_ID, CLASS_ID, true, Instant.now(), null, TEACHER_ID)),
            1, 2000, 1, 1));
        // The roles of the whole class are asked for with ONE batch query, not one student at a
        // time.
        when(userRoleQueryRepository.findUserIdsByRoleCode(List.of(STUDENT_ID), SchoolRoleCodes.STUDENT))
            .thenReturn(Set.of(STUDENT_ID));
    }

    @Test
    void should_stop_at_draft_so_teacher_can_author_papers_and_schedule() {
        CreateClassTestResponse result = useCase.execute(command(null, null, null, null));

        assertThat(result.assessment().status()).isEqualTo(AssessmentStatus.DRAFT);
    }

    /**
     * Soi hạn gói theo trường của LỚP chứ không phải trường lấy từ token người gọi: giáo viên có thể
     * dạy lớp thuộc trường khác, và bài kiểm tra tiêu quota của trường sở hữu lớp.
     */
    @Test
    void should_check_subscription_period_against_school_of_the_class() {
        useCase.execute(command(null, null, null, null));

        verify(subscriptionPeriodGuardService).requireWithinSubscriptionPeriod(
            eq(SCHOOL_ID),
            eq(Instant.parse("2026-08-10T08:00:00Z")),
            eq(Instant.parse("2026-08-10T09:00:00Z")));
    }

    @Test
    void should_reject_when_window_falls_outside_subscription_period() {
        doThrow(new IllegalStateException("Thời gian đóng bài phải nằm trong hạn gói dịch vụ của trường"))
            .when(subscriptionPeriodGuardService).requireWithinSubscriptionPeriod(any(), any(), any());

        assertThatThrownBy(() -> useCase.execute(command(null, null, null, null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("hạn gói dịch vụ");
        // Chặn trước khi dựng bất cứ thứ gì: tạo bài trên lớp còn kéo theo ca thi, CHAIR và cả danh
        // sách thí sinh, để lọt là phải dọn tay.
        verify(assessmentRepository, never()).save(any());
        verify(assessmentCandidateRepository, never()).saveAll(anyCollection());
    }

    /**
     * Drafting the paper moves entirely to the detail page, so creating the test produces no paper
     * variant.
     */
    @Test
    void should_not_create_any_paper_at_creation_time() {
        CreateClassTestResponse result = useCase.execute(command(null, null, null, null));

        assertThat(result.assessment().blueprintId()).isNull();
        assertThat(result.assessment().blueprintVersionId()).isNull();
    }

    @Test
    void should_leave_candidates_without_schedule_and_paper() {
        useCase.execute(command(null, null, null, null));

        ArgumentCaptor<Collection<AssessmentCandidate>> captor = ArgumentCaptor.captor();
        verify(assessmentCandidateRepository).saveAll(captor.capture());

        Collection<AssessmentCandidate> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.iterator().next().getScheduleId()).isNull();
        assertThat(saved.iterator().next().getAssignedPaperId()).isNull();
    }

    @Test
    void should_register_creating_teacher_as_default_proctor() {
        useCase.execute(command(null, null, null, null));

        ArgumentCaptor<AssessmentScheduleProctor> captor = ArgumentCaptor.forClass(AssessmentScheduleProctor.class);
        verify(assessmentScheduleProctorRepository).save(captor.capture());
        assertThat(captor.getValue().getTeacherId()).isEqualTo(TEACHER_ID);
    }

    /**
     * Luật "không gác hai ca trùng giờ" quét toàn trường, nên bài trên lớp tự gán giáo viên tạo bài
     * làm giám thị cũng có thể đâm vào một ca của kỳ thi tập trung.
     */
    @Test
    void should_reject_when_creating_teacher_already_proctors_an_overlapping_schedule() {
        when(assessmentScheduleProctorRepository.existsOverlappingAssignment(
            eq(TEACHER_ID), any(Instant.class), any(Instant.class), any())).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command(null, null, null, null)))
            .isInstanceOf(DuplicatedException.class);
        verify(assessmentScheduleProctorRepository, never()).save(any(AssessmentScheduleProctor.class));
    }

    @Test
    void should_store_requested_stream_config() {
        useCase.execute(command(List.of("CAMERA", "SCREEN"), "ALL", null, null));

        Assessment assessment = savedAssessment();
        assertThat(assessment.getRequiredStreamType()).isEqualTo(AssessmentRequiredStreamType.CAMERA_AND_SCREEN);
        assertThat(assessment.getStreamTypePermission()).isEqualTo(AssessmentStreamTypePermission.ALL);
    }

    @Test
    void should_leave_stream_config_empty_when_not_requested() {
        useCase.execute(command(null, null, null, null));

        Assessment assessment = savedAssessment();
        assertThat(assessment.getRequiredStreamType()).isNull();
        assertThat(assessment.getStreamTypePermission()).isNull();
    }

    @Test
    void should_reject_stream_permission_without_stream_types() {
        assertThatThrownBy(() -> useCase.execute(command(null, "ALL", null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không thể đặt quyền stream khi không yêu cầu stream nào");
    }

    @Test
    void should_default_to_student_device_when_delivery_mode_omitted() {
        useCase.execute(command(null, null, null, null));

        assertThat(savedAssessment().getDeliveryMode()).isEqualTo(AssessmentDeliveryMode.STUDENT_DEVICE);
    }

    @Test
    void should_accept_school_device_delivery_mode() {
        useCase.execute(command(null, null, "LAB", null));

        assertThat(savedAssessment().getDeliveryMode()).isEqualTo(AssessmentDeliveryMode.LAB);
    }

    @Test
    void should_not_require_otp_by_default() {
        useCase.execute(command(null, null, null, null));

        assertThat(savedAssessment().isRequiresOtp()).isFalse();
    }

    @Test
    void should_require_otp_when_teacher_asks_for_it() {
        useCase.execute(command(null, null, null, true));

        assertThat(savedAssessment().isRequiresOtp()).isTrue();
    }

    @Test
    void should_create_schedule_without_room_when_room_not_chosen_yet() {
        useCase.execute(command(null, null, null, null));

        ArgumentCaptor<AssessmentSchedule> captor = ArgumentCaptor.forClass(AssessmentSchedule.class);
        verify(assessmentScheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getSchoolRoomId()).isNull();
    }

    @Test
    void should_skip_non_student_class_members() {
        UUID teacherOnlyMember = UUID.randomUUID();
        when(schoolClassUserRepository.findBySchoolClassId(CLASS_ID, 1, 2000)).thenReturn(new PageResult<>(
            List.of(new SchoolClassUser(teacherOnlyMember, CLASS_ID, true, Instant.now(), null, TEACHER_ID)),
            1, 2000, 1, 1));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(teacherOnlyMember)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), teacherOnlyMember, UUID.randomUUID(), Instant.now(), "TEACHER", "Giáo viên")
        ));

        CreateClassTestResponse result = useCase.execute(command(null, null, null, null));

        assertThat(result.candidateCount()).isZero();
    }

    /**
     * Creating a test must no longer schedule it automatically -- the teacher must click SCHEDULE
     * after choosing a room and arranging sessions.
     */
    @Test
    void should_not_publish_schedule_at_creation_time() {
        useCase.execute(command(null, null, null, null));

        ArgumentCaptor<AssessmentSchedule> captor = ArgumentCaptor.forClass(AssessmentSchedule.class);
        verify(assessmentScheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus())
            .isEqualTo(com.sep.vox.domain.model.assessment.AssessmentScheduleStatus.DRAFT);
        verify(assessmentScheduleRepository, never()).findByAssessmentId(any(UUID.class));
    }

    private Assessment savedAssessment() {
        ArgumentCaptor<Assessment> captor = ArgumentCaptor.forClass(Assessment.class);
        verify(assessmentRepository).save(captor.capture());
        return captor.getValue();
    }

    private CreateClassTestCommand command(
            List<String> requiredStreamTypes,
            String streamTypePermission,
            String deliveryMode,
            Boolean requiresOtp) {
        return new CreateClassTestCommand(
            CLASS_ID,
            "Kiểm tra 15 phút",
            null,
            "2026-08-10T08:00:00Z",
            "2026-08-10T09:00:00Z",
            POLICY_ID,
            1,
            600,
            null,
            requiredStreamTypes,
            streamTypePermission,
            deliveryMode,
            requiresOtp,
            null,
            null
        );
    }

    private SchoolClass schoolClass() {
        return new SchoolClass(
            CLASS_ID, SCHOOL_ID, UUID.randomUUID(), UUID.randomUUID(),
            new ClassCode("10A1"), "Lớp 10A1", null, SchoolClassStatus.ACTIVE,
            Instant.now(), Instant.now(), TEACHER_ID, TEACHER_ID);
    }

    private AssessmentPolicy policy() {
        AssessmentPolicy policy = new AssessmentPolicy();
        policy.setId(POLICY_ID);
        policy.setSchoolId(SCHOOL_ID);
        policy.setStatus(AssessmentPolicyStatus.PUBLISHED);
        return policy;
    }
}
