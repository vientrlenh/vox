package com.sep.vox.application.port.input.usecase.practiceplanning;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.QuotaExceededException;
import com.sep.vox.application.mapper.practiceplanning.PracticePlanningResponseMapper;
import com.sep.vox.application.port.input.command.BuildPracticePaperCommand;
import com.sep.vox.application.port.input.service.PracticePaperPersistenceService;
import com.sep.vox.application.port.input.service.PracticeQuestionSelectionService;
import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaper;
import com.sep.vox.domain.mapper.PracticePaperDtoMapper;
import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.personalization.PracticePaperRepository;
import com.sep.vox.domain.repository.personalization.PracticeTopicRepository;

/**
 * Dựng đề luyện -- CHỈ câu MAIN đầu tiên (đúng README mục 4.1: vào phiên khi có 1 câu phù hợp
 * là đủ, không đợi dựng cả đề). Các câu MAIN tiếp theo được resolve trong lúc phiên chạy, xem
 * ResolveNextPracticeQuestionUseCase.
 */
@Service
public class BuildPracticePaperUseCase implements IUseCase<BuildPracticePaperCommand, PracticePaper> {

    private final PracticeTopicRepository topicRepository;
    private final PracticePaperRepository paperRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final PracticeTopicOfferEnrichmentService enrichmentService;
    private final PracticeQuestionSelectionService selectionService;
    private final PracticePaperPersistenceService persistenceService;
    private final UserContextPort userContextPort;
    private final ViewPracticeTopicOffersUseCase viewPracticeTopicOffersUseCase;

    public BuildPracticePaperUseCase(
            PracticeTopicRepository topicRepository,
            PracticePaperRepository paperRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            PracticeTopicOfferEnrichmentService enrichmentService,
            PracticeQuestionSelectionService selectionService,
            PracticePaperPersistenceService persistenceService,
            UserContextPort userContextPort,
            ViewPracticeTopicOffersUseCase viewPracticeTopicOffersUseCase) {
        this.viewPracticeTopicOffersUseCase = viewPracticeTopicOffersUseCase;
        this.topicRepository = topicRepository;
        this.paperRepository = paperRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.enrichmentService = enrichmentService;
        this.selectionService = selectionService;
        this.persistenceService = persistenceService;
        this.userContextPort = userContextPort;
    }

    @Override
    public PracticePaper execute(BuildPracticePaperCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var topic = requireTopic(input.topicId());
        var quotaRemaining = remainingPracticeQuota(studentId);
        var focus = selectionService.resolveFocus(studentId, input.fromSubAttribute());
        var baseRank = estimatedBaseRank(studentId, input.topicId());
        // Deliberately NOT wrapped in a Spring transaction: resolveNextQuestion can call out
        // to the Python agents service (diversity check, and -- on a thin/new topic -- live
        // LLM question generation, which alone can take 10+ seconds). Holding a HikariCP
        // connection open for that whole external call starves the pool under load; confirmed
        // via HikariCP's own leak-detector firing on this exact method. Each individual
        // repository call below still gets its own short-lived transaction from Spring Data's
        // repository proxy, so no atomicity is lost here -- only the final paper/item/exposure
        // write (persistenceService.persist, a separate bean so @Transactional actually applies
        // -- self-invocation within this class would bypass the AOP proxy) needs a real
        // transaction, scoped separately there.
        var selection = selectionService
            .resolveNextQuestion(topic, studentId, focus, baseRank, List.of())
            .orElseThrow(() -> new NotFoundException("Chủ đề chưa có câu luyện phù hợp."));
        var question = selection.question();
        if (quotaRemaining < question.spokenSeconds()) {
            throw new QuotaExceededException("Hạn mức PRACTICE không đủ cho một câu trọn vẹn.");
        }
        var paper = persistenceService.persist(
            studentId,
            input.topicId(),
            resolveOrigin(studentId, input),
            input.offeredTopicIds(),
            input.previousOfferedTopicIds(),
            question,
            selection
        );
        return PracticePlanningResponseMapper.toResponse(
            PracticePaperDtoMapper.toDto(paper, List.of(question))
        );
    }

    /**
     * Lối vào thật của phiên. Client chỉ biết "học sinh bấm một thẻ" nên gửi SELECTED, nhưng
     * thẻ đó có thể là slot ε-greedy do HỆ THỐNG tráo vào lô chào -- ghi SELECTED cho nó là
     * dương giả (0.95 như tự chọn, xem InterestVectorService.recordSessionOutcome), và làm
     * van thăm dò §2.7 không bao giờ đóng vì không phiếu nào mang origin EPSILON.
     *
     * Suy ra bằng cách xếp hạng lại thay vì nhớ slot nào đã chào cho ai: xem
     * ViewPracticeTopicOffersUseCase.isOutsideTopRanked để biết vì sao không dùng cache.
     */
    private String resolveOrigin(UUID studentId, BuildPracticePaperCommand input) {
        var origin = input.origin() == null ? "SELECTED" : input.origin();
        var cameFromOfferBatch = input.offeredTopicIds() != null
            && input.offeredTopicIds().contains(input.topicId());
        if (!"SELECTED".equals(origin) || !cameFromOfferBatch) {
            return origin;
        }
        return viewPracticeTopicOffersUseCase.isOutsideTopRanked(studentId, input.topicId())
            ? "EPSILON"
            : origin;
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

    private PracticeTopic requireTopic(UUID topicId) {
        return topicRepository.findTopicById(topicId)
            .filter(PracticeTopic::isActive)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề luyện tập."));
    }
}
