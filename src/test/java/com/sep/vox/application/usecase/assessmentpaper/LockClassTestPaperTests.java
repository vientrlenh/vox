package com.sep.vox.application.usecase.assessmentpaper;

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
import com.sep.vox.application.port.input.command.assessmentpaper.UpdateAssessmentPaperStatusCommand;
import com.sep.vox.application.port.input.service.AssessmentPaperAutoAssigner;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.usecase.assessmentpaper.UpdateAssessmentPaperStatusUseCase;
import com.sep.vox.application.port.input.service.AssessmentPaperAuthoringAccessService;
import com.sep.vox.domain.dto.AssessmentPaperDto;
import com.sep.vox.domain.model.assessment.AssessmentMember;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Assigning papers requires every paper to be LOCKED. A class test has no separation of author / reviewer --
 * the teacher is both the CHAIR and the author of every paper -- so the 3-step review flow of a centralized assessment
 * ({@code requireNotAuthor}) would lock the teacher out of their own paper. Here locking a paper is
 * a single step DRAFT → LOCKED.
 */
class LockClassTestPaperTests {

    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final UUID PAPER_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID TEACHER_ID = UUID.randomUUID();

    private AssessmentPaperRepository assessmentPaperRepository;
    private AssessmentPaperItemRepository assessmentPaperItemRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private UpdateAssessmentPaperStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        assessmentPaperRepository = mock(AssessmentPaperRepository.class);
        assessmentPaperItemRepository = mock(AssessmentPaperItemRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        AssessmentRepository assessmentRepository = mock(AssessmentRepository.class);
        UserContextPort userContextPort = mock(UserContextPort.class);

        useCase = new UpdateAssessmentPaperStatusUseCase(
            assessmentPaperRepository,
            assessmentPaperItemRepository,
            assessmentRepository,
            mock(AssessmentCandidateRepository.class),
            new AssessmentPaperAuthoringAccessService(
                assessmentMemberRepository, mock(SchoolUserRepository.class), mock(UserRoleQueryRepository.class)),
            mock(AssessmentTimeQuotaGuardService.class),
            mock(AssessmentPaperAutoAssigner.class),
            userContextPort
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(TEACHER_ID);
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(classTest()));
        when(assessmentMemberRepository.findByAssessmentIdAndUserId(ASSESSMENT_ID, TEACHER_ID)).thenReturn(Optional.of(
            AssessmentMember.builder()
                    .assessmentId(ASSESSMENT_ID)
                    .userId(TEACHER_ID)
                    .role(AssessmentMemberRole.CHAIR)
                    .grantedAt(Instant.now())
                    .grantedBy(TEACHER_ID)
                    .build()));
        when(assessmentPaperItemRepository.existsUnassignedItemByPaperId(PAPER_ID)).thenReturn(false);
        when(assessmentPaperRepository.save(any(AssessmentPaper.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    /**
     * Regression guard: the person who drafted the paper is the person who locks it, and that must
     * be allowed.
     */
    @Test
    void should_let_the_authoring_chair_lock_the_paper_in_one_step() {
        when(assessmentPaperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper(AssessmentPaperStatus.DRAFT)));

        AssessmentPaperDto result = useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "LOCK", null));

        assertThat(result.status()).isEqualTo(AssessmentPaperStatus.LOCKED);
    }

    @Test
    void should_reject_locking_a_paper_with_unassigned_slots() {
        when(assessmentPaperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper(AssessmentPaperStatus.DRAFT)));
        when(assessmentPaperItemRepository.existsUnassignedItemByPaperId(PAPER_ID)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "LOCK", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unassigned question slots");
    }

    @Test
    void should_reject_locking_when_caller_is_not_the_chair() {
        when(assessmentPaperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper(AssessmentPaperStatus.DRAFT)));
        when(assessmentMemberRepository.findByAssessmentIdAndUserId(ASSESSMENT_ID, TEACHER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "LOCK", null)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Access denied");
    }

    /**
     * Reopened to edit the content and then locked again -- this also does not go through the
     * review flow.
     */
    @Test
    void should_let_the_authoring_chair_reopen_a_locked_paper() {
        when(assessmentPaperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper(AssessmentPaperStatus.LOCKED)));

        AssessmentPaperDto result = useCase.execute(new UpdateAssessmentPaperStatusCommand(PAPER_ID, "REOPEN", null));

        assertThat(result.status()).isEqualTo(AssessmentPaperStatus.DRAFT);
    }

    private Assessment classTest() {
        Assessment assessment = new Assessment();
        assessment.setId(ASSESSMENT_ID);
        assessment.setKind(AssessmentKind.CLASS_TEST);
        assessment.setSchoolId(SCHOOL_ID);
        return assessment;
    }

    private AssessmentPaper paper(AssessmentPaperStatus status) {
        AssessmentPaper paper = new AssessmentPaper();
        paper.setId(PAPER_ID);
        paper.setAssessmentId(ASSESSMENT_ID);
        paper.setStatus(status);
        // The teacher who created the test is also the one who drafted the paper -- exactly the
        // case requireNotAuthor would block.
        paper.setCreatedBy(TEACHER_ID.toString());
        return paper;
    }
}
