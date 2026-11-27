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
            storeZeroDuration(assessment);
            return;
        }

        int maxDuration = recalculatePaperDurations(papers);
        assessment.setAssessmentTimeDurationSecond(maxDuration);
        requireActiveSchedulesStillFit(assessment);
        assessmentRepository.save(assessment);
    }

    /**
     * No paper variant means the attempt time cannot be computed yet. Writes 0 rather than null: the same
     * state must be encoded the same way, and the branch below already writes 0 for the case of a paper variant with
     * no question assigned yet. There is no need to re-check the schedules: with 0 seconds every session is long enough.
     */
    private void storeZeroDuration(Assessment assessment) {
        assessment.setAssessmentTimeDurationSecond(0);
        assessmentRepository.save(assessment);
    }

    /**
     * Two queries for the whole assessment instead of (1 query per paper variant + 1 query per question). This function
     * runs after EVERY paper edit, so an assessment of 5 variants x 40 questions used to cost ~200 queries on each save click.
     *
     * @return the longest paper duration in seconds
     */
    private int recalculatePaperDurations(List<AssessmentPaper> papers) {
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
        // The playback duration of AUDIO/VIDEO is also time the candidate sits in the room, so it must be inside the
        // clock budget -- see PaperTimeCalculator for why the COST estimate must not add this part.
        Map<UUID, QuestionAsset> assetByQuestionId = PaperTimeCalculator.indexByQuestionId(
            questionAssetRepository.findByQuestionIdIn(questionIds));
        return applyPaperDurations(papers, itemsByPaperId, questionById, assetByQuestionId);
    }

    private int applyPaperDurations(
            List<AssessmentPaper> papers, Map<UUID, List<AssessmentPaperItem>> itemsByPaperId,
            Map<UUID, Question> questionById, Map<UUID, QuestionAsset> assetByQuestionId) {
        int maxDuration = 0;
        List<AssessmentPaper> changedPapers = new ArrayList<AssessmentPaper>();
        for (AssessmentPaper paper : papers) {
            List<Question> paperQuestions = questionsOfPaper(itemsByPaperId.getOrDefault(paper.getId(), List.of()), questionById);
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
        return maxDuration;
    }

    /** A List, not a Set: a question repeated within the same paper variant must be counted every time it appears. */
    private List<Question> questionsOfPaper(List<AssessmentPaperItem> items, Map<UUID, Question> questionById) {
        List<Question> paperQuestions = new ArrayList<Question>();
        for (AssessmentPaperItem item : items) {
            if (item.getQuestionId() == null) {
                continue;
            }
            Question question = questionById.get(item.getQuestionId());
            if (question != null) {
                paperQuestions.add(question);
            }
        }
        return paperQuestions;
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
            throw new IllegalArgumentException(
                AssessmentScheduleWindowMessages.schedulesNoLongerFit((int) tooShortCount, assessment));
        }
    }
}
