package com.sep.vox.application.usecase.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.UpdateExamDeliveryModeCommand;
import com.sep.vox.application.port.input.usecase.exam.UpdateExamDeliveryModeUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamDeliveryMode;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class UpdateExamDeliveryModeUseCaseTests {

    private ExamRepository examRepository;
    private ExamPaperRepository examPaperRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private UpdateExamDeliveryModeUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examPaperRepository = mock(ExamPaperRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new UpdateExamDeliveryModeUseCase(
            examRepository,
            examPaperRepository,
            examMemberRepository,
            schoolUserRepository,
            userRoleQueryRepository,
            userContextPort
        );
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
    }

    @Test
    void should_update_delivery_mode_for_class_test_as_chair() {
        var exam = exam(ExamKind.CLASS_TEST, ExamStatus.DRAFT);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.save(exam)).thenAnswer(inv -> inv.getArgument(0));
        var lockedPaper = mock(ExamPaper.class);
        when(lockedPaper.getStatus()).thenReturn(ExamPaperStatus.LOCKED);
        when(examPaperRepository.findByExamId(examId)).thenReturn(List.of(lockedPaper));

        var result = useCase.execute(new UpdateExamDeliveryModeCommand(examId, "STUDENT_DEVICE"));

        assertThat(exam.getDeliveryMode()).isEqualTo(ExamDeliveryMode.STUDENT_DEVICE);
        assertThat(result.deliveryMode()).isEqualTo("STUDENT_DEVICE");
        assertThat(result.papersLocked()).isTrue();
        verify(examRepository).save(exam);
    }

    @Test
    void should_reject_when_exam_is_centralized() {
        var exam = exam(ExamKind.CENTRALIZED, ExamStatus.DRAFT);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamDeliveryModeCommand(examId, "LAB")))
            .isInstanceOf(IllegalStateException.class);
        verify(examRepository, never()).save(exam);
    }

    @Test
    void should_reject_when_exam_already_in_progress() {
        var exam = exam(ExamKind.CLASS_TEST, ExamStatus.IN_PROGRESS);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamDeliveryModeCommand(examId, "STUDENT_DEVICE")))
            .isInstanceOf(IllegalStateException.class);
        verify(examRepository, never()).save(exam);
    }

    @Test
    void should_reject_when_not_authorized() {
        var exam = exam(ExamKind.CLASS_TEST, ExamStatus.DRAFT);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamDeliveryModeCommand(examId, "LAB")))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_reject_invalid_delivery_mode() {
        assertThatThrownBy(() -> useCase.execute(new UpdateExamDeliveryModeCommand(examId, "INVALID")))
            .isInstanceOf(IllegalArgumentException.class);
        verify(examRepository, never()).findById(eq(examId));
    }

    private Exam exam(ExamKind kind, ExamStatus status) {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        exam.setKind(kind);
        exam.setStatus(status);
        return exam;
    }
}
