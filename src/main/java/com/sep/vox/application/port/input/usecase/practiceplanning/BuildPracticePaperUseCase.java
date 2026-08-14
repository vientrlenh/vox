package com.sep.vox.application.port.input.usecase.practiceplanning;

import java.math.BigDecimal;
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
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaper;
import com.sep.vox.domain.mapper.PracticePaperDtoMapper;
import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.domain.repository.PracticePaperRepository;
import com.sep.vox.domain.repository.PracticeTopicRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

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
    private final QuotaPricingPort quotaPricingPort;

    public BuildPracticePaperUseCase(
            PracticeTopicRepository topicRepository,
            PracticePaperRepository paperRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            PracticeTopicOfferEnrichmentService enrichmentService,
            PracticeQuestionSelectionService selectionService,
            PracticePaperPersistenceService persistenceService,
            UserContextPort userContextPort,
            ViewPracticeTopicOffersUseCase viewPracticeTopicOffersUseCase,
            QuotaPricingPort quotaPricingPort) {
        this.viewPracticeTopicOffersUseCase = viewPracticeTopicOffersUseCase;
        this.topicRepository = topicRepository;
        this.paperRepository = paperRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.enrichmentService = enrichmentService;
        this.selectionService = selectionService;
        this.persistenceService = persistenceService;
        this.userContextPort = userContextPort;
        this.quotaPricingPort = quotaPricingPort;
    }

    @Override
    public PracticePaper execute(BuildPracticePaperCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var topic = requireTopic(input.topicId());
        var quotaRemainingUsd = remainingPracticeQuotaUsd(studentId);
        var focus = selectionService.resolveFocus(studentId, input.fromSubAttribute());
        var chosenBandOrder = chosenBandOrder(input.targetFrameworkBandId());
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
            .resolveNextQuestion(topic, studentId, focus, chosenBandOrder, List.of())
            .orElseThrow(() -> new NotFoundException("Chủ đề chưa có câu luyện phù hợp."));
        var question = selection.question();
        // Ước lượng worst-case (giây x giá/giây calibrate) -- không phải chi phí thật, cùng tinh
        // thần Công thức 1 của ClassTestTokenQuotaGuardService (xem AI_USAGE_QUOTA_USD_MIGRATION.md
        // mục 5/6), nhưng dùng rate calibrate RIÊNG cho PRACTICE (currentEstimatedCostPerPracticeSecondUsd,
        // xem QuotaPricingCalibrationService/QuotaPricingSource) vì pipeline AI khác hẳn exam. Cộng
        // cả reservedSeconds (câu đang chờ nộp của các phiên khác) để không cho 2 phiên cùng lúc
        // cùng vượt hạn mức trong lúc chưa phiên nào submit turn thật.
        var reservedSeconds = paperRepository.sumReservedQuotaSeconds(studentId);
        var estimatedCostUsd = BigDecimal.valueOf((long) reservedSeconds + question.spokenSeconds())
            .multiply(quotaPricingPort.currentEstimatedCostPerPracticeSecondUsd());
        if (quotaRemainingUsd.compareTo(estimatedCostUsd) < 0) {
            throw new QuotaExceededException("Hạn mức PRACTICE không đủ cho một câu trọn vẹn.");
        }
        var paper = persistenceService.persist(
            studentId,
            input.topicId(),
            input.targetFrameworkBandId(),
            resolveOrigin(studentId, input),
            input.offeredTopicIds(),
            input.previousOfferedTopicIds(),
            question,
            selection
        );
        return PracticePlanningResponseMapper.toResponse(
            PracticePaperDtoMapper.toDto(
                paper,
                List.of(question),
                enrichmentService.sessionBudgetSeconds(studentId, chosenBandOrder)
            )
        );
    }

    /**
     * Lối vào thật của phiên. Client chỉ biết "học sinh bấm một thẻ" nên gửi SELECTED, nhưng
     * thẻ đó có thể là slot ε-greedy do HỆ THỐNG tráo vào lô chào. Ghi SELECTED cho nó hỏng
     * hai việc:
     *
     * <ul>
     *   <li>tín hiệu sở thích thành dương giả -- 0.95 như học sinh tự chọn, trong khi chủ đề
     *       là do hệ thống đưa tới. Từ 2026-08-06 EPSILON có case riêng trong
     *       {@code InterestVectorService.recordSessionOutcome} và nhận 0.60 cùng nhóm với
     *       EXPLORATION; trước đó nó rơi vào {@code default} nên nhận diện xong vẫn tính 0.95,
     *       tức vế này của chú thích cũ mô tả một điều chưa thành sự thật.</li>
     *   <li>van thăm dò §2.7 không bao giờ đóng, vì không phiếu nào mang origin EPSILON để
     *       mà đếm.</li>
     * </ul>
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

    private BigDecimal remainingPracticeQuotaUsd(UUID studentId) {
        var quota = schoolSubscriptionRepository.findPracticeQuotaRemaining(studentId);
        return quota.max(BigDecimal.ZERO);
    }

    /**
     * Thứ tự của bậc học sinh chọn, sau khi đã kiểm bậc đó có thật trong khung đang áp cho em.
     *
     * <p>Phải kiểm, không được tin thẳng id client gửi: nhận bừa một bậc thuộc khung khác thì
     * {@code result_band_order} của nó vô nghĩa với thang này, và mọi phép so độ khó phía sau
     * lệch âm thầm. Dùng luôn thang bậc đã nạp cho màn hình chọn nên không tốn query mới.
     */
    private int chosenBandOrder(UUID bandId) {
        if (bandId == null) {
            throw new NotFoundException("Chưa chọn bậc muốn luyện.");
        }
        return enrichmentService.frameworkBandLadder().stream()
            .filter(band -> bandId.equals(band.getId()))
            .findFirst()
            .map(band -> band.getOrder())
            .orElseThrow(() -> new NotFoundException(
                "Bậc luyện tập không thuộc khung đánh giá đang áp dụng."
            ));
    }

    private PracticeTopic requireTopic(UUID topicId) {
        return topicRepository.findTopicById(topicId)
            .filter(candidate -> candidate.isActive())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề luyện tập."));
    }
}
