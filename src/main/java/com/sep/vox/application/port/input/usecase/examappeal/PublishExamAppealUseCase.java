package com.sep.vox.application.port.input.usecase.examappeal;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.ExamAppealPublishedEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.PublishExamAppealCommand;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.examevaluation.UpsertExamCandidateResultUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamEvaluationEngineType;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemEvaluationStatus;
import com.sep.vox.domain.model.exam.ExamResultAppealItem;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamResultAppealItemRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

/**
 * Công bố kết quả phúc khảo.
 *
 * <p>Admin quyết <em>điểm cho từng part</em> chứ không quyết điểm tổng: mỗi part đang
 * phúc khảo nhận một bản HUMAN/FINALIZED, mọi bản cũ của các part đó chuyển SUPERSEDED,
 * rồi {@link UpsertExamCandidateResultUseCase} tính lại tổng và result band từ toàn bộ
 * item. Nhờ vậy tổng luôn bằng hàm của các item, thay vì phụ thuộc admin nhập đúng.
 */
@Service
public class PublishExamAppealUseCase implements IUseCase<PublishExamAppealCommand, UUID> {

    private static final String HUMAN_GRADER = "HUMAN";

    private final ExamResultAppealRepository examResultAppealRepository;
    private final ExamResultAppealItemRepository examResultAppealItemRepository;
    private final ExamItemEvaluationRepository examItemEvaluationRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase;
    private final ExamAppealAccessService examAppealAccessService;
    private final EventPublisherPort eventPublisherPort;

    public PublishExamAppealUseCase(
            ExamResultAppealRepository examResultAppealRepository,
            ExamResultAppealItemRepository examResultAppealItemRepository,
            ExamItemEvaluationRepository examItemEvaluationRepository,
            RubricVersionRepository rubricVersionRepository,
            UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase,
            ExamAppealAccessService examAppealAccessService,
            EventPublisherPort eventPublisherPort) {
        this.examResultAppealRepository = examResultAppealRepository;
        this.examResultAppealItemRepository = examResultAppealItemRepository;
        this.examItemEvaluationRepository = examItemEvaluationRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.upsertExamCandidateResultUseCase = upsertExamCandidateResultUseCase;
        this.examAppealAccessService = examAppealAccessService;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    @Transactional
    public UUID execute(PublishExamAppealCommand command) {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        var context = examAppealAccessService.load(command.appealId());
        examAppealAccessService.authorizeSchoolAdmin(context, currentUserId);

        var appeal = context.appeal();
        if (appeal.getStatus() != ExamAppealStatus.COMPARING) {
            throw new IllegalStateException(
                "Chỉ có thể công bố khi tất cả giám khảo đã nộp báo cáo chấm lại.");
        }

        var appealItems = examResultAppealItemRepository.findByAppealId(command.appealId()).stream()
            .collect(Collectors.toMap(ExamResultAppealItem::getId, Function.identity(),
                (left, right) -> left, LinkedHashMap::new));
        var itemScores = command.itemScores() == null
            ? new ArrayList<PublishExamAppealCommand.ItemScore>() : command.itemScores();
        validateItemCoverage(itemScores, appealItems.keySet());

        var rubricVersion = rubricVersionRepository.findById(context.candidateResult().getRubricVersionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản rubric của bài thi."));
        for (var itemScore : itemScores) {
            if (itemScore.partScore() == null) {
                throw new IllegalArgumentException("Phải nhập điểm cho phần thi được phúc khảo.");
            }
            if (itemScore.partScore().compareTo(rubricVersion.getScoringScaleMin()) < 0
                    || itemScore.partScore().compareTo(rubricVersion.getScoringScaleMax()) > 0) {
                throw new IllegalArgumentException("Điểm công bố phải nằm trong khoảng "
                    + rubricVersion.getScoringScaleMin() + " - " + rubricVersion.getScoringScaleMax() + ".");
            }
        }

        var now = OffsetDateTime.now();
        var scoreBefore = context.candidateResult().getTotalScore();

        // Bản AI và toàn bộ báo cáo giám khảo của MỌI part đang phúc khảo đều lùi về
        // SUPERSEDED, để mỗi part chỉ còn đúng một bản FINALIZED là nguồn điểm.
        var responseIds = appealItems.values().stream().map(ExamResultAppealItem::getResponseId).toList();
        var existing = examItemEvaluationRepository.findByResponseIdIn(responseIds);
        for (var evaluation : existing) {
            if (evaluation.getStatus() != ExamItemEvaluationStatus.SUPERSEDED) {
                evaluation.setStatus(ExamItemEvaluationStatus.SUPERSEDED);
                examItemEvaluationRepository.save(evaluation);
            }
        }

        for (var itemScore : itemScores) {
            var appealItem = appealItems.get(itemScore.appealItemId());
            examItemEvaluationRepository.save(new ExamItemEvaluation(
                appealItem.getResponseId(),
                appealItem.getPaperItemId(),
                ExamEvaluationEngineType.HUMAN,
                HUMAN_GRADER,
                null,
                currentUserId,
                itemScore.partScore(),
                itemScore.partScore(),
                null,
                false,
                null,
                false,
                false,
                null,
                null,
                command.decisionNote(),
                null,
                null,
                ExamItemEvaluationStatus.FINALIZED,
                now
            ));
            appealItem.setFinalScore(itemScore.partScore());
        }
        examResultAppealItemRepository.saveAll(List.copyOf(appealItems.values()));

        // Tổng + result band được dẫn xuất lại từ items, không do admin nhập. Một lần
        // gọi là đủ: calculator quét toàn bộ item nên thấy hết các bản FINALIZED mới.
        var recalculated = upsertExamCandidateResultUseCase.execute(
            context.candidateResult().getSessionId(), ExamCandidateResultStatus.FINAL);

        appeal.setStatus(ExamAppealStatus.PUBLISHED);
        appeal.setScoreAfter(recalculated.getTotalScore());
        appeal.setDecisionNote(command.decisionNote());
        appeal.setResolvedBy(currentUserId);
        appeal.setResolvedAt(now);
        examResultAppealRepository.save(appeal);

        eventPublisherPort.publish(new ExamAppealPublishedEvent(
            appeal.getId(),
            context.studentId(),
            context.examName(),
            scoreBefore,
            recalculated.getTotalScore()
        ));

        return appeal.getId();
    }

    private void validateItemCoverage(
            List<PublishExamAppealCommand.ItemScore> itemScores, Set<UUID> appealItemIds) {
        if (itemScores.isEmpty()) {
            throw new IllegalArgumentException("Phải nhập điểm cho tất cả phần thi được phúc khảo.");
        }
        var submittedIds = itemScores.stream()
            .map(PublishExamAppealCommand.ItemScore::appealItemId).toList();
        if (new HashSet<>(submittedIds).size() != submittedIds.size()) {
            throw new IllegalArgumentException("Không được nhập trùng phần thi.");
        }
        if (!appealItemIds.containsAll(submittedIds)) {
            throw new IllegalArgumentException("Phần thi không thuộc đơn phúc khảo này.");
        }
        if (submittedIds.size() != appealItemIds.size()) {
            throw new IllegalArgumentException(
                "Phải nhập điểm cho đủ " + appealItemIds.size() + " phần thi của đơn phúc khảo.");
        }
    }
}
