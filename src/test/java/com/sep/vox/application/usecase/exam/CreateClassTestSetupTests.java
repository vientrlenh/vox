package com.sep.vox.application.usecase.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.command.ClassTestQuestionCommand;
import com.sep.vox.application.port.input.command.ClassTestSectionCommand;
import com.sep.vox.application.port.input.command.CreateClassTestCommand;
import com.sep.vox.application.port.input.service.ExamScheduleRoomValidator;
import com.sep.vox.application.port.input.service.ExamStreamConfigResolver;
import com.sep.vox.application.port.input.service.ExamTimeQuotaGuardService;
import com.sep.vox.application.port.input.service.RecalculateExamTimeDurationService;
import com.sep.vox.application.port.input.usecase.exam.CreateClassTestUseCase;
import com.sep.vox.application.port.input.usecase.exam.ExamQuestionSecureLockService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamDeliveryMode;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamRequiredStreamType;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleProctor;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.exam.ExamStreamTypePermission;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.valueobject.ClassCode;

/**
 * Bài kiểm tra trên lớp giờ đi cùng quy trình với kỳ thi tập trung: chọn lớp chỉ nạp học sinh vào
 * danh sách dự thi, còn phòng thi / xếp ca / lên lịch là các bước riêng sau đó.
 */
class CreateClassTestSetupTests {

    private static final UUID TEACHER_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID CLASS_ID = UUID.randomUUID();
    private static final UUID POLICY_ID = UUID.randomUUID();
    private static final UUID QUESTION_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    private ExamRepository examRepository;
    private ExamScheduleRepository examScheduleRepository;
    private ExamScheduleProctorRepository examScheduleProctorRepository;
    private ExamCandidateRepository examCandidateRepository;
    private SchoolClassUserRepository schoolClassUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private CreateClassTestUseCase useCase;

    @BeforeEach
    void setUp() {
        var schoolClassRepository = mock(SchoolClassRepository.class);
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        var questionRepository = mock(QuestionRepository.class);
        var assessmentPolicyRepository = mock(AssessmentPolicyRepository.class);
        var userContextPort = mock(UserContextPort.class);
        examRepository = mock(ExamRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        examScheduleProctorRepository = mock(ExamScheduleProctorRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        var examPaperRepository = mock(ExamPaperRepository.class);
        var examPaperSectionRepository = mock(ExamPaperSectionRepository.class);

        useCase = new CreateClassTestUseCase(
            schoolClassRepository,
            schoolClassUserRepository,
            userRoleQueryRepository,
            questionRepository,
            mock(QuestionCollaboratorRepository.class),
            mock(ExamBlueprintRepository.class),
            mock(ExamBlueprintVersionRepository.class),
            mock(ExamBlueprintSectionRepository.class),
            mock(ExamBlueprintSlotRepository.class),
            examRepository,
            examPaperRepository,
            examPaperSectionRepository,
            mock(ExamPaperItemRepository.class),
            examScheduleRepository,
            examScheduleProctorRepository,
            mock(ExamMemberRepository.class),
            examCandidateRepository,
            assessmentPolicyRepository,
            mock(ExamQuestionSecureLockService.class),
            mock(ExamTimeQuotaGuardService.class),
            mock(RecalculateExamTimeDurationService.class),
            new ExamStreamConfigResolver(),
            mock(ExamScheduleRoomValidator.class),
            userContextPort
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
        when(questionRepository.findAccessibleById(any(UUID.class), any(UUID.class), any(UUID.class), anyBoolean(), anyBoolean()))
            .thenReturn(Optional.of(question()));

        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> {
            Exam exam = inv.getArgument(0);
            exam.setId(UUID.randomUUID());
            return exam;
        });
        when(examPaperRepository.save(any(ExamPaper.class))).thenAnswer(inv -> {
            ExamPaper paper = inv.getArgument(0);
            paper.setId(UUID.randomUUID());
            return paper;
        });
        when(examScheduleRepository.save(any(ExamSchedule.class))).thenAnswer(inv -> {
            ExamSchedule schedule = inv.getArgument(0);
            schedule.setId(UUID.randomUUID());
            return schedule;
        });
        when(examPaperSectionRepository.save(any())).thenAnswer(inv -> {
            var section = inv.getArgument(0, com.sep.vox.domain.model.exam.ExamPaperSection.class);
            section.setId(UUID.randomUUID());
            return section;
        });
        when(examCandidateRepository.saveAll(anyCollection())).thenAnswer(inv -> {
            Collection<ExamCandidate> arg = inv.getArgument(0);
            return List.copyOf(arg);
        });

        // Lớp có đúng một học sinh active.
        when(schoolClassUserRepository.findBySchoolClassId(CLASS_ID, 1, 2000)).thenReturn(new PageResult<>(
            List.of(new SchoolClassUser(STUDENT_ID, CLASS_ID, true, Instant.now(), null, TEACHER_ID)),
            1, 2000, 1, 1));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(STUDENT_ID)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), STUDENT_ID, UUID.randomUUID(), Instant.now(), "STUDENT", "Học sinh")
        ));
    }

    @Test
    void should_stop_at_draft_so_teacher_can_pick_room_and_schedule() {
        var result = useCase.execute(command(null, null, null, null));

        assertThat(result.exam().status()).isEqualTo(ExamStatus.DRAFT.name());
    }

    @Test
    void should_leave_candidates_without_schedule_and_paper() {
        useCase.execute(command(null, null, null, null));

        var captor = ArgumentCaptor.forClass(Collection.class);
        verify(examCandidateRepository).saveAll(captor.capture());

        @SuppressWarnings("unchecked")
        Collection<ExamCandidate> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.iterator().next().getScheduleId()).isNull();
        assertThat(saved.iterator().next().getAssignedPaperId()).isNull();
    }

    @Test
    void should_register_creating_teacher_as_default_proctor() {
        useCase.execute(command(null, null, null, null));

        var captor = ArgumentCaptor.forClass(ExamScheduleProctor.class);
        verify(examScheduleProctorRepository).save(captor.capture());
        assertThat(captor.getValue().getTeacherId()).isEqualTo(TEACHER_ID);
    }

    @Test
    void should_store_requested_stream_config() {
        useCase.execute(command(List.of("CAMERA", "SCREEN"), "ALL", null, null));

        var exam = savedExam();
        assertThat(exam.getRequiredStreamType()).isEqualTo(ExamRequiredStreamType.CAMERA_AND_SCREEN);
        assertThat(exam.getStreamTypePermission()).isEqualTo(ExamStreamTypePermission.ALL);
    }

    @Test
    void should_leave_stream_config_empty_when_not_requested() {
        useCase.execute(command(null, null, null, null));

        var exam = savedExam();
        assertThat(exam.getRequiredStreamType()).isNull();
        assertThat(exam.getStreamTypePermission()).isNull();
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

        assertThat(savedExam().getDeliveryMode()).isEqualTo(ExamDeliveryMode.STUDENT_DEVICE);
    }

    @Test
    void should_accept_school_device_delivery_mode() {
        useCase.execute(command(null, null, "LAB", null));

        assertThat(savedExam().getDeliveryMode()).isEqualTo(ExamDeliveryMode.LAB);
    }

    @Test
    void should_not_require_otp_by_default() {
        useCase.execute(command(null, null, null, null));

        assertThat(savedExam().isRequiresOtp()).isFalse();
    }

    @Test
    void should_require_otp_when_teacher_asks_for_it() {
        useCase.execute(command(null, null, null, true));

        assertThat(savedExam().isRequiresOtp()).isTrue();
    }

    @Test
    void should_create_schedule_without_room_when_room_not_chosen_yet() {
        useCase.execute(command(null, null, null, null));

        var captor = ArgumentCaptor.forClass(ExamSchedule.class);
        verify(examScheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getSchoolRoomId()).isNull();
    }

    @Test
    void should_skip_non_student_class_members() {
        var teacherOnlyMember = UUID.randomUUID();
        when(schoolClassUserRepository.findBySchoolClassId(CLASS_ID, 1, 2000)).thenReturn(new PageResult<>(
            List.of(new SchoolClassUser(teacherOnlyMember, CLASS_ID, true, Instant.now(), null, TEACHER_ID)),
            1, 2000, 1, 1));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(teacherOnlyMember)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), teacherOnlyMember, UUID.randomUUID(), Instant.now(), "TEACHER", "Giáo viên")
        ));

        var result = useCase.execute(command(null, null, null, null));

        assertThat(result.candidateCount()).isZero();
    }

    /** Tạo bài không được tự lên lịch nữa — giáo viên phải bấm SCHEDULE sau khi chọn phòng, xếp ca. */
    @Test
    void should_not_publish_schedule_at_creation_time() {
        useCase.execute(command(null, null, null, null));

        var captor = ArgumentCaptor.forClass(ExamSchedule.class);
        verify(examScheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus())
            .isEqualTo(com.sep.vox.domain.model.exam.ExamScheduleStatus.DRAFT);
        verify(examScheduleRepository, never()).findByExamId(any(UUID.class));
    }

    private Exam savedExam() {
        var captor = ArgumentCaptor.forClass(Exam.class);
        verify(examRepository).save(captor.capture());
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
            List.of(new ClassTestSectionCommand(
                "Phần 1",
                null,
                BigDecimal.ONE,
                List.of(new ClassTestQuestionCommand(QUESTION_ID, BigDecimal.ONE))
            )),
            null,
            null,
            1,
            600,
            null,
            requiredStreamTypes,
            streamTypePermission,
            deliveryMode,
            requiresOtp,
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
        var policy = new AssessmentPolicy();
        policy.setId(POLICY_ID);
        policy.setSchoolId(SCHOOL_ID);
        policy.setStatus(AssessmentPolicyStatus.PUBLISHED);
        return policy;
    }

    private Question question() {
        var question = new Question();
        question.setId(QUESTION_ID);
        question.setCreatedBy(TEACHER_ID);
        question.setSharing(QuestionSharing.SCHOOL_SHARED);
        question.setPreparationTimeSeconds(30);
        question.setMaxResponseSeconds(60);
        return question;
    }
}
