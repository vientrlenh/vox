package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.personalization.PracticePaper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.personalization.PracticePaperItem;
import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.repository.LearnerProfileRepository;
import com.sep.vox.domain.repository.PracticePaperItemRepository;
import com.sep.vox.domain.repository.PracticePaperRepository;
import com.sep.vox.domain.repository.StudentQuestionExposureRepository;

/**
 * Writes PracticePaper + PracticePaperItem + exposure -- split out of
 * BuildPracticePaperUseCase so @Transactional wraps only the DB write, not
 * the external calls (Python agents) that come before it. A separate bean
 * (not a private/protected method of the same class) because self-invocation bypasses the
 * Spring AOP proxy -- @Transactional on a method of the same class would be ignored
 * when called through this.
 */
@Service
@RequiredArgsConstructor
public class PracticePaperPersistenceService {

    /** How long quota stays reserved for a paper that was built but has not entered a session yet. */
    private static final Duration RESERVATION_WINDOW = Duration.ofMinutes(10);

    private final PracticePaperRepository paperRepository;
    private final PracticePaperItemRepository paperItemRepository;
    private final StudentQuestionExposureRepository studentQuestionExposureRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final JsonSerializationPort jsonSerializationPort;

    @Transactional
    public com.sep.vox.domain.model.personalization.PracticePaper persist(
            UUID studentId,
            UUID topicId,
            UUID targetFrameworkBandId,
            String origin,
            List<UUID> offeredTopicIds,
            List<UUID> previousOfferedTopicIds,
            PracticeQuestion question,
            PracticeQuestionSelectionService.NextQuestionSelection selection) {
        PracticePaper paper = createPaper(
            studentId, topicId, targetFrameworkBandId, origin,
            offeredTopicIds, previousOfferedTopicIds, question
        );
        saveItemAndExposure(studentId, paper.getId(), selection);
        return paper;
    }

    private String currentGoal(UUID studentId) {
        return learnerProfileRepository.findCurrent(studentId)
            .map(profile -> profile.getGoalType() == null ? "ABILITY_IMPROVEMENT" : profile.getGoalType())
            .orElse("ABILITY_IMPROVEMENT");
    }

    private com.sep.vox.domain.model.personalization.PracticePaper createPaper(
            UUID studentId,
            UUID topicId,
            UUID targetFrameworkBandId,
            String origin,
            List<UUID> offeredTopicIds,
            List<UUID> previousOfferedTopicIds,
            PracticeQuestion question) {
        Instant now = Instant.now();
        return paperRepository.save(PracticePaper.reserved(
            studentId,
            topicId,
            targetFrameworkBandId,
            origin,
            currentGoal(studentId),
            jsonSerializationPort.toJson(offeredTopicIds == null ? List.of() : offeredTopicIds),
            jsonSerializationPort.toJson(previousOfferedTopicIds == null ? List.of() : previousOfferedTopicIds),
            question.spokenSeconds(),
            // Instant has no plusMinutes -- it is an absolute point that carries no calendar or time zone, so only a
            // Duration can be added. The meaning is unchanged: reserve the quota for 10 minutes.
            now.plus(RESERVATION_WINDOW),
            now
        ));
    }

    private void saveItemAndExposure(
            UUID studentId,
            UUID paperId,
            PracticeQuestionSelectionService.NextQuestionSelection selection) {
        paperItemRepository.save(PracticePaperItem.forSlot(
                paperId,
                selection.question().getId(),
                selection.slot(),
                selection.criterion(),
                selection.subAttribute(),
                selection.targetRank()));
        studentQuestionExposureRepository.recordExposure(studentId, selection.question().getId());
    }
}
