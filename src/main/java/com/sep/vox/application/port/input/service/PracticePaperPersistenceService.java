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
 * Ghi PracticePaper + PracticePaperItem + exposure -- tách riêng khỏi
 * BuildPracticePaperUseCase để @Transactional chỉ bọc đúng phần ghi DB, không
 * bọc luôn phần gọi ra ngoài (Python agents) phía trước nó. Là bean riêng
 * (không phải method private/protected cùng class) vì self-invocation bỏ qua
 * proxy AOP của Spring -- @Transactional trên method cùng class sẽ bị lờ đi
 * nếu gọi qua this.
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
        String resolvedOrigin = origin == null ? "SELECTED" : origin;
        Instant now = Instant.now();
        return paperRepository.save(com.sep.vox.domain.model.personalization.PracticePaper.builder()
            .studentId(studentId)
            .practiceTopicId(topicId)
            .targetFrameworkBandId(targetFrameworkBandId)
            .origin(resolvedOrigin)
            .goalAtBuild(currentGoal(studentId))
            .offeredTopicIdsJson(jsonSerializationPort.toJson(
                offeredTopicIds == null ? List.of() : offeredTopicIds
            ))
            .previousOfferedTopicIdsJson(jsonSerializationPort.toJson(
                previousOfferedTopicIds == null ? List.of() : previousOfferedTopicIds
            ))
            // The old plannedSeconds was preparationTimeSeconds + spokenSeconds; with the preparation column dropped (V11)
            // the two numbers are equal, so they are merged into one name instead of keeping two names for one value.
            .plannedSeconds(question.spokenSeconds())
            .reservedQuotaSeconds(question.spokenSeconds())
            // Instant has no plusMinutes -- it is an absolute point that carries no calendar or time zone, so only a
            // Duration can be added. The meaning is unchanged: reserve the quota for 10 minutes.
            .expiresAt(now.plus(RESERVATION_WINDOW))
            .status("RESERVED")
            .createdAt(now)
            .build());
    }

    private void saveItemAndExposure(
            UUID studentId,
            UUID paperId,
            PracticeQuestionSelectionService.NextQuestionSelection selection) {
        paperItemRepository.save(PracticePaperItem.builder()
                .practicePaperId(paperId)
                .practiceQuestionId(selection.question().getId())
                .slotOrder(selection.slot())
                .targetCriterionCode(selection.criterion())
                .targetSubAttribute(selection.subAttribute())
                .targetDifficultyRank(selection.targetRank())
                .build());
        studentQuestionExposureRepository.recordExposure(studentId, selection.question().getId());
    }
}
