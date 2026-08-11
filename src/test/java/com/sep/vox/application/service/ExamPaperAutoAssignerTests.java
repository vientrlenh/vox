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
        var paperId = givenPapers(1).get(0);
        var candidate = candidate();

        assigner.assignPapersIfNeeded(exam(ExamKind.CLASS_TEST), List.of(candidate), Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isEqualTo(paperId);
    }

    /** Trước đây kỳ thi tập trung bị bỏ qua hoàn toàn — giờ cũng được gán mặc định. */
    @Test
    void should_assign_papers_of_a_centralized_exam() {
        var paperIds = givenPapers(2);
        var candidate = candidate();

        assigner.assignPapersIfNeeded(exam(ExamKind.CENTRALIZED), List.of(candidate), Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isIn(paperIds);
    }

    @Test
    void should_spread_papers_evenly_across_candidates() {
        var paperIds = givenPapers(2);
        var candidates = List.of(candidate(), candidate(), candidate(), candidate());

        assigner.assignPapersIfNeeded(exam(ExamKind.CENTRALIZED), candidates, Instant.now(), TEACHER_ID);

        assertThat(candidates).allSatisfy(candidate -> assertThat(candidate.getAssignedPaperId()).isNotNull());
        assertThat(countUsing(candidates, paperIds.get(0))).isEqualTo(2);
        assertThat(countUsing(candidates, paperIds.get(1))).isEqualTo(2);
    }

    /**
     * Rải đều phải tính TRONG TỪNG CA. Rải đều trên toàn kỳ thi thì hai vòng round-robin -- xếp ca và
     * phân đề -- trùng chu kỳ, và cả phòng lĩnh trọn một mã đề, mất sạch tác dụng của nhiều mã đề.
     * Thứ tự dựng ở đây đúng bằng thứ tự AutoFillExamCandidatesUseCase tạo ra.
     */
    @Test
    void should_spread_papers_within_each_schedule() {
        givenPapers(2);
        var firstSchedule = UUID.randomUUID();
        var secondSchedule = UUID.randomUUID();
        var candidates = List.of(
            candidateInSchedule(firstSchedule),
            candidateInSchedule(secondSchedule),
            candidateInSchedule(firstSchedule),
            candidateInSchedule(secondSchedule));

        assigner.assignPapersIfNeeded(exam(ExamKind.CENTRALIZED), candidates, Instant.now(), TEACHER_ID);

        assertThat(distinctPapersIn(candidates, firstSchedule)).hasSize(2);
        assertThat(distinctPapersIn(candidates, secondSchedule)).hasSize(2);
    }

    /** Phân bố phải tính cả thí sinh đã có đề, nếu không mỗi lượt gán lại dồn vào mã đề đầu tiên. */
    @Test
    void should_continue_the_existing_distribution() {
        var paperIds = givenPapers(2);
        var alreadyOnFirst = candidate();
        alreadyOnFirst.setAssignedPaperId(paperIds.get(0));
        var anotherOnFirst = candidate();
        anotherOnFirst.setAssignedPaperId(paperIds.get(0));
        when(examCandidateRepository.findByExamId(EXAM_ID)).thenReturn(List.of(alreadyOnFirst, anotherOnFirst));
        var candidates = List.of(candidate(), candidate());

        assigner.assignPapersIfNeeded(exam(ExamKind.CENTRALIZED), candidates, Instant.now(), TEACHER_ID);

        assertThat(countUsing(candidates, paperIds.get(1))).isEqualTo(2);
    }

    @Test
    void should_keep_an_already_assigned_paper() {
        givenPapers(2);
        var alreadyAssigned = UUID.randomUUID();
        var candidate = candidate();
        candidate.setAssignedPaperId(alreadyAssigned);

        assigner.assignPapersIfNeeded(exam(ExamKind.CENTRALIZED), List.of(candidate), Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isEqualTo(alreadyAssigned);
    }

    /**
     * Bám đúng AssignExamPapersUseCase: chưa khoá hết thì không gán, nếu không thí sinh trỏ vào một
     * mã đề mà người ra đề vẫn sửa được.
     */
    @Test
    void should_skip_when_any_paper_is_not_locked() {
        when(examPaperRepository.findByExamId(EXAM_ID)).thenReturn(List.of(
            paper(1, ExamPaperStatus.LOCKED),
            paper(2, ExamPaperStatus.DRAFT)));
        var candidate = candidate();

        assigner.assignPapersIfNeeded(exam(ExamKind.CENTRALIZED), List.of(candidate), Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isNull();
    }

    @Test
    void should_skip_when_exam_has_no_paper() {
        when(examPaperRepository.findByExamId(EXAM_ID)).thenReturn(List.of());
        var candidate = candidate();

        assigner.assignPapersIfNeeded(exam(ExamKind.CENTRALIZED), List.of(candidate), Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isNull();
    }

    /** Thí sinh đã huỷ/miễn thi không vào phòng nên không cần đề. */
    @Test
    void should_skip_non_scorable_candidate() {
        givenPapers(1);
        var candidate = candidate();
        candidate.setStatus(ExamCandidateStatus.CANCELLED);

        assigner.assignPapersIfNeeded(exam(ExamKind.CENTRALIZED), List.of(candidate), Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isNull();
    }

    @Test
    void should_backfill_every_candidate_without_paper() {
        var paperIds = givenPapers(2);
        var withPaper = candidate();
        withPaper.setAssignedPaperId(paperIds.get(0));
        var first = candidate();
        var second = candidate();
        when(examCandidateRepository.findByExamId(EXAM_ID)).thenReturn(List.of(withPaper, first, second));

        var backfilled = assigner.backfillExam(exam(ExamKind.CENTRALIZED), Instant.now(), TEACHER_ID);

        assertThat(backfilled).isEqualTo(2);
        assertThat(first.getAssignedPaperId()).isNotNull();
        assertThat(second.getAssignedPaperId()).isNotNull();
        assertThat(withPaper.getAssignedPaperId()).isEqualTo(paperIds.get(0));
        verify(examCandidateRepository).saveAll(List.of(first, second));
    }

    @Test
    void should_not_save_anything_when_backfill_has_nothing_to_do() {
        var paperIds = givenPapers(1);
        var withPaper = candidate();
        withPaper.setAssignedPaperId(paperIds.get(0));
        when(examCandidateRepository.findByExamId(EXAM_ID)).thenReturn(List.of(withPaper));

        var backfilled = assigner.backfillExam(exam(ExamKind.CENTRALIZED), Instant.now(), TEACHER_ID);

        assertThat(backfilled).isZero();
        verify(examCandidateRepository, never()).saveAll(any());
    }

    @Test
    void should_not_backfill_when_papers_are_not_all_locked() {
        when(examPaperRepository.findByExamId(EXAM_ID)).thenReturn(List.of(paper(1, ExamPaperStatus.APPROVED)));

        var backfilled = assigner.backfillExam(exam(ExamKind.CENTRALIZED), Instant.now(), TEACHER_ID);

        assertThat(backfilled).isZero();
        verify(examCandidateRepository, never()).saveAll(any());
    }

    /** Mọi mã đề đã LOCKED nhưng danh sách trả về phải ổn định theo variant để gán tái lập được. */
    private List<UUID> givenPapers(int count) {
        var papers = new java.util.ArrayList<ExamPaper>();
        for (int variant = 1; variant <= count; variant++) {
            papers.add(paper(variant, ExamPaperStatus.LOCKED));
        }
        when(examPaperRepository.findByExamId(EXAM_ID)).thenReturn(papers);
        return papers.stream().map(ExamPaper::getId).toList();
    }

    private ExamCandidate candidateInSchedule(UUID scheduleId) {
        var candidate = candidate();
        candidate.setScheduleId(scheduleId);
        return candidate;
    }

    private java.util.Set<UUID> distinctPapersIn(List<ExamCandidate> candidates, UUID scheduleId) {
        return candidates.stream()
            .filter(candidate -> scheduleId.equals(candidate.getScheduleId()))
            .map(ExamCandidate::getAssignedPaperId)
            .collect(java.util.stream.Collectors.toSet());
    }

    private long countUsing(List<ExamCandidate> candidates, UUID paperId) {
        return candidates.stream().filter(candidate -> paperId.equals(candidate.getAssignedPaperId())).count();
    }

    private Exam exam(ExamKind kind) {
        var exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setKind(kind);
        return exam;
    }

    private ExamCandidate candidate() {
        var candidate = new ExamCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setExamId(EXAM_ID);
        candidate.setStatus(ExamCandidateStatus.ASSIGNED);
        return candidate;
    }

    private ExamPaper paper(int variant, ExamPaperStatus status) {
        var paper = new ExamPaper();
        paper.setId(UUID.randomUUID());
        paper.setExamId(EXAM_ID);
        paper.setCode("MD" + variant);
        paper.setVariant(variant);
        paper.setStatus(status);
        return paper;
    }
}
