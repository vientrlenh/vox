package com.sep.vox.application.usecase.exampaper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.UpdateExamPaperStatusCommand;
import com.sep.vox.application.port.input.service.ExamPaperAutoAssigner;
import com.sep.vox.application.port.input.service.ExamTimeQuotaGuardService;
import com.sep.vox.application.port.input.usecase.exampaper.UpdateExamPaperStatusUseCase;
import com.sep.vox.application.port.input.service.ExamPaperAuthoringAccessService;
import com.sep.vox.domain.model.exam.ExamMember;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Phân đề đòi mọi mã đề phải LOCKED. Bài trên lớp lại không có tách bạch tác giả / người duyệt —
 * giáo viên vừa là CHAIR vừa là người soạn mọi mã đề — nên luồng duyệt 3 bước của kỳ thi tập trung
 * ({@code requireNotAuthor}) sẽ khoá chết giáo viên khỏi chính đề của mình. Ở đây khoá đề là một
 * bước DRAFT → LOCKED.
 */
class LockClassTestPaperTests {

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID PAPER_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID TEACHER_ID = UUID.randomUUID();

    private ExamPaperRepository examPaperRepository;
    private ExamPaperItemRepository examPaperItemRepository;
    private ExamMemberRepository examMemberRepository;
    private UpdateExamPaperStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        examPaperRepository = mock(ExamPaperRepository.class);
        examPaperItemRepository = mock(ExamPaperItemRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        var examRepository = mock(ExamRepository.class);
        var userContextPort = mock(UserContextPort.class);

        useCase = new UpdateExamPaperStatusUseCase(
            examPaperRepository,
            examPaperItemRepository,
            examRepository,
            mock(ExamCandidateRepository.class),
            new ExamPaperAuthoringAccessService(
                examMemberRepository, mock(SchoolUserRepository.class), mock(UserRoleQueryRepository.class)),
            mock(ExamTimeQuotaGuardService.class),
            mock(ExamPaperAutoAssigner.class),
            userContextPort
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(TEACHER_ID);
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(classTest()));
        when(examMemberRepository.findByExamIdAndUserId(EXAM_ID, TEACHER_ID)).thenReturn(Optional.of(
            new ExamMember(EXAM_ID, TEACHER_ID, ExamMemberRole.CHAIR, Instant.now(), TEACHER_ID)));
        when(examPaperItemRepository.existsUnassignedItemByPaperId(PAPER_ID)).thenReturn(false);
        when(examPaperRepository.save(any(ExamPaper.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    /** Chốt hồi quy: người soạn đề chính là người khoá đề, và điều đó phải được phép. */
    @Test
    void should_let_the_authoring_chair_lock_the_paper_in_one_step() {
        when(examPaperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper(ExamPaperStatus.DRAFT)));

        var result = useCase.execute(new UpdateExamPaperStatusCommand(PAPER_ID, "LOCK", null));

        assertThat(result.status()).isEqualTo(ExamPaperStatus.LOCKED.name());
    }

    @Test
    void should_reject_locking_a_paper_with_unassigned_slots() {
        when(examPaperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper(ExamPaperStatus.DRAFT)));
        when(examPaperItemRepository.existsUnassignedItemByPaperId(PAPER_ID)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new UpdateExamPaperStatusCommand(PAPER_ID, "LOCK", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("còn ô câu hỏi chưa được gán");
    }

    @Test
    void should_reject_locking_when_caller_is_not_the_chair() {
        when(examPaperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper(ExamPaperStatus.DRAFT)));
        when(examMemberRepository.findByExamIdAndUserId(EXAM_ID, TEACHER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdateExamPaperStatusCommand(PAPER_ID, "LOCK", null)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Quyền truy cập bị từ chối");
    }

    /** Mở lại để sửa nội dung rồi khoá lần nữa — cũng không qua luồng duyệt. */
    @Test
    void should_let_the_authoring_chair_reopen_a_locked_paper() {
        when(examPaperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper(ExamPaperStatus.LOCKED)));

        var result = useCase.execute(new UpdateExamPaperStatusCommand(PAPER_ID, "REOPEN", null));

        assertThat(result.status()).isEqualTo(ExamPaperStatus.DRAFT.name());
    }

    private Exam classTest() {
        var exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setKind(ExamKind.CLASS_TEST);
        exam.setSchoolId(SCHOOL_ID);
        return exam;
    }

    private ExamPaper paper(ExamPaperStatus status) {
        var paper = new ExamPaper();
        paper.setId(PAPER_ID);
        paper.setExamId(EXAM_ID);
        paper.setStatus(status);
        // Giáo viên tạo bài cũng là người soạn đề — chính là ca requireNotAuthor sẽ chặn.
        paper.setCreatedBy(TEACHER_ID);
        return paper;
    }
}
