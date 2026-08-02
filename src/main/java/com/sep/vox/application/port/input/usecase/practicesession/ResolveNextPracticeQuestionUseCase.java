package com.sep.vox.application.port.input.usecase.practicesession;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.service.PracticeQuestionSelectionService;
import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.domain.model.personalization.PracticePaperItem;
import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.personalization.PracticeItemResponseRepository;
import com.sep.vox.domain.repository.personalization.PracticePaperItemRepository;
import com.sep.vox.domain.repository.personalization.PracticeQuestionRepository;
import com.sep.vox.domain.repository.personalization.PracticeSessionRepository;
import com.sep.vox.domain.repository.personalization.PracticeTopicRepository;
import com.sep.vox.domain.repository.personalization.StudentQuestionExposureRepository;

/**
 * Resolve ĐÚNG 1 câu MAIN tiếp theo trong lúc phiên luyện đang chạy -- gọi từ Python (nội bộ,
 * PracticeInternalSecretFilter bảo vệ) khi decision.should_continue == False. Xem gói 11 mục
 * 2.4 bước 4.
 */
@Service
public class ResolveNextPracticeQuestionUseCase {

    public record QuestionPayload(
        UUID questionId,
        int slot,
        String questionText,
        String criterionCode,
        String subAttribute,
        int difficultyRank,
        int preparationTimeSeconds,
        int maxResponseSeconds,
        int maxFollowupSeconds,
        List<String> suggestedIdeas) {
    }

    public record Result(String status, String reason, QuestionPayload question) {

        static Result ok(QuestionPayload question) {
            return new Result("ok", null, question);
        }

        static Result noMoreQuestions(String reason) {
            return new Result("no_more_questions", reason, null);
        }
    }

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeTopicRepository topicRepository;
    private final PracticeQuestionRepository questionRepository;
    private final PracticePaperItemRepository paperItemRepository;
    private final PracticeItemResponseRepository practiceItemResponseRepository;
    private final StudentQuestionExposureRepository studentQuestionExposureRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final PracticeQuestionSelectionService selectionService;
    private final PracticeTopicOfferEnrichmentService enrichmentService;

    public ResolveNextPracticeQuestionUseCase(
            PracticeSessionRepository practiceSessionRepository,
            PracticeTopicRepository topicRepository,
            PracticeQuestionRepository questionRepository,
            PracticePaperItemRepository paperItemRepository,
            PracticeItemResponseRepository practiceItemResponseRepository,
            StudentQuestionExposureRepository studentQuestionExposureRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            PracticeQuestionSelectionService selectionService,
            PracticeTopicOfferEnrichmentService enrichmentService) {
        this.practiceSessionRepository = practiceSessionRepository;
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.paperItemRepository = paperItemRepository;
        this.practiceItemResponseRepository = practiceItemResponseRepository;
        this.studentQuestionExposureRepository = studentQuestionExposureRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.selectionService = selectionService;
        this.enrichmentService = enrichmentService;
    }

    @Transactional
    public Result execute(UUID sessionId) {
        // FOR UPDATE: serializes concurrent calls for the SAME session (e.g. Python's
        // timeout-retry racing the original still-in-flight request, see
        // practice_session_client.py) -- the second call blocks until the first commits,
        // then the idempotency check below sees its committed item instead of both
        // independently selecting/inserting a new one.
        var session = practiceSessionRepository.findByIdForUpdate(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên luyện."));
        if (!"IN_PROGRESS".equals(session.status())) {
            throw new IllegalStateException("Phiên luyện không còn hoạt động.");
        }

        var alreadyChosenIds = paperItemRepository.findQuestionIdsForPaper(session.practicePaperId());
        if (!alreadyChosenIds.isEmpty()) {
            var latestQuestionId = alreadyChosenIds.getLast();
            // The latest item was already resolved/committed by THIS call (or an earlier one
            // that timed out on Python's side before the response arrived, see infra/
            // practice_session_client.py's 8s timeout) but the student never got to answer it
            // -- a retry must return the SAME item, not pick a fresh one, or the timed-out
            // original leaves an orphaned paper item nothing will ever answer/grade.
            if (!practiceItemResponseRepository.existsResponse(sessionId, latestQuestionId)) {
                var latestQuestion = questionRepository.findById(latestQuestionId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi luyện."));
                return Result.ok(toPayload(latestQuestion, alreadyChosenIds.size()));
            }
        }

        var usedSeconds = paperItemRepository.sumPlannedSecondsForPaper(session.practicePaperId());
        var maxMinutes = schoolSubscriptionRepository
            .findMaxTimePerAttemptMinForUser(session.studentId());
        var maxSeconds = maxMinutes == null ? 0 : maxMinutes * 60;
        if (usedSeconds >= maxSeconds) {
            return Result.noMoreQuestions("budget_exhausted");
        }

        var topic = topicRepository.findTopicById(session.chosenPracticeTopicId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề luyện tập."));
        var focus = selectionService.resolveFocus(session.studentId(), null);
        var signal = enrichmentService.studentRankSignal(session.studentId());
        var baseRank = enrichmentService.rankForTopic(session.studentId(), topic.id(), signal);
        var alreadyChosen = questionRepository.findByIds(alreadyChosenIds);

        var selection = selectionService
            .resolveNextQuestion(topic, session.studentId(), focus, baseRank, alreadyChosen)
            .orElse(null);
        if (selection == null) {
            return Result.noMoreQuestions("pool_exhausted");
        }

        var question = selection.question();
        if (usedSeconds + question.plannedSeconds() > maxSeconds) {
            return Result.noMoreQuestions("budget_exhausted");
        }
        paperItemRepository.save(new PracticePaperItem(
            UUID.randomUUID(),
            session.practicePaperId(),
            question.id(),
            selection.slot(),
            selection.criterion(),
            selection.subAttribute(),
            selection.targetRank()
        ));
        studentQuestionExposureRepository.recordExposure(session.studentId(), question.id());

        return Result.ok(toPayload(question, selection.slot()));
    }

    private QuestionPayload toPayload(PracticeQuestion question, int slot) {
        return new QuestionPayload(
            question.id(),
            slot,
            question.questionText(),
            question.targetCriterionCode(),
            question.targetSubAttribute(),
            question.difficultyRank(),
            question.preparationTimeSeconds(),
            question.maxResponseSeconds(),
            question.maxFollowupSeconds(),
            parseIdeas(question.suggestedIdeasJson())
        );
    }

    private List<String> parseIdeas(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return new tools.jackson.databind.json.JsonMapper()
                .readValue(value, new tools.jackson.core.type.TypeReference<List<String>>() {
                });
        } catch (Exception exception) {
            return List.of();
        }
    }
}
