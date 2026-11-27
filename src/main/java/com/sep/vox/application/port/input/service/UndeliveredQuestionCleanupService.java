package com.sep.vox.application.port.input.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.PracticeItemResponseRepository;
import com.sep.vox.domain.repository.PracticePaperItemRepository;
import com.sep.vox.domain.repository.PracticeQuestionRepository;
import com.sep.vox.domain.repository.StudentQuestionExposureRepository;

/**
 * Returns the questions that were SELECTED but the student NEVER answered back to the bank, when the session closes.
 *
 * <p><b>Why needed.</b> Selecting one question writes three things, not one:
 *
 * <ol>
 *   <li>{@code practice_paper_items} -- the question went into the paper</li>
 *   <li>{@code student_question_exposures} -- <b>the "already seen" mark</b>, and every question-selection query
 *       filters on {@code exposure.id IS NULL}</li>
 *   <li>{@code practice_questions.usage_count += 1}</li>
 * </ol>
 *
 * The second one is what hurts: a question that was only selected and then abandoned <b>disappears forever</b> from
 * that student's bank, even though they never saw it.
 *
 * <p><b>When it happens.</b> Two paths, and the second is new:
 *
 * <ul>
 *   <li>A student is pushed a question and closes the app without answering -- this already existed.</li>
 *   <li>Prefetch on the Python side: the next question is generated in advance while the student is still
 *       in a follow-up, to hide the 10-40 second LLM call. When the follow-up chain ends normally
 *       that question is used right away; when the session breaks midway it is left over.</li>
 * </ul>
 *
 * <p><b>Only check the LAST slot.</b> A middle slot cannot be unanswered: if it were,
 * {@code ResolveNextPracticeQuestionClaimService} would have returned that very one (the idempotent branch
 * {@code existsResponse}) instead of picking a new question, so no new slot is ever inserted after it.
 *
 * <p><b>{@code REQUIRES_NEW}.</b> Called from {@code PracticeSessionClosedHandler} -- which already runs
 * in its own transaction and may throw out. A failed cleanup must NOT be allowed to drag down writing the interest score or
 * closing the session: the session being closed is the truth, this is only returning to the bank.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UndeliveredQuestionCleanupService {

    private final PracticePaperItemRepository paperItemRepository;
    private final PracticeItemResponseRepository practiceItemResponseRepository;
    private final StudentQuestionExposureRepository studentQuestionExposureRepository;
    private final PracticeQuestionRepository practiceQuestionRepository;

    /**
     * The item is deleted FIRST and the number of deleted rows is used as the guard: if another next-question run has
     * just inserted a new slot in between, the question just read is no longer the last slot and the statement returns 0.
     * We must stop then -- removing the exposure of a question that is still in use would push it back into the bank
     * while the student is about to be asked that very question.
     *
     * @param paperId the paper of the session that was just closed; {@code null} means there is nothing to clean up.
     * @return {@code true} if a question was really returned to the bank.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean releaseUndeliveredQuestion(UUID studentId, UUID sessionId, UUID paperId) {
        if (paperId == null) {
            return false;
        }
        UUID lastQuestionId = findUnansweredLastQuestionId(sessionId, paperId);
        if (lastQuestionId == null) {
            return false;
        }
        int deleted = paperItemRepository.deleteLastItemForPaper(paperId, lastQuestionId);
        if (deleted == 0) {
            return false;
        }
        studentQuestionExposureRepository.removeExposure(studentId, lastQuestionId);
        practiceQuestionRepository.decrementUsageCount(lastQuestionId);
        log.info(
            "Returning question {} to the bank: selected for session {} but the student has not answered.",
            lastQuestionId,
            sessionId
        );
        return true;
    }

    /** @return the last question of the paper when the student never answered it, otherwise {@code null}. */
    private UUID findUnansweredLastQuestionId(UUID sessionId, UUID paperId) {
        List<UUID> questionIds = paperItemRepository.findQuestionIdsForPaper(paperId);
        if (questionIds.isEmpty()) {
            return null;
        }
        UUID lastQuestionId = questionIds.getLast();
        if (practiceItemResponseRepository.existsResponse(sessionId, lastQuestionId)) {
            // The student ALREADY answered the last question -- nothing is left over.
            return null;
        }
        return lastQuestionId;
    }
}
