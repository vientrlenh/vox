package com.sep.vox.application.port.input.usecase.practiceplanning;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.QuotaExceededException;
import com.sep.vox.application.mapper.practiceplanning.PracticePlanningResponseMapper;
import com.sep.vox.application.port.input.command.BuildPracticePaperCommand;
import com.sep.vox.application.port.input.service.PracticeQuestionSelectionService;
import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaper;
import com.sep.vox.domain.mapper.PracticePaperDtoMapper;
import com.sep.vox.domain.model.personalization.PracticePaperItem;
import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.personalization.LearnerProfileRepository;
import com.sep.vox.domain.repository.personalization.PracticePaperItemRepository;
import com.sep.vox.domain.repository.personalization.PracticePaperRepository;
import com.sep.vox.domain.repository.personalization.PracticeTopicRepository;
import com.sep.vox.domain.repository.personalization.StudentQuestionExposureRepository;

/**
 * Dựng đề luyện -- CHỈ câu MAIN đầu tiên (đúng README mục 4.1: vào phiên khi có 1 câu phù hợp
 * là đủ, không đợi dựng cả đề). Các câu MAIN tiếp theo được resolve trong lúc phiên chạy, xem
 * ResolveNextPracticeQuestionUseCase.
 */
@Service
public class BuildPracticePaperUseCase implements IUseCase<BuildPracticePaperCommand, PracticePaper> {

    private final PracticeTopicRepository topicRepository;
    private final PracticePaperRepository paperRepository;
    private final PracticePaperItemRepository paperItemRepository;
    private final StudentQuestionExposureRepository studentQuestionExposureRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final PracticeTopicOfferEnrichmentService enrichmentService;
    private final PracticeQuestionSelectionService selectionService;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserContextPort userContextPort;

    public BuildPracticePaperUseCase(
            PracticeTopicRepository topicRepository,
            PracticePaperRepository paperRepository,
            PracticePaperItemRepository paperItemRepository,
            StudentQuestionExposureRepository studentQuestionExposureRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            LearnerProfileRepository learnerProfileRepository,
            PracticeTopicOfferEnrichmentService enrichmentService,
            PracticeQuestionSelectionService selectionService,
            JsonSerializationPort jsonSerializationPort,
            UserContextPort userContextPort) {
        this.topicRepository = topicRepository;
        this.paperRepository = paperRepository;
        this.paperItemRepository = paperItemRepository;
        this.studentQuestionExposureRepository = studentQuestionExposureRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.learnerProfileRepository = learnerProfileRepository;
        this.enrichmentService = enrichmentService;
        this.selectionService = selectionService;
        this.jsonSerializationPort = jsonSerializationPort;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public PracticePaper execute(BuildPracticePaperCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var topic = requireTopic(input.topicId());
        var quotaRemaining = remainingPracticeQuota(studentId);
        var focus = selectionService.resolveFocus(studentId, input.fromSubAttribute());
        var baseRank = estimatedBaseRank(studentId, input.topicId());
        var selection = selectionService
            .resolveNextQuestion(topic, studentId, focus, baseRank, List.of())
            .orElseThrow(() -> new NotFoundException("Chủ đề chưa có câu luyện phù hợp."));
        var question = selection.question();
        if (quotaRemaining < question.spokenSeconds()) {
            throw new QuotaExceededException("Hạn mức PRACTICE không đủ cho một câu trọn vẹn.");
        }
        var paper = createPaper(
            studentId,
            input.topicId(),
            input.origin(),
            input.offeredTopicIds(),
            input.previousOfferedTopicIds(),
            question
        );
        saveItemAndExposure(studentId, paper.id(), selection);
        return PracticePlanningResponseMapper.toResponse(
            PracticePaperDtoMapper.toDto(paper, List.of(question))
        );
    }

    private int remainingPracticeQuota(UUID studentId) {
        var quota = schoolSubscriptionRepository.findPracticeQuotaRemaining(studentId);
        var reserved = paperRepository.sumReservedQuotaSeconds(studentId);
        return Math.max(0, quota - reserved);
    }

    private int estimatedBaseRank(UUID studentId, UUID topicId) {
        var signal = enrichmentService.studentRankSignal(studentId);
        return enrichmentService.rankForTopic(studentId, topicId, signal);
    }

    private String currentGoal(UUID studentId) {
        return learnerProfileRepository.findCurrent(studentId)
            .map(profile -> profile.goalType() == null ? "ABILITY_IMPROVEMENT" : profile.goalType())
            .orElse("ABILITY_IMPROVEMENT");
    }

    private com.sep.vox.domain.model.personalization.PracticePaper createPaper(
            UUID studentId,
            UUID topicId,
            String origin,
            List<UUID> offeredTopicIds,
            List<UUID> previousOfferedTopicIds,
            PracticeQuestion question) {
        var resolvedOrigin = origin == null ? "SELECTED" : origin;
        var now = OffsetDateTime.now();
        return paperRepository.save(new com.sep.vox.domain.model.personalization.PracticePaper(
            UUID.randomUUID(),
            studentId,
            topicId,
            resolvedOrigin,
            currentGoal(studentId),
            jsonSerializationPort.toJson(
                offeredTopicIds == null ? List.of() : offeredTopicIds
            ),
            jsonSerializationPort.toJson(
                previousOfferedTopicIds == null ? List.of() : previousOfferedTopicIds
            ),
            question.plannedSeconds(),
            question.spokenSeconds(),
            now.plusMinutes(10),
            "RESERVED",
            now
        ));
    }

    private void saveItemAndExposure(
            UUID studentId,
            UUID paperId,
            PracticeQuestionSelectionService.NextQuestionSelection selection) {
        paperItemRepository.save(new PracticePaperItem(
            UUID.randomUUID(),
            paperId,
            selection.question().id(),
            selection.slot(),
            selection.criterion(),
            selection.subAttribute(),
            selection.targetRank()
        ));
        studentQuestionExposureRepository.recordExposure(studentId, selection.question().id());
    }

    private PracticeTopic requireTopic(UUID topicId) {
        return topicRepository.findTopicById(topicId)
            .filter(PracticeTopic::active)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề luyện tập."));
    }
}
