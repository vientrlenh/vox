package com.sep.vox.application.port.input.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.repository.assessment.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.service.assessment.AssessmentScheduleWindowMessages;
import com.sep.vox.domain.service.assessment.PaperTimeCalculator;

/**
 * H.1: paper.timeDurationSeconds = tổng thời gian THẬT của mọi item đã gán câu hỏi trong mã đề --
 * preparationTimeSeconds + maxResponseSeconds, CỘNG thời lượng phát của asset AUDIO/VIDEO (xem
 * {@link PaperTimeCalculator}); assessment.timeDurationSecond = MAX(paperDuration) trên mọi AssessmentPaper
 * của kỳ thi. Các field này tự động tính, không nhập tay ở frontend - gọi lại hàm này mỗi khi cấu
 * trúc 1 mã đề thay đổi.
 *
 * <p>Lưu ý: ước tính CHI PHÍ AI ({@code ClassTestTokenQuotaGuardService}) cố ý KHÔNG dùng con số này
 * mà tự tính lại phần billable, vì giây phát media không sinh chi phí AI nào.
 */
@Service
public class RecalculateAssessmentTimeDurationService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;

    public RecalculateAssessmentTimeDurationService(
            AssessmentRepository assessmentRepository,
            AssessmentPaperRepository assessmentPaperRepository,
            AssessmentPaperItemRepository assessmentPaperItemRepository,
            QuestionRepository questionRepository,
            QuestionAssetRepository questionAssetRepository,
            AssessmentScheduleRepository assessmentScheduleRepository) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentPaperRepository = assessmentPaperRepository;
        this.assessmentPaperItemRepository = assessmentPaperItemRepository;
        this.questionRepository = questionRepository;
        this.questionAssetRepository = questionAssetRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
    }

    @Transactional
    public void recalculate(UUID assessmentId) {
        var assessment = assessmentRepository.findById(assessmentId).orElse(null);
        if (assessment == null) {
            return;
        }

        var papers = assessmentPaperRepository.findByAssessmentId(assessmentId);
        if (papers.isEmpty()) {
            // Không mã đề nào ⇒ chưa tính được thời gian làm bài. Ghi 0 chứ không phải null: cùng
            // một trạng thái thì phải cùng một cách mã hoá, mà nhánh dưới đã ghi 0 cho trường hợp
            // có mã đề nhưng chưa gán câu hỏi nào. Không cần soi lại ca thi: 0 giây thì ca nào cũng
            // đủ dài.
            assessment.setTimeDurationSecond(0);
            assessmentRepository.save(assessment);
            return;
        }

        // Hai query cho cả kỳ thi thay vì (1 query/mã đề + 1 query/câu hỏi). Hàm này chạy sau MỌI thao
        // tác sửa đề, nên kỳ thi 5 mã đề × 40 câu trước đây tốn ~200 query mỗi lần bấm lưu.
        var paperIds = papers.stream().map(paper -> paper.getId()).toList();
        var itemsByPaperId = assessmentPaperItemRepository.findByPaperIdIn(paperIds).stream()
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
        var changedPapers = new ArrayList<AssessmentPaper>();
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
            assessmentPaperRepository.saveAll(changedPapers);
        }

        assessment.setTimeDurationSecond(maxDuration);
        requireActiveSchedulesStillFit(assessment);
        assessmentRepository.save(assessment);
    }

    /**
     * Thời gian làm bài tăng lên có thể khiến ca thi đã lên lịch không còn đủ dài. Chặn tại đây thay vì
     * để lọt: mọi caller đều @Transactional nên thao tác sửa đề bị rollback, dữ liệu không bao giờ rơi
     * vào trạng thái ca thi ngắn hơn đề. Chỉ xét ca còn sẽ diễn ra (findByAssessmentId đã loại DELETED).
     */
    private void requireActiveSchedulesStillFit(Assessment assessment) {
        var tooShortCount = assessmentScheduleRepository.findByAssessmentId(assessment.getId()).stream()
            .filter(schedule -> schedule.getStatus() == AssessmentScheduleStatus.DRAFT
                || schedule.getStatus() == AssessmentScheduleStatus.PUBLISHED)
            .filter(schedule -> assessment.isScheduleWindowShorterThanAssessmentTime(
                schedule.getStartDate(), schedule.getEndDate()))
            .count();
        if (tooShortCount > 0) {
            throw new IllegalStateException(
                AssessmentScheduleWindowMessages.schedulesNoLongerFit((int) tooShortCount, assessment));
        }
    }
}
