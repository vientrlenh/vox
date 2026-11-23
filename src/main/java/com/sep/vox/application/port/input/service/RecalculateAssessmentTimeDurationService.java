package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.question.QuestionAsset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.service.assessment.AssessmentScheduleWindowMessages;
import com.sep.vox.domain.service.assessment.PaperTimeCalculator;

/**
 * H.1: paper.timeDurationSeconds = the REAL total time of every item that has a question assigned in the paper variant --
 * preparationTimeSeconds + maxResponseSeconds, PLUS the playback duration of AUDIO/VIDEO assets (see
 * {@link PaperTimeCalculator}); assessment.assessmentTimeDurationSecond = MAX(paperDuration) over every AssessmentPaper
 * of the assessment. These fields are computed automatically, not typed by hand in the frontend - call this function again whenever the
 * structure of a paper variant changes.
 *
 * <p>Note: the AI COST estimate ({@code ClassTestTokenQuotaGuardService}) deliberately does NOT use this number
 * and recomputes the billable part itself, because media playback seconds produce no AI cost.
 */
@Service
@RequiredArgsConstructor
public class RecalculateAssessmentTimeDurationService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;

    @Transactional
    public void recalculate(UUID assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId).orElse(null);
        if (assessment == null) {
            return;
        }

        List<AssessmentPaper> papers = assessmentPaperRepository.findByAssessmentId(assessmentId);
        if (papers.isEmpty()) {
            // Không mã đề nào ⇒ chưa tính được thời gian làm bài. Ghi 0 chứ không phải null: cùng
            // một trạng thái thì phải cùng một cách mã hoá, mà nhánh dưới đã ghi 0 cho trường hợp
            // có mã đề nhưng chưa gán câu hỏi nào. Không cần soi lại ca thi: 0 giây thì ca nào cũng
            // đủ dài.
            assessment.setAssessmentTimeDurationSecond(0);
            assessmentRepository.save(assessment);
            return;
        }

        // Hai query cho cả kỳ thi thay vì (1 query/mã đề + 1 query/câu hỏi). Hàm này chạy sau MỌI thao
        // tác sửa đề, nên kỳ thi 5 mã đề × 40 câu trước đây tốn ~200 query mỗi lần bấm lưu.
        List<UUID> paperIds = papers.stream().map(paper -> paper.getId()).toList();
        Map<UUID, List<AssessmentPaperItem>> itemsByPaperId = assessmentPaperItemRepository.findByPaperIdIn(paperIds).stream()
            .collect(Collectors.groupingBy(item -> item.getPaperId()));

        List<UUID> questionIds = itemsByPaperId.values().stream()
            .flatMap(items -> items.stream())
            .map(item -> item.getQuestionId())
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<UUID, Question> questionById = questionRepository.findByIdIn(questionIds).stream()
            .collect(Collectors.toMap(question -> question.getId(), question -> question));
        // Thời lượng phát AUDIO/VIDEO cũng là thời gian thí sinh ngồi trong phòng, nên phải nằm trong
        // ngân sách đồng hồ -- xem PaperTimeCalculator để rõ vì sao ước tính CHI PHÍ lại không được
        // cộng phần này.
        Map<UUID, QuestionAsset> assetByQuestionId = PaperTimeCalculator.indexByQuestionId(
            questionAssetRepository.findByQuestionIdIn(questionIds));

        int maxDuration = 0;
        List<AssessmentPaper> changedPapers = new ArrayList<AssessmentPaper>();
        for (AssessmentPaper paper : papers) {
            // List chứ không phải Set: một câu hỏi lặp lại trong cùng mã đề thì phải tính đủ số lần.
            List<Question> paperQuestions = new ArrayList<Question>();
            for (AssessmentPaperItem item : itemsByPaperId.getOrDefault(paper.getId(), List.of())) {
                if (item.getQuestionId() == null) {
                    continue;
                }
                Question question = questionById.get(item.getQuestionId());
                if (question != null) {
                    paperQuestions.add(question);
                }
            }
            int paperDuration = PaperTimeCalculator.breakdownOf(paperQuestions, assetByQuestionId).totalSeconds();
            if (paper.getTimeDurationSeconds() == null || paper.getTimeDurationSeconds() != paperDuration) {
                paper.setTimeDurationSeconds(paperDuration);
                changedPapers.add(paper);
            }
            maxDuration = Math.max(maxDuration, paperDuration);
        }
        if (!changedPapers.isEmpty()) {
            assessmentPaperRepository.saveAll(changedPapers);
        }

        assessment.setAssessmentTimeDurationSecond(maxDuration);
        requireActiveSchedulesStillFit(assessment);
        assessmentRepository.save(assessment);
    }

    /**
     * A longer attempt time may make an already scheduled session no longer long enough. Blocked here instead of
     * letting it through: every caller is @Transactional so the paper edit is rolled back, and the data never falls
     * into a state where a session is shorter than the paper. Only sessions that will still take place are considered (findByAssessmentId already excludes DELETED).
     */
    private void requireActiveSchedulesStillFit(Assessment assessment) {
        long tooShortCount = assessmentScheduleRepository.findByAssessmentId(assessment.getId()).stream()
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
