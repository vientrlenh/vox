package com.sep.vox.application.port.input.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.service.exam.ExamScheduleWindowMessages;
import com.sep.vox.domain.service.exam.PaperTimeCalculator;

/**
 * H.1: paper.timeDurationSeconds = tổng thời gian THẬT của mọi item đã gán câu hỏi trong mã đề --
 * preparationTimeSeconds + maxResponseSeconds, CỘNG thời lượng phát của asset AUDIO/VIDEO (xem
 * {@link PaperTimeCalculator}); exam.examTimeDurationSecond = MAX(paperDuration) trên mọi ExamPaper
 * của kỳ thi. Các field này tự động tính, không nhập tay ở frontend - gọi lại hàm này mỗi khi cấu
 * trúc 1 mã đề thay đổi.
 *
 * <p>Lưu ý: ước tính CHI PHÍ AI ({@code ClassTestTokenQuotaGuardService}) cố ý KHÔNG dùng con số này
 * mà tự tính lại phần billable, vì giây phát media không sinh chi phí AI nào.
 */
@Service
public class RecalculateExamTimeDurationService {

    private final ExamRepository examRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final ExamScheduleRepository examScheduleRepository;

    public RecalculateExamTimeDurationService(
            ExamRepository examRepository,
            ExamPaperRepository examPaperRepository,
            ExamPaperItemRepository examPaperItemRepository,
            QuestionRepository questionRepository,
            QuestionAssetRepository questionAssetRepository,
            ExamScheduleRepository examScheduleRepository) {
        this.examRepository = examRepository;
        this.examPaperRepository = examPaperRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.questionRepository = questionRepository;
        this.questionAssetRepository = questionAssetRepository;
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
            // Không mã đề nào ⇒ chưa tính được thời gian làm bài. Ghi 0 chứ không phải null: cùng
            // một trạng thái thì phải cùng một cách mã hoá, mà nhánh dưới đã ghi 0 cho trường hợp
            // có mã đề nhưng chưa gán câu hỏi nào. Không cần soi lại ca thi: 0 giây thì ca nào cũng
            // đủ dài.
            exam.setExamTimeDurationSecond(0);
            examRepository.save(exam);
            return;
        }

        // Hai query cho cả kỳ thi thay vì (1 query/mã đề + 1 query/câu hỏi). Hàm này chạy sau MỌI thao
        // tác sửa đề, nên kỳ thi 5 mã đề × 40 câu trước đây tốn ~200 query mỗi lần bấm lưu.
        var paperIds = papers.stream().map(paper -> paper.getId()).toList();
        var itemsByPaperId = examPaperItemRepository.findByPaperIdIn(paperIds).stream()
            .collect(Collectors.groupingBy(item -> item.getPaperId()));

        var questionIds = itemsByPaperId.values().stream()
            .flatMap(items -> items.stream())
            .map(item -> item.getQuestionId())
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        var questionById = questionRepository.findByIdIn(questionIds).stream()
            .collect(Collectors.toMap(question -> question.getId(), question -> question));
        // Thời lượng phát AUDIO/VIDEO cũng là thời gian thí sinh ngồi trong phòng, nên phải nằm trong
        // ngân sách đồng hồ -- xem PaperTimeCalculator để rõ vì sao ước tính CHI PHÍ lại không được
        // cộng phần này.
        var assetByQuestionId = PaperTimeCalculator.indexByQuestionId(
            questionAssetRepository.findByQuestionIdIn(questionIds));

        var maxDuration = 0;
        var changedPapers = new ArrayList<ExamPaper>();
        for (var paper : papers) {
            // List chứ không phải Set: một câu hỏi lặp lại trong cùng mã đề thì phải tính đủ số lần.
            var paperQuestions = new ArrayList<Question>();
            for (var item : itemsByPaperId.getOrDefault(paper.getId(), List.of())) {
                if (item.getQuestionId() == null) {
                    continue;
                }
                var question = questionById.get(item.getQuestionId());
                if (question != null) {
                    paperQuestions.add(question);
                }
            }
            var paperDuration = PaperTimeCalculator.breakdownOf(paperQuestions, assetByQuestionId).totalSeconds();
            if (paper.getTimeDurationSeconds() == null || paper.getTimeDurationSeconds() != paperDuration) {
                paper.setTimeDurationSeconds(paperDuration);
                changedPapers.add(paper);
            }
            maxDuration = Math.max(maxDuration, paperDuration);
        }
        if (!changedPapers.isEmpty()) {
            examPaperRepository.saveAll(changedPapers);
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
