package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.ExamScheduleWindowMessages;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.QuestionRepository;

/**
 * H.1: paper.timeDurationSeconds = tổng preparationTimeSeconds + maxResponseSeconds của mọi item
 * đã gán câu hỏi trong mã đề; exam.examTimeDurationSecond = MAX(paperDuration) trên mọi ExamPaper của kỳ thi.
 * Các field này tự động tính, không nhập tay ở frontend - gọi lại hàm này mỗi khi cấu trúc 1 mã đề thay đổi.
 */
@Service
public class RecalculateExamTimeDurationService {

    private final ExamRepository examRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final ExamScheduleRepository examScheduleRepository;

    public RecalculateExamTimeDurationService(
            ExamRepository examRepository,
            ExamPaperRepository examPaperRepository,
            ExamPaperItemRepository examPaperItemRepository,
            QuestionRepository questionRepository,
            ExamScheduleRepository examScheduleRepository) {
        this.examRepository = examRepository;
        this.examPaperRepository = examPaperRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.questionRepository = questionRepository;
        this.examScheduleRepository = examScheduleRepository;
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
                    paperDuration += question.getPreparationTimeSeconds() + question.getMaxResponseSeconds();
                }
            }
            if (paper.getTimeDurationSeconds() == null || paper.getTimeDurationSeconds() != paperDuration) {
                paper.setTimeDurationSeconds(paperDuration);
                examPaperRepository.save(paper);
            }
            maxDuration = Math.max(maxDuration, paperDuration);
        }

        exam.setExamTimeDurationSecond(maxDuration);
        requireActiveSchedulesStillFit(exam);
        examRepository.save(exam);
    }

    /**
     * Thời gian làm bài tăng lên có thể khiến ca thi đã lên lịch không còn đủ dài. Chặn tại đây thay vì
     * để lọt: mọi caller đều @Transactional nên thao tác sửa đề bị rollback, dữ liệu không bao giờ rơi
     * vào trạng thái ca thi ngắn hơn đề. Chỉ xét ca còn sẽ diễn ra (findByExamId đã loại DELETED).
     */
    private void requireActiveSchedulesStillFit(Exam exam) {
        var tooShortCount = examScheduleRepository.findByExamId(exam.getId()).stream()
            .filter(schedule -> schedule.getStatus() == ExamScheduleStatus.DRAFT
                || schedule.getStatus() == ExamScheduleStatus.PUBLISHED)
            .filter(schedule -> exam.isScheduleWindowShorterThanExamTime(
                schedule.getStartDate(), schedule.getEndDate()))
            .count();
        if (tooShortCount > 0) {
            throw new IllegalStateException(
                ExamScheduleWindowMessages.schedulesNoLongerFit((int) tooShortCount, exam));
        }
    }
}
