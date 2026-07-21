package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.QuestionRepository;

/**
 * H.1: exam.examTimeDurationSecond = MAX(paperDuration) trên mọi ExamPaper của kỳ thi,
 * paperDuration = tổng question.maxResponseSeconds của mọi item đã gán câu hỏi trong mã đề
 * đó. Field tự động tính, KHÔNG có ô nhập frontend - gọi lại hàm này mỗi khi cấu trúc 1 mã đề
 * thay đổi (tạo mã đề mới, đổi câu hỏi 1 item, ghi đè câu hỏi class test, đổi blueprint).
 */
@Service
public class RecalculateExamTimeDurationService {

    private final ExamRepository examRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final QuestionRepository questionRepository;

    public RecalculateExamTimeDurationService(
            ExamRepository examRepository,
            ExamPaperRepository examPaperRepository,
            ExamPaperItemRepository examPaperItemRepository,
            QuestionRepository questionRepository) {
        this.examRepository = examRepository;
        this.examPaperRepository = examPaperRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional
    public void recalculate(UUID examId) {
        var exam = examRepository.findById(examId).orElse(null);
        if (exam == null) {
            return;
        }

        var papers = examPaperRepository.findByExamId(examId);
        if (papers.isEmpty()) {
            exam.setExamTimeDurationSecond(null);
            examRepository.save(exam);
            return;
        }

        var maxDuration = 0;
        for (var paper : papers) {
            var paperDuration = 0;
            for (var item : examPaperItemRepository.findByPaperId(paper.getId())) {
                if (item.getQuestionId() == null) {
                    continue;
                }
                var question = questionRepository.findById(item.getQuestionId()).orElse(null);
                if (question != null) {
                    paperDuration += question.getMaxResponseSeconds();
                }
            }
            maxDuration = Math.max(maxDuration, paperDuration);
        }

        exam.setExamTimeDurationSecond(maxDuration);
        examRepository.save(exam);
    }
}
