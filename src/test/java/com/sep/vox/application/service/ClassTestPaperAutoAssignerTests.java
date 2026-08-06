package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.ClassTestPaperAutoAssigner;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.repository.ExamPaperRepository;

/**
 * Bài kiểm tra trên lớp chỉ có đúng một đề nên gán luôn khi xếp học sinh vào ca; kỳ thi tập trung
 * vẫn phải phân đề thủ công qua AssignExamPapersUseCase.
 */
class ClassTestPaperAutoAssignerTests {

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID PAPER_ID = UUID.randomUUID();
    private static final UUID TEACHER_ID = UUID.randomUUID();

    private ExamPaperRepository examPaperRepository;
    private ClassTestPaperAutoAssigner assigner;

    @BeforeEach
    void setUp() {
        examPaperRepository = mock(ExamPaperRepository.class);
        assigner = new ClassTestPaperAutoAssigner(examPaperRepository);
    }

    @Test
    void should_assign_the_single_paper_of_a_class_test() {
        when(examPaperRepository.findByExamId(EXAM_ID)).thenReturn(List.of(paper(PAPER_ID)));
        var candidate = candidate();

        assigner.assignSinglePaperIfNeeded(exam(ExamKind.CLASS_TEST), candidate, Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isEqualTo(PAPER_ID);
    }

    @Test
    void should_not_touch_centralized_exam_candidates() {
        var candidate = candidate();

        assigner.assignSinglePaperIfNeeded(exam(ExamKind.CENTRALIZED), candidate, Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isNull();
    }

    @Test
    void should_keep_an_already_assigned_paper() {
        var alreadyAssigned = UUID.randomUUID();
        when(examPaperRepository.findByExamId(EXAM_ID)).thenReturn(List.of(paper(PAPER_ID)));
        var candidate = candidate();
        candidate.setAssignedPaperId(alreadyAssigned);

        assigner.assignSinglePaperIfNeeded(exam(ExamKind.CLASS_TEST), candidate, Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isEqualTo(alreadyAssigned);
    }

    /** Nhiều đề thì không đoán được đề nào — để phân đề thủ công quyết định. */
    @Test
    void should_skip_when_class_test_has_more_than_one_paper() {
        when(examPaperRepository.findByExamId(EXAM_ID))
            .thenReturn(List.of(paper(PAPER_ID), paper(UUID.randomUUID())));
        var candidate = candidate();

        assigner.assignSinglePaperIfNeeded(exam(ExamKind.CLASS_TEST), candidate, Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isNull();
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
        return candidate;
    }

    /**
     * Phân đề thủ công (AssignExamPapersUseCase) đòi mọi mã đề đã LOCKED. Auto-assign phải theo cùng
     * bất biến, nếu không thí sinh trỏ vào một mã đề vẫn sửa được cho tới lúc START ép khoá.
     */
    @Test
    void should_skip_when_the_only_paper_is_not_locked() {
        when(examPaperRepository.findByExamId(EXAM_ID)).thenReturn(List.of(paper(PAPER_ID, ExamPaperStatus.DRAFT)));
        var candidate = candidate();

        assigner.assignSinglePaperIfNeeded(exam(ExamKind.CLASS_TEST), candidate, Instant.now(), TEACHER_ID);

        assertThat(candidate.getAssignedPaperId()).isNull();
    }

    private ExamPaper paper(UUID id) {
        return paper(id, ExamPaperStatus.LOCKED);
    }

    private ExamPaper paper(UUID id, ExamPaperStatus status) {
        var paper = new ExamPaper();
        paper.setId(id);
        paper.setExamId(EXAM_ID);
        paper.setStatus(status);
        return paper;
    }
}
