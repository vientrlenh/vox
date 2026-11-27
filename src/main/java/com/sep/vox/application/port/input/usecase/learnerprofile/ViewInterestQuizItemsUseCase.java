package com.sep.vox.application.port.input.usecase.learnerprofile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.mapper.learnerprofile.LearnerProfileResponseMapper;
import com.sep.vox.application.port.input.service.InterestQuizItemSelector;
import com.sep.vox.application.port.input.service.InterestQuizScorer;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.InterestQuizConfigPort;
import com.sep.vox.application.port.output.InterestQuizGenerationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.LearnerProfileRepository;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.InterestQuizItem;
import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;
import com.sep.vox.domain.repository.InterestQuizItemRepository;
import com.sep.vox.domain.repository.TopicInterestEventRepository;

/**
 * Package 13 -- generates the interest quiz from situations with AI (Verbalized Sampling, not personalized by
 * history). See task/implement/13-quiz-so-thich-sinh-theo-tinh-huong.md section 3.
 */
@Service
@RequiredArgsConstructor
public class ViewInterestQuizItemsUseCase implements IUseCase<Void, List<InterestQuizItem>> {

    private final InterestQuizItemRepository quizItemRepository;
    private final TopicInterestEventRepository topicInterestEventRepository;
    private final InterestQuizGenerationPort quizGenerationPort;
    private final UserContextPort userContextPort;
    private final InterestQuizScorer interestQuizScorer;
    private final InterestQuizItemSelector itemSelector;
    private final InterestQuizConfigPort quizConfig;
    private final LearnerProfileRepository learnerProfileRepository;

    // Not @Transactional -- quizGenerationPort.generate() calls a slow LLM (Python agents, 10-20s). Wrapping it in a
    // transaction once exhausted the HikariCP pool under load (see BuildPracticePaperUseCase/
    // PracticeQuestionGenerationService, fixed in the same round). Each repository call below runs its own
    // transaction through the Spring Data proxy.
    @Override
    public List<InterestQuizItem> execute(Void input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();

        if (quizItemRepository.hasQuizItemsForStudent(studentId)) {
            return toResponse(selectForStudent(studentId));
        }

        List<InterestQuizSeedItem> sharedPool = selectBalanced(quizItemRepository.findAllActiveQuizItems());

        // There is already a real interest signal (from earlier interactions/practice) -- no need to generate a separate
        // quiz, use the original static set. Personalization should not live in the quiz generation step (section 0 of
        // task 13), it belongs to the practice topic selection step (already correct, left untouched).
        if (!topicInterestEventRepository.findByStudent(studentId).isEmpty()) {
            return toResponse(sharedPool);
        }

        return generateForStudent(studentId, sharedPool);
    }

    /**
     * Redo the quiz at the student's request.
     *
     * <p>Rule: if the old set is NOT yet fully submitted, return EXACTLY that set to continue -- generating a new set
     * midway throws away the work the student already did, and the answers already chosen would point to items that
     * were just switched off. Only when {@code quizCompletedAt} is set is the old set switched off and a new one
     * generated.
     *
     * <p>Not {@code @Transactional}: same as {@link #execute} -- there is an LLM call of 10-20 seconds inside, and
     * holding a transaction for that long once exhausted the connection pool.
     */
    public List<InterestQuizItem> regenerate() {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();

        Boolean completed = learnerProfileRepository.findCurrent(studentId)
            .map(profile -> profile.getQuizCompletedAt() != null)
            .orElse(false);
        if (!completed) {
            // Not finished -> continue with the old set, touch no data.
            return execute(null);
        }

        quizItemRepository.deactivateGeneratedForStudent(studentId);
        List<InterestQuizSeedItem> sharedPool = selectBalanced(quizItemRepository.findAllActiveQuizItems());
        return generateForStudent(studentId, sharedPool);
    }

    /** Generates the student's own set and saves it. When generation returns nothing, falls back to the shared static set instead of returning empty. */
    private List<InterestQuizItem> generateForStudent(
            UUID studentId, List<InterestQuizSeedItem> sharedPool) {
        List<InterestQuizSeedItem> generated = generateQuizItems(sharedPool);
        if (generated.isEmpty()) {
            // Python/LLM not available -- fall back to the original static set instead of returning empty to the student.
            return toResponse(sharedPool);
        }
        quizItemRepository.saveGeneratedForStudent(studentId, generated);
        return toResponse(selectForStudent(studentId));
    }

    // Generates SYNCHRONOUSLY, then writes, then returns -- the student gets their own set on the very first call.
    //
    // The write is not moved to the background: the slow part is the LLM, not the INSERT (7 rows, a few
    // milliseconds). Splitting it saves nothing noticeable and breaks elsewhere -- the id is generated by the DB
    // (uuidv7, insertable=false), so returning before the write leaves every item with a null id and the student
    // submitting answers by itemId breaks.
    //
    // What really had to be fixed is speed: the old version made ONE LLM call for all 7 items and made the model
    // balance the dimensions itself -- 46.8 seconds measured, over both the Tomcat and the client ceiling.
    // Python now splits it into 7 parallel calls, each for one item with its dimension triple and context already
    // assigned, at a low reasoning level.
    private List<InterestQuizSeedItem> generateQuizItems(List<InterestQuizSeedItem> sharedPool) {
        List<String> existingStatements = sharedPool.stream()
            .flatMap(item -> item.getStatements().stream())
            .toList();
        return quizGenerationPort.generate(
            quizConfig.itemCount(),
            existingStatements,
            interestQuizScorer.quizDimensionCodes()
        );
    }

    /**
     * Candidates = the items generated for this student + the WHOLE shared pool.
     *
     * Not only the student's own items, for two reasons that both leave the student stuck:
     * 1. An older student's own items only carry the dimensions that EXISTED WHEN THEY WERE GENERATED. When an admin
     *    adds a new dimension later, the shared pool is topped up but the own items are not -- the new dimension
     *    would never be asked of students who already onboarded.
     * 2. If a dimension is switched off, every own item containing it is dropped; the student may be left with
     *    fewer than 5 items, while submitQuiz requires 5-7 answers -> cannot submit, no way out.
     *
     * Own items are placed FIRST so that when the coverage score ties they still win (the selector only replaces a
     * candidate when the score is strictly HIGHER, so on a tie the earlier one is kept).
     */
    private List<InterestQuizSeedItem> selectForStudent(UUID studentId) {
        Map<UUID, InterestQuizSeedItem> pool = new LinkedHashMap<UUID, InterestQuizSeedItem>();
        for (InterestQuizSeedItem item : quizItemRepository.findAllActiveQuizItemsForStudent(studentId)) {
            pool.put(item.getId(), item);
        }
        for (InterestQuizSeedItem item : quizItemRepository.findAllActiveQuizItems()) {
            pool.putIfAbsent(item.getId(), item);
        }
        return selectBalanced(List.copyOf(pool.values()));
    }

    /** Drops items that contain a dimension that can no longer be asked + covers the dimensions evenly within the exact
     * question budget (app.personalization.quiz.item-count) -- see InterestQuizItemSelector for why both are
     * needed. */
    private List<InterestQuizSeedItem> selectBalanced(List<InterestQuizSeedItem> pool) {
        return itemSelector.select(pool, quizConfig.itemCount());
    }

    private List<InterestQuizItem> toResponse(List<InterestQuizSeedItem> items) {
        return LearnerProfileResponseMapper.toResponse(items);
    }
}
