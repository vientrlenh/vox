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

import com.sep.vox.application.port.input.service.ExamPaperAutoAssigner;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;

/**
 * Gán đề tự động cho cả hai loại bài: điều kiện là MỌI mã đề của kỳ thi đã LOCKED (đúng bất biến của
 * AssignExamPapersUseCase), và đề được rải đều theo phân bố hiện có để nhiều mã đề còn giữ được ý
 * nghĩa. Phân đề thủ công luôn được ưu tiên.
 */
class ExamPaperAutoAssignerTests {

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID TEACHER_ID = UUID.randomUUID();

    private ExamPaperRepository examPaperRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamPaperAutoAssigner assigner;

    @BeforeEach
    void setUp() {
        examPaperRepository = mock(ExamPaperRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        assigner = new ExamPaperAutoAssigner(examPaperRepository, examCandidateRepository);
    }

    @Test
    void should_assign_the_single_locked_paper_of_a_class_test() {
        UUID paperId = givenPapers(1).get(0);
        ExamCandidate candidate = candidate();

        assigner.assignPapersIfNeeded(exam(ExamKind.CLASS_TEST), List.of(candidate), Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isEqualTo(paperId);
    }

    /**
     * Previously a centralized exam was skipped entirely -- now it also gets the default
     * assignment.
     */
    @Test
    void should_assign_papers_of_a_centralized_exam() {
        List<UUID> paperIds = givenPapers(2);
        ExamCandidate candidate = candidate();

        assigner.assignPapersIfNeeded(exam(ExamKind.CENTRALIZED), List.of(candidate), Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isIn(paperIds);
    }

    @Test
    void should_spread_papers_evenly_across_candidates() {
        List<UUID> paperIds = givenPapers(2);
        List<ExamCandidate> candidates = List.of(candidate(), candidate(), candidate(), candidate());

        assigner.assignPapersIfNeeded(exam(ExamKind.CENTRALIZED), candidates, Instant.now(), TEACHER_ID);

        assertThat(candidates).allSatisfy(candidate -> assertThat(candidate.getAssignedPaperId()).isNotNull());
        assertThat(countUsing(candidates, paperIds.get(0))).isEqualTo(2);
        assertThat(countUsing(candidates, paperIds.get(1))).isEqualTo(2);
    }

    /**
     * Even distribution must be computed WITHIN EACH SESSION. If it is spread over the whole exam,
     * the two round-robin passes -- placing candidates in sessions and assigning papers -- share
     * the same cycle, and a whole room ends up with a single paper variant, losing all the benefit
     * of having several variants. The order built here is exactly the order
     * AutoFillExamCandidatesUseCase produces.
     */
    @Test
    void should_spread_papers_within_each_schedule() {
        givenPapers(2);
        UUID firstSchedule = UUID.randomUUID();
        UUID secondSchedule = UUID.randomUUID();
        List<ExamCandidate> candidates = List.of(
            candidateInSchedule(firstSchedule),
            candidateInSchedule(secondSchedule),
            candidateInSchedule(firstSchedule),
            candidateInSchedule(secondSchedule));

        assigner.assignPapersIfNeeded(exam(ExamKind.CENTRALIZED), candidates, Instant.now(), TEACHER_ID);

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
        ExamCandidate alreadyOnFirst = candidate();
        alreadyOnFirst.setAssignedPaperId(paperIds.get(0));
        ExamCandidate anotherOnFirst = candidate();
        anotherOnFirst.setAssignedPaperId(paperIds.get(0));
        when(examCandidateRepository.findByExamId(EXAM_ID)).thenReturn(List.of(alreadyOnFirst, anotherOnFirst));
        List<ExamCandidate> candidates = List.of(candidate(), candidate());

        assigner.assignPapersIfNeeded(exam(ExamKind.CENTRALIZED), candidates, Instant.now(), TEACHER_ID);

        assertThat(countUsing(candidates, paperIds.get(1))).isEqualTo(2);
    }

    @Test
    void should_keep_an_already_assigned_paper() {
        givenPapers(2);
        UUID alreadyAssigned = UUID.randomUUID();
        ExamCandidate candidate = candidate();
        candidate.setAssignedPaperId(alreadyAssigned);

        assigner.assignPapersIfNeeded(exam(ExamKind.CENTRALIZED), List.of(candidate), Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isEqualTo(alreadyAssigned);
    }

    /**
     * Follows AssignExamPapersUseCase exactly: nothing is assigned until everything is locked;
     * otherwise a candidate would point to a variant that the author can still edit.
     */
    @Test
    void should_skip_when_any_paper_is_not_locked() {
        when(examPaperRepository.findByExamId(EXAM_ID)).thenReturn(List.of(
            paper(1, ExamPaperStatus.LOCKED),
            paper(2, ExamPaperStatus.DRAFT)));
        ExamCandidate candidate = candidate();

        assigner.assignPapersIfNeeded(exam(ExamKind.CENTRALIZED), List.of(candidate), Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isNull();
    }

    @Test
    void should_skip_when_exam_has_no_paper() {
        when(examPaperRepository.findByExamId(EXAM_ID)).thenReturn(List.of());
        ExamCandidate candidate = candidate();

        assigner.assignPapersIfNeeded(exam(ExamKind.CENTRALIZED), List.of(candidate), Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isNull();
    }

    /** A candidate who was cancelled/exempted does not enter the room, so does not need a paper. */
    @Test
    void should_skip_non_scorable_candidate() {
        givenPapers(1);
        ExamCandidate candidate = candidate();
        candidate.setStatus(ExamCandidateStatus.CANCELLED);

        assigner.assignPapersIfNeeded(exam(ExamKind.CENTRALIZED), List.of(candidate), Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isNull();
    }

    @Test
    void should_backfill_every_candidate_without_paper() {
        List<UUID> paperIds = givenPapers(2);
        ExamCandidate withPaper = candidate();
        withPaper.setAssignedPaperId(paperIds.get(0));
        ExamCandidate first = candidate();
        ExamCandidate second = candidate();
        when(examCandidateRepository.findByExamId(EXAM_ID)).thenReturn(List.of(withPaper, first, second));

        int backfilled = assigner.backfillExam(exam(ExamKind.CENTRALIZED), Instant.now(), TEACHER_ID);

        assertThat(backfilled).isEqualTo(2);
        assertThat(first.getAssignedPaperId()).isNotNull();
        assertThat(second.getAssignedPaperId()).isNotNull();
        assertThat(withPaper.getAssignedPaperId()).isEqualTo(paperIds.get(0));
        verify(examCandidateRepository).saveAll(List.of(first, second));
    }

    @Test
    void should_not_save_anything_when_backfill_has_nothing_to_do() {
        List<UUID> paperIds = givenPapers(1);
        ExamCandidate withPaper = candidate();
        withPaper.setAssignedPaperId(paperIds.get(0));
        when(examCandidateRepository.findByExamId(EXAM_ID)).thenReturn(List.of(withPaper));

        int backfilled = assigner.backfillExam(exam(ExamKind.CENTRALIZED), Instant.now(), TEACHER_ID);

        assertThat(backfilled).isZero();
        verify(examCandidateRepository, never()).saveAll(any());
    }

    @Test
    void should_not_backfill_when_papers_are_not_all_locked() {
        when(examPaperRepository.findByExamId(EXAM_ID)).thenReturn(List.of(paper(1, ExamPaperStatus.APPROVED)));

        int backfilled = assigner.backfillExam(exam(ExamKind.CENTRALIZED), Instant.now(), TEACHER_ID);

        assertThat(backfilled).isZero();
        verify(examCandidateRepository, never()).saveAll(any());
    }

    /**
     * Every variant is LOCKED, but the returned list must be stable by variant so the assignment is
     * reproducible.
     */
    private List<UUID> givenPapers(int count) {
        List<ExamPaper> papers = new java.util.ArrayList<ExamPaper>();
        for (int variant = 1; variant <= count; variant++) {
            papers.add(paper(variant, ExamPaperStatus.LOCKED));
        }
        when(examPaperRepository.findByExamId(EXAM_ID)).thenReturn(papers);
        return papers.stream().map(paper -> paper.getId()).toList();
    }

    private ExamCandidate candidateInSchedule(UUID scheduleId) {
        ExamCandidate candidate = candidate();
        candidate.setScheduleId(scheduleId);
        return candidate;
    }

    private java.util.Set<UUID> distinctPapersIn(List<ExamCandidate> candidates, UUID scheduleId) {
        return candidates.stream()
            .filter(candidate -> scheduleId.equals(candidate.getScheduleId()))
            .map(candidate -> candidate.getAssignedPaperId())
            .collect(java.util.stream.Collectors.toSet());
    }

    private long countUsing(List<ExamCandidate> candidates, UUID paperId) {
        return candidates.stream().filter(candidate -> paperId.equals(candidate.getAssignedPaperId())).count();
    }

    private Exam exam(ExamKind kind) {
        Exam exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setKind(kind);
        return exam;
    }

    private ExamCandidate candidate() {
        ExamCandidate candidate = new ExamCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setExamId(EXAM_ID);
        candidate.setStatus(ExamCandidateStatus.ASSIGNED);
        return candidate;
    }

    private ExamPaper paper(int variant, ExamPaperStatus status) {
        ExamPaper paper = new ExamPaper();
        paper.setId(UUID.randomUUID());
        paper.setExamId(EXAM_ID);
        paper.setCode("MD" + variant);
        paper.setVariant(variant);
        paper.setStatus(status);
        return paper;
    }
}
