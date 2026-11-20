package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.AssessmentPaperAutoAssigner;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;

/**
 * Automatic paper assignment for both kinds of assessment: the condition is that EVERY paper variant of the assessment is LOCKED (exactly the invariant of
 * AssignAssessmentPapersUseCase), and papers are spread evenly over the existing distribution so that several variants keep their
 * meaning. Manual paper assignment always takes priority.
 */
class AssessmentPaperAutoAssignerTests {

    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final UUID TEACHER_ID = UUID.randomUUID();

    private AssessmentPaperRepository assessmentPaperRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentPaperAutoAssigner assigner;

    @BeforeEach
    void setUp() {
        assessmentPaperRepository = mock(AssessmentPaperRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assigner = new AssessmentPaperAutoAssigner(assessmentPaperRepository, assessmentCandidateRepository);
    }

    @Test
    void should_assign_the_single_locked_paper_of_a_class_test() {
        UUID paperId = givenPapers(1).get(0);
        AssessmentCandidate candidate = candidate();

        assigner.assignPapersIfNeeded(assessment(AssessmentKind.CLASS_TEST), List.of(candidate), Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isEqualTo(paperId);
    }

    /**
     * Previously a centralized assessment was skipped entirely -- now it also gets the default
     * assignment.
     */
    @Test
    void should_assign_papers_of_a_centralized_assessment() {
        List<UUID> paperIds = givenPapers(2);
        AssessmentCandidate candidate = candidate();

        assigner.assignPapersIfNeeded(assessment(AssessmentKind.CENTRALIZED), List.of(candidate), Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isIn(paperIds);
    }

    @Test
    void should_spread_papers_evenly_across_candidates() {
        List<UUID> paperIds = givenPapers(2);
        List<AssessmentCandidate> candidates = List.of(candidate(), candidate(), candidate(), candidate());

        assigner.assignPapersIfNeeded(assessment(AssessmentKind.CENTRALIZED), candidates, Instant.now(), TEACHER_ID);

        assertThat(candidates).allSatisfy(candidate -> assertThat(candidate.getAssignedPaperId()).isNotNull());
        assertThat(countUsing(candidates, paperIds.get(0))).isEqualTo(2);
        assertThat(countUsing(candidates, paperIds.get(1))).isEqualTo(2);
    }

    /**
     * Even distribution must be computed WITHIN EACH SESSION. If it is spread over the whole assessment,
     * the two round-robin passes -- placing candidates in sessions and assigning papers -- share
     * the same cycle, and a whole room ends up with a single paper variant, losing all the benefit
     * of having several variants. The order built here is exactly the order
     * AutoFillAssessmentCandidatesUseCase produces.
     */
    @Test
    void should_spread_papers_within_each_schedule() {
        givenPapers(2);
        UUID firstSchedule = UUID.randomUUID();
        UUID secondSchedule = UUID.randomUUID();
        List<AssessmentCandidate> candidates = List.of(
            candidateInSchedule(firstSchedule),
            candidateInSchedule(secondSchedule),
            candidateInSchedule(firstSchedule),
            candidateInSchedule(secondSchedule));

        assigner.assignPapersIfNeeded(assessment(AssessmentKind.CENTRALIZED), candidates, Instant.now(), TEACHER_ID);

        assertThat(distinctPapersIn(candidates, firstSchedule)).hasSize(2);
        assertThat(distinctPapersIn(candidates, secondSchedule)).hasSize(2);
    }

    /**
     * The distribution must also count candidates who already have a paper; otherwise every
     * assignment run piles onto the first variant again.
     */
    @Test
    void should_continue_the_existing_distribution() {
        List<UUID> paperIds = givenPapers(2);
        AssessmentCandidate alreadyOnFirst = candidate();
        alreadyOnFirst.setAssignedPaperId(paperIds.get(0));
        AssessmentCandidate anotherOnFirst = candidate();
        anotherOnFirst.setAssignedPaperId(paperIds.get(0));
        when(assessmentCandidateRepository.findByAssessmentId(ASSESSMENT_ID)).thenReturn(List.of(alreadyOnFirst, anotherOnFirst));
        List<AssessmentCandidate> candidates = List.of(candidate(), candidate());

        assigner.assignPapersIfNeeded(assessment(AssessmentKind.CENTRALIZED), candidates, Instant.now(), TEACHER_ID);

        assertThat(countUsing(candidates, paperIds.get(1))).isEqualTo(2);
    }

    @Test
    void should_keep_an_already_assigned_paper() {
        givenPapers(2);
        UUID alreadyAssigned = UUID.randomUUID();
        AssessmentCandidate candidate = candidate();
        candidate.setAssignedPaperId(alreadyAssigned);

        assigner.assignPapersIfNeeded(assessment(AssessmentKind.CENTRALIZED), List.of(candidate), Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isEqualTo(alreadyAssigned);
    }

    /**
     * Follows AssignAssessmentPapersUseCase exactly: nothing is assigned until everything is locked;
     * otherwise a candidate would point to a variant that the author can still edit.
     */
    @Test
    void should_skip_when_any_paper_is_not_locked() {
        when(assessmentPaperRepository.findByAssessmentId(ASSESSMENT_ID)).thenReturn(List.of(
            paper(1, AssessmentPaperStatus.LOCKED),
            paper(2, AssessmentPaperStatus.DRAFT)));
        AssessmentCandidate candidate = candidate();

        assigner.assignPapersIfNeeded(assessment(AssessmentKind.CENTRALIZED), List.of(candidate), Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isNull();
    }

    @Test
    void should_skip_when_assessment_has_no_paper() {
        when(assessmentPaperRepository.findByAssessmentId(ASSESSMENT_ID)).thenReturn(List.of());
        AssessmentCandidate candidate = candidate();

        assigner.assignPapersIfNeeded(assessment(AssessmentKind.CENTRALIZED), List.of(candidate), Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isNull();
    }

    /** A candidate who was cancelled/exempted does not enter the room, so does not need a paper. */
    @Test
    void should_skip_non_scorable_candidate() {
        givenPapers(1);
        AssessmentCandidate candidate = candidate();
        candidate.setStatus(AssessmentCandidateStatus.CANCELLED);

        assigner.assignPapersIfNeeded(assessment(AssessmentKind.CENTRALIZED), List.of(candidate), Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isNull();
    }

    @Test
    void should_backfill_every_candidate_without_paper() {
        List<UUID> paperIds = givenPapers(2);
        AssessmentCandidate withPaper = candidate();
        withPaper.setAssignedPaperId(paperIds.get(0));
        AssessmentCandidate first = candidate();
        AssessmentCandidate second = candidate();
        when(assessmentCandidateRepository.findByAssessmentId(ASSESSMENT_ID)).thenReturn(List.of(withPaper, first, second));

        int backfilled = assigner.backfillAssessment(assessment(AssessmentKind.CENTRALIZED), Instant.now(), TEACHER_ID);

        assertThat(backfilled).isEqualTo(2);
        assertThat(first.getAssignedPaperId()).isNotNull();
        assertThat(second.getAssignedPaperId()).isNotNull();
        assertThat(withPaper.getAssignedPaperId()).isEqualTo(paperIds.get(0));
        verify(assessmentCandidateRepository).saveAll(List.of(first, second));
    }

    @Test
    void should_not_save_anything_when_backfill_has_nothing_to_do() {
        List<UUID> paperIds = givenPapers(1);
        AssessmentCandidate withPaper = candidate();
        withPaper.setAssignedPaperId(paperIds.get(0));
        when(assessmentCandidateRepository.findByAssessmentId(ASSESSMENT_ID)).thenReturn(List.of(withPaper));

        int backfilled = assigner.backfillAssessment(assessment(AssessmentKind.CENTRALIZED), Instant.now(), TEACHER_ID);

        assertThat(backfilled).isZero();
        verify(assessmentCandidateRepository, never()).saveAll(any());
    }

    @Test
    void should_not_backfill_when_papers_are_not_all_locked() {
        when(assessmentPaperRepository.findByAssessmentId(ASSESSMENT_ID)).thenReturn(List.of(paper(1, AssessmentPaperStatus.APPROVED)));

        int backfilled = assigner.backfillAssessment(assessment(AssessmentKind.CENTRALIZED), Instant.now(), TEACHER_ID);

        assertThat(backfilled).isZero();
        verify(assessmentCandidateRepository, never()).saveAll(any());
    }

    /**
     * Every variant is LOCKED, but the returned list must be stable by variant so the assignment is
     * reproducible.
     */
    private List<UUID> givenPapers(int count) {
        List<AssessmentPaper> papers = new java.util.ArrayList<AssessmentPaper>();
        for (int variant = 1; variant <= count; variant++) {
            papers.add(paper(variant, AssessmentPaperStatus.LOCKED));
        }
        when(assessmentPaperRepository.findByAssessmentId(ASSESSMENT_ID)).thenReturn(papers);
        return papers.stream().map(paper -> paper.getId()).toList();
    }

    private AssessmentCandidate candidateInSchedule(UUID scheduleId) {
        AssessmentCandidate candidate = candidate();
        candidate.setScheduleId(scheduleId);
        return candidate;
    }

    private java.util.Set<UUID> distinctPapersIn(List<AssessmentCandidate> candidates, UUID scheduleId) {
        return candidates.stream()
            .filter(candidate -> scheduleId.equals(candidate.getScheduleId()))
            .map(candidate -> candidate.getAssignedPaperId())
            .collect(java.util.stream.Collectors.toSet());
    }

    private long countUsing(List<AssessmentCandidate> candidates, UUID paperId) {
        return candidates.stream().filter(candidate -> paperId.equals(candidate.getAssignedPaperId())).count();
    }

    private Assessment assessment(AssessmentKind kind) {
        Assessment assessment = new Assessment();
        assessment.setId(ASSESSMENT_ID);
        assessment.setKind(kind);
        return assessment;
    }

    private AssessmentCandidate candidate() {
        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setAssessmentId(ASSESSMENT_ID);
        candidate.setStatus(AssessmentCandidateStatus.ASSIGNED);
        return candidate;
    }

    private AssessmentPaper paper(int variant, AssessmentPaperStatus status) {
        AssessmentPaper paper = new AssessmentPaper();
        paper.setId(UUID.randomUUID());
        paper.setAssessmentId(ASSESSMENT_ID);
        paper.setCode("MD" + variant);
        paper.setVariant(variant);
        paper.setStatus(status);
        return paper;
    }
}
