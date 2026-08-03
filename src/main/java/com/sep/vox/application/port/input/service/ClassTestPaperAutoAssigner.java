package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.repository.ExamPaperRepository;

/**
 * Tự gán đề cho thí sinh của bài kiểm tra trên lớp ngay khi được xếp vào ca thi.
 *
 * <p>Kỳ thi tập trung có nhiều đề nên phải phân đề thủ công (AssignExamPapersUseCase, và đề phải
 * LOCKED trước). Bài kiểm tra trên lớp thì CreateClassTestUseCase chỉ tạo đúng một đề, nên bắt giáo
 * viên bấm thêm một bước "chọn đề" trên đúng một lựa chọn là thừa — gán luôn ở đây.
 *
 * <p>Không đụng tới thí sinh đã có đề: phân đề thủ công (nếu sau này bài trên lớp có nhiều đề) vẫn
 * được ưu tiên.
 */
@Service
public class ClassTestPaperAutoAssigner {

    private final ExamPaperRepository examPaperRepository;

    public ClassTestPaperAutoAssigner(ExamPaperRepository examPaperRepository) {
        this.examPaperRepository = examPaperRepository;
    }

    public void assignSinglePaperIfNeeded(Exam exam, ExamCandidate candidate, Instant now, UUID updatedBy) {
        var paperId = resolveSinglePaperId(exam);
        if (paperId == null || candidate.getAssignedPaperId() != null) {
            return;
        }
        candidate.assignPaper(paperId, now, updatedBy);
    }

    /**
     * Id của đề duy nhất, hoặc {@code null} nếu không áp dụng (không phải bài trên lớp, hoặc bài có
     * số đề khác 1 — lúc đó phải phân đề thủ công).
     */
    public UUID resolveSinglePaperId(Exam exam) {
        if (exam.getKind() != ExamKind.CLASS_TEST) {
            return null;
        }
        var papers = examPaperRepository.findByExamId(exam.getId());
        if (papers.size() != 1) {
            return null;
        }
        return papers.get(0).getId();
    }
}
